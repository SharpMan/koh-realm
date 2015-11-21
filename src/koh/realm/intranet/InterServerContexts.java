package koh.realm.intranet;

import koh.patterns.handler.context.Context;

public class InterServerContexts {

    public static class Authenticating implements Context { private Authenticating(){} }
    public static class Authenticated implements Context { private Authenticated(){} }

    public static final Authenticating AUTHENTICATING = new Authenticating();
    public static final Authenticated AUTHENTICATED = new Authenticated();

}
