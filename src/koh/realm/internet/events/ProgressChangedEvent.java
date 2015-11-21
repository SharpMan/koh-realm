package koh.realm.internet.events;

import koh.patterns.event.Event;
import koh.realm.internet.RealmClient;

public class ProgressChangedEvent extends Event<RealmClient> {

    public final int position;
    public final int total;

    public ProgressChangedEvent(RealmClient target, int position, int total) {
        super(target);

        this.total = total;
        this.position = position;
    }

}
