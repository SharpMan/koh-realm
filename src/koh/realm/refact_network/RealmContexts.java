package koh.realm.refact_network;

import koh.patterns.handler.context.Context;

public class RealmContexts {

    public static class Authenticating implements Context { private Authenticating(){} }
    public static class InWaitingQueue implements Context { private InWaitingQueue(){} }
    public static class Authenticated implements Context { private Authenticated(){} }
    public static class Disconnected implements Context { private Disconnected(){} }
    public static class Switching implements Context { private Switching(){} }

    public static final Authenticating AUTHENTICATING = new Authenticating();
    public static final InWaitingQueue IN_WAITING_QUEUE = new InWaitingQueue();
    public static final Authenticated AUTHENTICATED = new Authenticated();
    public static final Switching SWITCHING = new Switching();
    public static final Disconnected DISCONNECTED = new Disconnected();

}
