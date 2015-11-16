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
import koh.protocol.client.Message;
import koh.protocol.client.MessageTransaction;
import koh.protocol.client.enums.IdentificationFailureReason;
import koh.protocol.messages.connection.CredentialsAcknowledgementMessage;
import koh.protocol.messages.connection.IdentificationFailedMessage;
import koh.protocol.messages.connection.IdentificationMessage;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.protocol.messages.security.RawDataMessage;
import koh.realm.refact_network.AuthenticationToken;
import koh.realm.refact_network.RealmClient;
import koh.realm.refact_network.RealmContexts;
import koh.realm.utils.Settings;
import koh.utils.LambdaCloseable;

@RequireContexts(@Ctx(RealmContexts.Authenticating.class))
public class AuthenticatingHandler implements Handler, EventListener {

    @Inject Settings settings;

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
        System.out.println("New client : " + client.getRemoteAddress() + " => " + client.getLocalAddress());
        try(MessageTransaction transact = client.startTransaction()) {
            transact.write(protocolRequiredMessage);
            transact.write(authenticationBypasser);
        }
    }

    private final static Message credentialsAck = new CredentialsAcknowledgementMessage();

    @Receive
    public void authenticate(RealmClient client, IdentificationMessage message) {
        client.setAuthenticationToken(new AuthenticationToken(message));
        client.setHandlerContext(RealmContexts.IN_WAITING_QUEUE);
        client.write(credentialsAck);
    }

}
