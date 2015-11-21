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
import koh.realm.intranet.GameServerClient;
import koh.realm.intranet.InterServerContexts;
import koh.realm.intranet.events.ServerStatusChangedEvent;
import koh.realm.internet.RealmContexts;
import koh.realm.internet.RealmServer;

@RequireContexts(@Ctx(value = InterServerContexts.Authenticated.class))
public class AuthenticatedHandler implements Controller {

    @Inject @ServiceDependency("RealmServices")
    private RealmServer realmServer;

    @Receive
    public void onExpulse(GameServerClient server, ExpulseAccountMessage message) throws Exception {

    }

    @Receive
    public void onPlayerComing(GameServerClient server, PlayerComingMessage message) throws Exception {

    }

    @Receive
    public void onPlayerCreated(GameServerClient server, PlayerCreatedMessage message) throws Exception {

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