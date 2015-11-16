package koh.realm.refact_network.handlers;

import com.google.inject.Inject;
import koh.concurrency.LambdaException;
import koh.concurrency.WaitingQueue;
import koh.mina.api.annotations.Disconnect;
import koh.patterns.event.EventExecutor;
import koh.patterns.event.api.EventListener;
import koh.patterns.event.api.Listen;
import koh.patterns.handler.api.Handler;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.protocol.client.BufUtils;
import koh.protocol.client.Message;
import koh.protocol.client.MessageTransaction;
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
import koh.realm.refact_network.AuthenticationToken;
import koh.realm.refact_network.RealmClient;
import koh.realm.refact_network.RealmContexts;
import koh.realm.refact_network.RealmPackage;
import koh.realm.refact_network.events.ClientContextChangedEvent;
import koh.realm.refact_network.events.ProgressChangedEvent;
import koh.repositories.RepositoryReference;
import org.apache.mina.core.buffer.IoBuffer;

import java.time.Instant;
import java.util.ArrayList;

@RequireContexts(@Ctx(RealmContexts.InWaitingQueue.class))
public class WaitingHandler implements Handler, EventListener {

    private @Inject @RealmPackage EventExecutor eventsEmitter;
    @Inject AccountDAO accountDAO;
    @Inject GameServerDAO serverDAO;

    @Listen
    public void onContextChanged(ClientContextChangedEvent event) {
        if(event.getNewContext() != RealmContexts.IN_WAITING_QUEUE)
            return;

        int startPos = queue.push(event.getTarget());
        event.getTarget().write(new LoginQueueStatusMessage((short)startPos, (short)queue.size()));
    }

    private final WaitingQueue<RealmClient> queue = new WaitingQueue<>(100, 5000, this::treatWaiting, this::signalProgress);

    private static final Message wrongCredentialsMessage = new IdentificationFailedMessage(IdentificationFailureReason.WRONG_CREDENTIALS);
    private static final Message bannedMessage = new IdentificationFailedMessage(IdentificationFailureReason.BANNED);
    private static final Message alreadyConnectedMessage = new IdentificationFailedMessage(IdentificationFailureReason.TOO_MANY_ON_IP);

    private static final Message endQueueMessage = new LoginQueueStatusMessage((short)0, (short)0);

    private void signalProgress(int position, RealmClient client) {
        eventsEmitter.fire(new ProgressChangedEvent(client, position));
    }

    @Listen
    public void onProgressChanged(ProgressChangedEvent event) {
        if(event.getTarget().getAuthenticationToken() == null)
            return;

        event.getTarget().write(new LoginQueueStatusMessage((short)event.position, (short)queue.size()));
    }

    private void treatWaiting(RealmClient client) {
        if(client.getAuthenticationToken() == null)
            return;

        AuthenticationToken token = client.getAuthenticationToken();
        client.setAuthenticationToken(null);
        client.write(endQueueMessage);

        String login;
        String password;
        {
            IoBuffer credentialsBuffer = IoBuffer.wrap(token.identificationMessage.getCredentials());
            login = BufUtils.readUTF(credentialsBuffer);
            password = BufUtils.readUTF(credentialsBuffer);
        }

        RepositoryReference<Account> loadedAccount = accountDAO.getCompteByName(login);

        try {
            loadedAccount.sync(() -> {

                if(!loadedAccount.get().isValidPass(password)) {
                    throw new LambdaException(() -> client.disconnect(wrongCredentialsMessage));
                }

                if(loadedAccount.get().client != null) {
                    if(loadedAccount.get().isBanned()) {
                        throw new LambdaException(() -> {
                            client.disconnect(bannedMessage);
                            loadedAccount.get().getClient().disconnect(bannedMessage);
                        });
                    }
                    throw new LambdaException(() -> {
                        client.disconnect(alreadyConnectedMessage);
                        loadedAccount.get().getClient().disconnect();
                    });
                }

                if(loadedAccount.get().isBanned()) {
                    throw new LambdaException(() -> client.disconnect(bannedMessage));
                }

                loadedAccount.get().setClient(client);
                client.setAccount(loadedAccount);

            });
        } catch(LambdaException exception) {
            exception.treat();
            return;
        }

        Account acc = loadedAccount.get();

        client.setHandlerContext(RealmContexts.AUTHENTICATED);
        try(MessageTransaction trans = client.startTransaction()) {
            trans.write(new IdentificationSuccessMessage(acc.Username, acc.NickName, acc.ID,
                    /*Community*/ 0, acc.Right > 0, acc.SecretQuestion,
                    Instant.now().getEpochSecond() * 1000, false));

            trans.write(new ServersListMessage(new ArrayList<GameServerInformations>() {{
                serverDAO.getGameServers().stream().forEach((server) -> add(new GameServerInformations(
                        server.ID, server.State,
                        (byte) (server.State == ServerStatusEnum.FULL ? 1 : 0),
                        true, acc.getPlayers(server.ID), 0))
                );
            }}));
        }
    }

    @Disconnect
    public void onDisconnect(RealmClient client) {
        queue.remove(client);
        client.disconnect(false);
    }
}
