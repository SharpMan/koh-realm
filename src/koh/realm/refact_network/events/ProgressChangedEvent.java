package koh.realm.refact_network.events;

import koh.patterns.event.Event;
import koh.realm.refact_network.RealmClient;

public class ProgressChangedEvent extends Event<RealmClient> {

    public final int position;

    public ProgressChangedEvent(RealmClient target, int position) {
        super(target);

        this.position = position;
    }

}
