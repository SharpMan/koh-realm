package koh.realm.inter;

import com.google.inject.Inject;
import koh.inter.messages.ExpulseAccountMessage;
import koh.inter.messages.PlayerCommingMessage;
import koh.inter.messages.PlayerCreatedMessage;
import koh.patterns.event.api.EventListener;
import koh.patterns.handler.api.Handler;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.realm.app.Logs;
import koh.realm.entities.GameServer;
import koh.realm.inter.annotations.Disconnect;
import koh.realm.inter.annotations.ReceiveInterMessage;
import koh.realm.inter.contexts.Authenticated;

@RequireContexts(@Ctx(value = Authenticated.class))
public class AuthenticatedHandler implements Handler, EventListener {

    @Inject
    private Logs logs;

    @ReceiveInterMessage
    public void onExpulse(GameServer server, ExpulseAccountMessage message) throws Exception {

    }

    @ReceiveInterMessage
    public void onPlayerCome(GameServer server, PlayerCommingMessage message) throws Exception {

    }

    @ReceiveInterMessage
    public void onPlayerCreated(GameServer server, PlayerCreatedMessage message) throws Exception {

    }

    @Disconnect
    public void onDisconnect(GameServer server) throws Exception {

    }

}