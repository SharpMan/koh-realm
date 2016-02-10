package koh.realm.internet;

import koh.patterns.handler.context.Context;

public class RealmContexts {

    public static class Authenticating implements Context { private Authenticating(){} }
    public static class InWaitingQueue implements Context { private InWaitingQueue(){} }
    public static class Authenticated implements Context { private Authenticated(){} }

    public static final Authenticating AUTHENTICATING = new Authenticating();
    public static final InWaitingQueue IN_WAITING_QUEUE = new InWaitingQueue();
    public static final Authenticated AUTHENTICATED = new Authenticated();

}
