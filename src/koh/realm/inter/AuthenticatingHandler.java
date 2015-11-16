package koh.realm.inter;

import com.google.inject.Inject;
import koh.inter.messages.HelloMessage;
import koh.patterns.BreakPropagation;
import koh.patterns.event.EventExecutor;
import koh.patterns.event.api.EventListener;
import koh.patterns.event.api.EventTreatmentPriority;
import koh.patterns.event.api.Listen;
import koh.patterns.handler.api.Handler;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.realm.app.Logs;
import koh.realm.entities.GameServer;
import koh.realm.inter.annotations.Connect;
import koh.realm.inter.annotations.Disconnect;
import koh.realm.inter.annotations.InterPackage;
import koh.realm.inter.annotations.ReceiveInterMessage;
import koh.realm.inter.contexts.Authenticating;
import koh.realm.inter.events.GameServerAuthenticatedEvent;

@RequireContexts(@Ctx(value = Authenticating.class))
public class AuthenticatingHandler implements Handler, EventListener {

    @Inject private Logs logs;

    @Inject @InterPackage private EventExecutor eventListening;

    @Connect
    public void onConnect(GameServer server) throws Exception {
    }

    @ReceiveInterMessage
    public void onHello(GameServer server, HelloMessage message) throws Exception, BreakPropagation {
    }

    @Listen
    public void onReceiveEvent(GameServerAuthenticatedEvent event) {
    }

    @Listen(priority = EventTreatmentPriority.LOWEST)
    public void onReceiveEvent2(GameServerAuthenticatedEvent event) {
    }

    @Disconnect
    public void onDisconnect(GameServer server) throws Exception {

    }
}
