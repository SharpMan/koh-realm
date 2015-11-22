package koh.realm.internet.handlers;

import com.google.inject.Inject;
import koh.concurrency.LambdaException;
import koh.concurrency.WaitingQueue;
import koh.mina.api.annotations.Disconnect;
import koh.patterns.Controller;
import koh.patterns.event.EventExecutor;
import koh.patterns.event.api.Listen;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.protocol.client.*;
import koh.protocol.client.codec.Dofus2ProtocolEncoder;
import koh.protocol.client.enums.IdentificationFailureReason;
import koh.protocol.client.enums.ServerStatusEnum;
import koh.protocol.client.types.GameServerInformations;
import koh.protocol.messages.connection.IdentificationFailedMessage;
import koh.protocol.messages.connection.IdentificationSuccessMessage;
import koh.protocol.messages.connection.LoginQueueStatusMessage;
import koh.protocol.messages.connection.ServersListMessage;
import koh.realm.dao.api.AccountDAO;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.entities.Account;
import koh.realm.entities.GameServer;
import koh.realm.internet.AuthenticationToken;
import koh.realm.internet.RealmClient;
import koh.realm.internet.RealmContexts;
import koh.realm.internet.RealmPackage;
import koh.realm.internet.events.ClientContextChangedEvent;
import koh.realm.internet.events.ProgressChangedEvent;
import koh.realm.intranet.events.ServerStatusChangedEvent;
import koh.repositories.RepositoryReference;
import org.apache.mina.core.buffer.IoBuffer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

@RequireContexts(@Ctx(RealmContexts.InWaitingQueue.class))
public class WaitingHandler implements Controller {

    private final Dofus2ProtocolEncoder encoder;
    private final GameServerDAO serverDAO;

    private final PregenMessage wrongCredentialsMessage;
    private final PregenMessage bannedMessage;
    private final PregenMessage alreadyConnectedMessage;
    private final PregenMessage maintenanceMessage;
    private final PregenMessage endQueueMessage;

    @Inject
    public WaitingHandler(GameServerDAO serverDAO, Dofus2ProtocolEncoder encoder) {
        this.encoder = encoder;
        this.serverDAO = serverDAO;

        this.wrongCredentialsMessage = new PregenMessage(
                encoder.encodeMessage(new IdentificationFailedMessage(IdentificationFailureReason.WRONG_CREDENTIALS), IoBuffer.allocate(16))
        );
        this.bannedMessage = new PregenMessage(
                encoder.encodeMessage(new IdentificationFailedMessage(IdentificationFailureReason.BANNED), IoBuffer.allocate(16))
        );
        this.alreadyConnectedMessage = new PregenMessage(
                encoder.encodeMessage(new IdentificationFailedMessage(IdentificationFailureReason.TOO_MANY_ON_IP), IoBuffer.allocate(16))
        );
        this.maintenanceMessage = new PregenMessage(
                encoder.encodeMessage(new IdentificationFailedMessage(IdentificationFailureReason.IN_MAINTENANCE), IoBuffer.allocate(16))
        );
        this.endQueueMessage = new PregenMessage(
                encoder.encodeMessage(new LoginQueueStatusMessage((short)0, (short)0), IoBuffer.allocate(16))
        );
        this.serversListMessage = new PregenMessage(
                encoder.encodeMessage( new ServersListMessage(new ArrayList<GameServerInformations>() {{
                            serverDAO.getGameServers().stream().forEach((server) -> add(server.toInformations()));
                        }}),  IoBuffer.allocate(128)
                )
        );
        System.out.println(serversListMessage.get());
    }

    private @Inject EventExecutor eventsEmitter;
    private @Inject AccountDAO accountDAO;

    @Listen
    public void onContextChanged(ClientContextChangedEvent event) {
        if(event.getNewContext() != RealmContexts.IN_WAITING_QUEUE)
            return;

        int startPos = queue.push(event.getTarget());
        if(startPos > 1)
            event.getTarget().write(new LoginQueueStatusMessage((short)startPos, (short)queue.size()));
    }

