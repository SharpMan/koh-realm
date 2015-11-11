package koh.realm.inter.events;

import koh.patterns.event.Event;
import koh.realm.entities.GameServer;

public class GameServerAuthenticatedEvent extends Event<GameServer> {

    public final long numSeq;

    public GameServerAuthenticatedEvent(GameServer emitter, long numSeq) {
        super(emitter);
        this.numSeq = numSeq;
    }
}
