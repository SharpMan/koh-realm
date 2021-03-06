package koh.realm.internet.events;

import koh.patterns.event.Event;
import koh.patterns.handler.context.Context;
import koh.realm.internet.RealmClient;

public class ClientContextChangedEvent extends Event<RealmClient> {

    private final Context lastContext;
    private final Context newContext;

    public ClientContextChangedEvent(RealmClient target, Context lastContext, Context newContext) {
        super(target);

        this.lastContext = lastContext;
        this.newContext = newContext;
    }

    public Context getLastContext() {
        return lastContext;
    }

    public Context getNewContext() {
        return newContext;
    }

}
