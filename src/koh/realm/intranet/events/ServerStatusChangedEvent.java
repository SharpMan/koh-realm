package koh.realm.intranet.events;

import koh.patterns.event.Event;
import koh.realm.entities.GameServer;
import koh.realm.intranet.GameServerClient;

public class ServerStatusChangedEvent extends Event<GameServerClient> {

    public final GameServer entity;

    public ServerStatusChangedEvent(GameServerClient emitter, GameServer entity) {
        super(emitter);

        this.entity = entity;
    }
}
