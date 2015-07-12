package koh.realm.network;

import koh.protocol.client.Message;
import koh.protocol.messages.connection.HelloConnectMessage;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.protocol.messages.security.RawDataMessage;
import koh.realm.Main;
import koh.realm.utils.Settings;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author Neo-Craft
 */
public class RealmHandler extends IoHandlerAdapter {

    public static byte[] RawBytes;
    public static char[] binaryKeys;

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        session.setAttribute("session", new RealmClient(session));
        session.write(new ProtocolRequired(Settings.GetIntElement("Protocol.requiredVersion"), Settings.GetIntElement("Protocol.currentVersion")));
        session.write(new RawDataMessage((short) RawBytes.length, RawBytes));
    }

    /**
     *
     * @param session
     * @param arg1
     * @throws Exception
     */
    @Override
    public void messageReceived(IoSession session, Object arg1) throws Exception {
        Message message = (Message) arg1;
        Main.Logs().writeDebug(new StringBuilder("[DEBUG] Client recv >> ").append(message.getClass().getSimpleName()).toString());

        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof RealmClient) {
            RealmClient client = (RealmClient) objClient;
            client.parsePacket(message);
        }
    }

    /**
     *
     * @param session
     * @param arg1
     * @throws Exception
     */
    @Override
    public void messageSent(IoSession session, Object arg1) throws Exception {
        Message message = (Message) arg1;
        Main.Logs().writeDebug(new StringBuilder("[DEBUG] Client send >> ").append(message.getClass().getSimpleName()).toString());
    }

    /**
     *
     * @param session
     * @param status
     * @throws Exception
     */
    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof RealmClient) {
            RealmClient client = (RealmClient) objClient;
            client.timeOut();
        }
    }

    /**
     *
     * @param session
     * @throws Exception
     */
    @Override
    public void sessionClosed(IoSession session) throws Exception {
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof RealmClient) {
            RealmClient client = (RealmClient) objClient;
            client.close();
        }
        session.removeAttribute("session");

    }

    /**
     *
     * @param session
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof RealmClient) {
            RealmClient client = (RealmClient) objClient;
            client.close();
        }
    }

}
