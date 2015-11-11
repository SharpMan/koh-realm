package koh.realm.network;

import koh.patterns.event.api.EventListener;
import koh.patterns.event.api.Listen;
import koh.patterns.handler.api.Handler;
import koh.realm.inter.events.GameServerAuthenticatedEvent;

public class TestController implements Handler, EventListener {

    @Listen
    public void onGameServerConnected(GameServerAuthenticatedEvent event) {
        System.out.println(event.numSeq);
    }
}
