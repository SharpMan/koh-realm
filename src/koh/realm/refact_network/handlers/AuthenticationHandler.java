package koh.realm.refact_network.handlers;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import koh.concurrency.WaitingQueue;
import koh.mina.api.annotations.Connect;
import koh.mina.api.annotations.Disconnect;
import koh.mina.api.annotations.InactiveTimeout;
import koh.mina.api.annotations.Receive;
import koh.patterns.event.api.EventListener;
import koh.patterns.handler.api.Handler;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.protocol.client.MessageTransaction;
import koh.protocol.messages.connection.IdentificationMessage;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.protocol.messages.security.RawDataMessage;
import koh.realm.refact_network.RealmClient;
import koh.realm.refact_network.RealmContexts;
import koh.realm.utils.Settings;
import koh.utils.LambdaCloseable;

@RequireContexts(@Ctx(RealmContexts.Authenticating.class))
public class AuthenticationHandler implements Handler, EventListener {

    @Inject
    Settings settings;

    @Inject @Named("Messages.AuthenticationBypasser")
    RawDataMessage authenticationBypasser;
    @Inject @Named("Messages.ProtocolRequired")
    ProtocolRequired protocolRequiredMessage;

    /*
        TODO pregen messages (AuthenticationBypasser / ProtocolRequired)
      transact.write(new ProtocolRequired(settings.getIntElement("Protocol.requiredVersion"),
     settings.getIntElement("Protocol.currentVersion")));

     */


    @Connect
    public void onConnect(RealmClient client) {
        try(MessageTransaction transact = client.startTransaction()) {
            transact.write(protocolRequiredMessage);
            transact.write(authenticationBypasser);
        }
    }

    private final WaitingQueue<AuthenticationToken> queue = new WaitingQueue<>(20, this::treatWaiting);

    private class AuthenticationToken {
        final RealmClient target;
        final IdentificationMessage value;

        public AuthenticationToken(RealmClient target, IdentificationMessage value) {
            this.target = target;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            AuthenticationToken that = (AuthenticationToken) o;

            return !(target != null ? !target.equals(that.target) : that.target != null);
        }

        @Override
        public int hashCode() {
            return target != null ? target.hashCode() : 0;
        }
    }

    private void treatWaiting(AuthenticationToken token) {
        RealmClient client = token.target;
        try(LambdaCloseable suspend = client.suspendReads()) {
            IdentificationMessage message = token.value;


        }

    }

    @Receive
    public void authenticate(RealmClient client, IdentificationMessage message) {
        queue.push(new AuthenticationToken(client, message));
    }

    @InactiveTimeout
    public void onInactivityTimeout(RealmClient client) {
        //TODO Send inactive disconnection
        client.disconnect();
    }

    @Disconnect
    public void onDisconnect(RealmClient client) {
        queue.remove(new AuthenticationToken(client, null));
    }

}
