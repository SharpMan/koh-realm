package koh.realm.refact_network;

import koh.patterns.handler.context.Context;

public class RealmContexts {

    public static class CheckingProtocol implements Context { private CheckingProtocol(){} }
    public static class Authenticating implements Context { private Authenticating(){} }
    public static class Authenticated implements Context { private Authenticated(){} }
    public static class Disconnected implements Context { private Disconnected(){} }
    public static class Switching implements Context { private Switching(){} }

    public static final CheckingProtocol CHECKING_PROTOCOL = new CheckingProtocol();
    public static final Authenticating AUTHENTICATING = new Authenticating();
    public static final Authenticated AUTHENTICATED = new Authenticated();
    public static final Disconnected DISCONNECTED = new Disconnected();
    public static final Switching SWITCHING = new Switching();

}
