package koh.realm.internet.handlers;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import koh.mina.api.annotations.Connect;
import koh.mina.api.annotations.Disconnect;
import koh.mina.api.annotations.Receive;
import koh.patterns.Controller;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.protocol.client.Message;
import koh.protocol.client.PregenMessage;
import koh.protocol.client.codec.Dofus2ProtocolEncoder;
import koh.protocol.messages.connection.CredentialsAcknowledgementMessage;
import koh.protocol.messages.connection.IdentificationMessage;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.protocol.messages.security.RawDataMessage;
import koh.realm.internet.AuthenticationToken;
import koh.realm.internet.RealmClient;
import koh.realm.internet.RealmContexts;
import org.apache.mina.core.buffer.IoBuffer;

@RequireContexts(@Ctx(RealmContexts.Authenticating.class))
public class AuthenticatingHandler implements Controller {

    private final PregenMessage welcomeMessageBuffer;

    @Inject
    public AuthenticatingHandler(@Named("Messages.AuthenticationBypasser") RawDataMessage authenticationBypasser,
                                 @Named("Messages.ProtocolRequired") ProtocolRequired protocolRequiredMessage,
                                 Dofus2ProtocolEncoder encoder) {

        IoBuffer msgBuffer = IoBuffer.allocate(authenticationBypasser.getContent().length + 16);

        encoder.encodeMessage(authenticationBypasser, msgBuffer);
        encoder.encodeMessage(protocolRequiredMessage, msgBuffer);

        this.welcomeMessageBuffer = new PregenMessage(msgBuffer);
    }

    @Connect
    public void onConnect(RealmClient client) {
        System.out.println("Client connected : " + client.getRemoteAddress());
        client.write(welcomeMessageBuffer);
    }

    private final static Message credentialsAck = new CredentialsAcknowledgementMessage();

    @Receive
    public void authenticate(RealmClient client, IdentificationMessage message) {
        client.setAuthenticationToken(new AuthenticationToken(message));
        client.setHandlerContext(RealmContexts.IN_WAITING_QUEUE);
        client.write(credentialsAck);
    }

    @Disconnect
    public void onDisconnect(RealmClient client) {
        System.out.println("Client disconnected : " + client.getRemoteAddress());
    }

}
