package koh.realm.internet.handlers;

import com.google.inject.Inject;
import koh.concurrency.LambdaException;
import koh.concurrency.WaitingQueue;
import koh.inter.InterMessage;
import koh.inter.messages.ExpulseAccountMessage;
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
import koh.protocol.messages.connection.*;
import koh.realm.Main;
import koh.realm.dao.api.AccountDAO;
import koh.realm.dao.api.BannedAddressDAO;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.entities.Account;
import koh.realm.entities.GameServer;
import koh.realm.internet.AuthenticationToken;
import koh.realm.internet.RealmClient;
import koh.realm.internet.RealmContexts;
import koh.realm.internet.RealmServer;
import koh.realm.internet.events.ClientContextChangedEvent;
import koh.realm.internet.events.ProgressChangedEvent;
import koh.realm.intranet.InterServer;
import koh.realm.intranet.events.ServerStatusChangedEvent;
import koh.repositories.RepositoryReference;
import org.apache.mina.core.buffer.IoBuffer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RequireContexts(@Ctx(RealmContexts.InWaitingQueue.class))
public class WaitingHandler implements Controller {

    private final Dofus2ProtocolEncoder encoder;
    private final GameServerDAO serverDAO;

    private final PregenMessage wrongCredentialsMessage;
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
                encoder.encodeMessage(new ServersListMessage(serverDAO.getGameServers()
                        .map(GameServer::toInformations).collect(Collectors.toList())),  IoBuffer.allocate(128))
        );
        this.adminServersListMessage = new PregenMessage(
                encoder.encodeMessage(new ServersListMessage(serverDAO.getGameServers()
                        .map(GameServer::toInformations).collect(Collectors.toList())),  IoBuffer.allocate(128))
        );
        this.pvpListMessage = new PregenMessage(
                encoder.encodeMessage(new ServersListMessage(serverDAO.getGameServers()
                        .filter(g -> g.ID == 1)
                        .map(GameServer::toInformations).collect(Collectors.toList())),  IoBuffer.allocate(128))
        );
        this.pvmListMessage = new PregenMessage(
                encoder.encodeMessage(new ServersListMessage(serverDAO.getGameServers()
                        .filter(g -> g.ID == 3)
                        .map(GameServer::toInformations).collect(Collectors.toList())),  IoBuffer.allocate(128))
        );
    }

    private @Inject EventExecutor eventsEmitter;
    private @Inject AccountDAO accountDAO;
    private @Inject BannedAddressDAO addressDAO;
    //private @Inject InterServer interServer;

    @Listen
    public void onContextChanged(ClientContextChangedEvent event) {
        if(event.getNewContext() != RealmContexts.IN_WAITING_QUEUE)
            return;

        int startPos = queue.push(event.getTarget());
        if(startPos > 1)
            event.getTarget().write(new LoginQueueStatusMessage((short)startPos, (short)queue.size()));
    }

    private final WaitingQueue<RealmClient> queue = new WaitingQueue<>(30, 3000, this::treatWaiting, this::signalProgress);

    private void signalProgress(RealmClient client, int position, int total) {
        eventsEmitter.fire(new ProgressChangedEvent(client, position, total));
    }

    @Listen
    public void onProgressChanged(ProgressChangedEvent event) {
        if(event.getTarget().getAuthenticationToken() == null)
            return;

        event.getTarget().write(new LoginQueueStatusMessage((short)event.position, (short)event.total));
    }

    private void treatWaiting(RealmClient client) {
        if(client.disconnecting() || client.getAuthenticationToken() == null){
            return;
        }

        final AuthenticationToken token = client.getAuthenticationToken();
        client.setAuthenticationToken(null);

/*        if(serverDAO.getGameServers().filter(gs -> gs.getStatus() == ServerStatusEnum.ONLINE).count() == 0) {
            client.disconnect(maintenanceMessage);
            return;
        }*/

        final String login;
        final String password;
        {
            IoBuffer credentialsBuffer = IoBuffer.wrap(token.identificationMessage.getCredentials());
            login = BufUtils.readUTF(credentialsBuffer);
            password = BufUtils.readUTF(credentialsBuffer);
        }

        final RepositoryReference<Account> loadedAccount = accountDAO.getAccount(login);

        if(loadedAccount == null || loadedAccount.get() == null || loadedAccount.get().username == null) {
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
                    if(addressDAO.isBanned(client.getRemoteAddress().getAddress().getHostAddress())){
                        throw new LambdaException(() -> {
                            client.disconnect(new IdentificationFailedBannedMessage(IdentificationFailureReason.BANNED,loadedAccount.get().suspendedTime));
                            loadedAccount.get().getClient().disconnect(
                                    new MessageQueue(endQueueMessage, new IdentificationFailedBannedMessage(IdentificationFailureReason.BANNED, addressDAO.get(client.getRemoteAddress().getAddress().toString())))
                            );
                        });
                    }
                    if(loadedAccount.get().isBanned()) {
                        throw new LambdaException(() -> {
                            client.disconnect(new IdentificationFailedBannedMessage(IdentificationFailureReason.BANNED,loadedAccount.get().suspendedTime));
                            loadedAccount.get().getClient().disconnect(
                                    new MessageQueue(endQueueMessage, new IdentificationFailedBannedMessage(IdentificationFailureReason.BANNED, loadedAccount.get().suspendedTime))
                            );
                        });
                    }
                    throw new LambdaException(() -> {
                        client.disconnect(new MessageQueue(endQueueMessage, alreadyConnectedMessage));
                        loadedAccount.get().getClient().disconnect();
                    });
                }

                if(addressDAO.isBanned(client.getRemoteAddress().getAddress().getHostAddress())){
                    throw new LambdaException(() -> client.disconnect(
                            new MessageQueue(endQueueMessage, new IdentificationFailedBannedMessage(IdentificationFailureReason.BANNED, addressDAO.get(client.getRemoteAddress().getAddress().toString())))
                    ));
                }

                if(loadedAccount.get().isBanned()) {
                    throw new LambdaException(() -> client.disconnect(
                            new MessageQueue(endQueueMessage, new IdentificationFailedBannedMessage(IdentificationFailureReason.BANNED, loadedAccount.get().suspendedTime))
                    ));
                }

                loadedAccount.get().setClient(client);
                client.setAccount(loadedAccount);
                accountDAO.save(loadedAccount.get());

            });
        } catch(LambdaException exception) {
            exception.treat();
            client.log((logger) -> logger.info("Server refused connection"));
            return;
        }

        final Account acc = loadedAccount.get();

        client.setHandlerContext(RealmContexts.AUTHENTICATED);

        /*try {
            final InterMessage msg = new ExpulseAccountMessage(loadedAccount.get().id);
            serverDAO.getGameServers()
                    .filter(se -> se.getClient() != null)
                    .forEach(server -> {
                        try {
                            server.getClient().write(msg).await(10, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });


        }
        catch (Exception e){
            e.printStackTrace();
        }*/
       // System.out.println("job done"+(this.interServer == null) + " "+(this.interServer.getMina() == null));


        client.transact((trans) -> {
            trans.write(endQueueMessage);

            trans.write(new IdentificationSuccessMessage(acc.username, acc.nickName, acc.id,
                    /*Community*/ 0, acc.right > 0, acc.secretQuestion,
                    Instant.now().plus(365, ChronoUnit.DAYS).toEpochMilli() , false));

            if(acc.reg_server == 1 && acc.getPlayers(PVP) == 0){
                trans.write(acc.right > 0 ? adminServersListMessage : pvpListMessage);
            }
            else if(acc.reg_server == 3 && acc.getPlayers(PVM) == 0){
                trans.write(acc.right > 0 ? adminServersListMessage : pvmListMessage);
            }
            else
                trans.write(acc.right > 0 ? adminServersListMessage : serversListMessage);
        });
        //Main.INTER_SERVER.getMina().broadcast(new ExpulseAccountMessage(loadedAccount.get().id));

    }
    private static final short PVP = 1,PVM = 3;

    @Inject
    private RealmServer realmServer;

    private volatile PregenMessage serversListMessage, pvpListMessage,pvmListMessage;
    private volatile PregenMessage adminServersListMessage;

    @Listen public void onStatusChanged(ServerStatusChangedEvent event) {
        this.serversListMessage = new PregenMessage(
                encoder.encodeMessage(new ServersListMessage(serverDAO.getGameServers()
                        .filter(g -> g.RequiredRole  == 0)
                        .map(GameServer::toInformations).collect(Collectors.toList())),  IoBuffer.allocate(128))
        );
        this.adminServersListMessage = new PregenMessage(
                encoder.encodeMessage(new ServersListMessage(serverDAO.getGameServers()
                        .map(GameServer::toInformations).collect(Collectors.toList())),  IoBuffer.allocate(128))
        );
        this.pvpListMessage = new PregenMessage(
                encoder.encodeMessage(new ServersListMessage(serverDAO.getGameServers()
                        .filter(g -> g.ID == 1)
                        .map(GameServer::toInformations).collect(Collectors.toList())),  IoBuffer.allocate(128))
        );
        this.pvmListMessage = new PregenMessage(
                encoder.encodeMessage(new ServersListMessage(serverDAO.getGameServers()
                        .filter(g -> g.ID == 3)
                        .map(GameServer::toInformations).collect(Collectors.toList())),  IoBuffer.allocate(128))
        );
    }

    @Disconnect
    public void onDisconnect(RealmClient client) {
        client.log((logger) -> logger.info("Client disconnected"));
        queue.remove(client);
        client.disconnect(false);
    }
}