    private final WaitingQueue<RealmClient> queue = new WaitingQueue<>(100, 3000, this::treatWaiting, this::signalProgress);

    private void signalProgress(RealmClient client, int position, int total) {
        eventsEmitter.fire(new ProgressChangedEvent(client, position, total));
    }

    @Listen
    public void onProgressChanged(ProgressChangedEvent event) {
        if(event.getTarget().getAuthenticationToken() == null)
            return;

        event.getTarget().write(new LoginQueueStatusMessage((short)event.position, (short)event.total));
    }

    private int countOnlineServers() {
        int count = 0;
        for(GameServer server : serverDAO.getGameServers())
            if(server.getStatus() == ServerStatusEnum.ONLINE)
                ++count;
        //return count;

        return 1;
    }

    private void treatWaiting(RealmClient client) {
        if(client.disconnecting() || client.getAuthenticationToken() == null)
            return;

        AuthenticationToken token = client.getAuthenticationToken();
        client.setAuthenticationToken(null);

        if(serverDAO.getGameServers().size() == 0 || countOnlineServers() == 0) {
            client.disconnect(maintenanceMessage);
            return;
        }

        String login;
        String password;
        {
            IoBuffer credentialsBuffer = IoBuffer.wrap(token.identificationMessage.getCredentials());
            login = BufUtils.readUTF(credentialsBuffer);
            password = BufUtils.readUTF(credentialsBuffer);
        }

        RepositoryReference<Account> loadedAccount = accountDAO.getCompteByName(login);

        if(loadedAccount == null) {
            client.disconnect(new MessageQueue(endQueueMessage, wrongCredentialsMessage));
            return;
        }

        try {
            loadedAccount.sync(() -> {

                if(!loadedAccount.get().isValidPass(password)) {
                    throw new LambdaException(() -> client.disconnect(
                            new MessageQueue(endQueueMessage, wrongCredentialsMessage)
                    ));
                }

                if(loadedAccount.get().client != null) {
                    if(loadedAccount.get().isBanned()) {
                        throw new LambdaException(() -> {
                            client.disconnect(bannedMessage);
                            loadedAccount.get().getClient().disconnect(
                                    new MessageQueue(endQueueMessage, bannedMessage)
                            );
                        });
                    }
                    throw new LambdaException(() -> {
                        client.disconnect(new MessageQueue(endQueueMessage, alreadyConnectedMessage));
                        loadedAccount.get().getClient().disconnect();
                    });
                }

                if(loadedAccount.get().isBanned()) {
                    throw new LambdaException(() -> client.disconnect(
                            new MessageQueue(endQueueMessage, bannedMessage)
                    ));
                }

                loadedAccount.get().setClient(client);
                client.setAccount(loadedAccount);
                accountDAO.save(loadedAccount.get());

            });
        } catch(LambdaException exception) {
            exception.treat();
            return;
        }

        Account acc = loadedAccount.get();

        client.setHandlerContext(RealmContexts.AUTHENTICATED);

        client.transact((trans) -> {
            trans.write(endQueueMessage);

            trans.write(new IdentificationSuccessMessage(acc.Username, acc.NickName, acc.ID,
                    /*Community*/ 0, acc.Right > 0, acc.SecretQuestion,
                    Instant.now().plus(365, ChronoUnit.DAYS).toEpochMilli() , false));

            trans.write(serversListMessage);
        });
    }

    private volatile PregenMessage serversListMessage;
    @Listen public void onStatusChanged(ServerStatusChangedEvent event) {
        this.serversListMessage = new PregenMessage(
                encoder.encodeMessage( new ServersListMessage(new ArrayList<GameServerInformations>() {{
                      serverDAO.getGameServers().stream().forEach((server) -> add(server.toInformations()));
                }}),
                IoBuffer.allocate(128))
        );
    }

    @Disconnect
    public void onDisconnect(RealmClient client) {
        System.out.println("Client disconnected from WaitingHandler : " + client.getRemoteAddress());
        queue.remove(client);
        client.disconnect(false);
    }
}
