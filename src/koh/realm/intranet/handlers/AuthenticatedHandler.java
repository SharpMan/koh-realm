package koh.realm.intranet.handlers;

import com.google.inject.Inject;
import koh.inter.messages.ExpulseAccountMessage;
import koh.inter.messages.PlayerComingMessage;
import koh.inter.messages.PlayerCreatedMessage;
import koh.mina.api.annotations.Disconnect;
import koh.mina.api.annotations.Receive;
import koh.patterns.Controller;
import koh.patterns.event.api.Listen;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.patterns.services.api.ServiceDependency;
import koh.protocol.messages.connection.ServerStatusUpdateMessage;
import koh.realm.dao.api.AccountDAO;
import koh.realm.dao.api.CharacterDAO;
import koh.realm.entities.Account;
import koh.realm.intranet.GameServerClient;
import koh.realm.intranet.InterServerContexts;
import koh.realm.intranet.events.ServerStatusChangedEvent;
import koh.realm.internet.RealmContexts;
import koh.realm.internet.RealmServer;
import koh.repositories.RepositoryReference;

@RequireContexts(@Ctx(value = InterServerContexts.Authenticated.class))
public class AuthenticatedHandler implements Controller {

    @Inject
    private RealmServer realmServer;

    @Inject @ServiceDependency("RealmServices")
    private CharacterDAO characterDAO;

    @Receive
    public void onPlayerCreated(GameServerClient server, PlayerCreatedMessage message) throws Exception {
        characterDAO.insertOrUpdate(message.accountId, server.getEntity().ID, (short)message.currentCount);
    }

    @Listen
    public void onStatusChanged(ServerStatusChangedEvent event) {
        realmServer.getMina().broadcast(new ServerStatusUpdateMessage(event.entity.toInformations()),
                (client) -> client.getHandlerContext() == RealmContexts.AUTHENTICATED);
    }

    @Disconnect
    public void onDisconnect(GameServerClient server) throws Exception {
        server.disconnect(false);
    }

}