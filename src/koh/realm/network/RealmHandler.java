package koh.realm.network;

import com.google.inject.Inject;
import koh.protocol.client.Message;
import koh.protocol.client.MessageQueue;
import koh.protocol.messages.connection.HelloConnectMessage;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.protocol.messages.security.RawDataMessage;
import koh.realm.Logs;
import koh.realm.Main;
import koh.realm.handlers.HandleMethod;
import koh.realm.handlers.HandlersProvider;
import koh.realm.utils.Settings;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author Neo-Craft
 */
public class RealmHandler extends IoHandlerAdapter {

    private final byte[] rawBytes = null;
    private final char[] binaryKeys = null ;

    private final Settings settings;
    private final Logs logs;
    private final HandlersProvider<RealmClient, Message> handlers;

    @Inject
    public RealmHandler(Settings settings, Logs logs, HandlersProvider<RealmClient, Message> handlers) {

        this.logs = logs;
        this.settings = settings;
        this.handlers = handlers;


    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        session.setAttribute("session", new RealmClient(session));
        session.write(new ProtocolRequired(settings.getIntElement("Protocol.requiredVersion"),
                settings.getIntElement("Protocol.currentVersion")));
        session.write(new RawDataMessage((short) rawBytes.length, rawBytes));
    }

    @Override
    public void messageReceived(IoSession session, Object arg1) throws Exception {
        Message message = (Message) arg1;
        logs.writeDebug("[DEBUG] Client recv >> " + message.getClass().getSimpleName());

        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof RealmClient) {
            RealmClient client = (RealmClient) objClient;
            handlers.getLambdas(message.getClass()).anyMatch((method) -> {
                try {
                    return method.handle(client, message);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                    return false;
                }
            });
            //client.parsePacket(message);
        }
    }

   @Override
    public void messageSent(IoSession session, Object arg1) throws Exception {
        if (arg1 instanceof Message) {
            Main.Logs().writeDebug(new StringBuilder("[DEBUG] Client send >> ").append(((Message) arg1).getClass().getSimpleName()).toString());
        } else if (arg1 instanceof MessageQueue) {
            for (Message message : ((MessageQueue) arg1).get()) {
                Main.Logs().writeDebug(new StringBuilder("[DEBUG] Client send >> ").append(message.getClass().getSimpleName()).toString());
            }
        } else{
            throw new NullPointerException();
        }
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof RealmClient) {
            RealmClient client = (RealmClient) objClient;
            client.timeOut();
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof RealmClient) {
            RealmClient client = (RealmClient) objClient;
            client.close();
        }
        session.removeAttribute("session");

    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        Object objClient = session.getAttribute("session");
        if(Logs.DEBUG)
           cause.printStackTrace();
        if (objClient != null && objClient instanceof RealmClient) {
            RealmClient client = (RealmClient) objClient;
            client.close();
        }
    }

}