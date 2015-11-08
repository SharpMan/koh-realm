package koh.realm.inter;

import koh.inter.InterMessage;
import koh.inter.messages.HelloMessage;
import koh.realm.Main;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.dao.impl.GameServerDAOImpl;
import koh.realm.entities.GameServer;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author Neo-Craft
 */
public class InterHandler extends IoHandlerAdapter {

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        //TODO: Allow Host
    }

    /**
     *
     * @param session
     * @param arg1
     * @throws Exception
     */
    @Override
    public void messageReceived(IoSession session, Object arg1) throws Exception {
        InterMessage message = (InterMessage) arg1;
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof GameServer) {
            GameServer client = (GameServer) objClient;
            client.parsePacket(message);
            Main.Logs().writeInfo("[INFOS] " + client.Name + " recv >> " + message.getClass().getSimpleName());
        } else {
            if (arg1 instanceof HelloMessage) {
                GameServer GameServer = GameServerDAO.get().getGameServers().stream().filter(x -> x.Hash.equalsIgnoreCase(((HelloMessage) arg1).Key)).findFirst().get();
                if (GameServer != null) {
                    session.setAttribute("session", GameServer);
                    GameServer.onConnected(session);
                }
            } else {
                session.close();
            }
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
        InterMessage message = (InterMessage) arg1;
        Main.Logs().writeDebug(new StringBuilder("[INFOS] Inter send >> ").append(message.getClass().getSimpleName()).toString());
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
        if (objClient != null && objClient instanceof GameServer) {
            GameServer client = (GameServer) objClient;
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
        if (objClient != null && objClient instanceof GameServer) {
            GameServer client = (GameServer) objClient;
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
        if (objClient != null && objClient instanceof GameServer) {
            GameServer client = (GameServer) objClient;
            client.close();
        }
    }

}
