package koh.realm.intranet;

import koh.mina.api.MinaClient;
import koh.patterns.event.Event;
import koh.patterns.event.EventExecutor;
import koh.realm.entities.GameServer;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.session.IoSession;

public class GameServerClient extends MinaClient {

    private final EventExecutor eventEmitter;

    private GameServer entity;

    public GameServerClient(IoSession session, EventExecutor eventEmitter) {
        super(session, InterServerContexts.AUTHENTICATING);

        this.eventEmitter = eventEmitter;
    }

    public void emit(Event<GameServerClient> event) {
        eventEmitter.fire(event);
    }

    public GameServer getEntity() {
        return entity;
    }

    public void setEntity(GameServer entity) {
        this.entity = entity;
    }

    @Override
    public CloseFuture disconnect(boolean waitPendingMessages) {
        CloseFuture closing = super.disconnect(waitPendingMessages);

        if (entity != null)
            entity.setOffline();
        this.entity = null;

        return closing;
    }

}
