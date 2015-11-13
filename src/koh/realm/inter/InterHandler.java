package koh.realm.inter;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import koh.inter.InterMessage;
import koh.inter.messages.HelloMessage;
import koh.patterns.event.EventExecutor;
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.patterns.handler.SimpleHandlerExecutor;
import koh.realm.app.Logs;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.entities.GameServer;
import koh.realm.inter.annotations.InterPackage;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author Neo-Craft
 */

public class InterHandler extends IoHandlerAdapter {

    private final Logs logs;
    private final ConsumerHandlerExecutor<GameServer, InterMessage> messagesHandling;

    @Inject
    public InterHandler(Logs logs, @InterPackage SimpleHandlerExecutor<GameServer> actionsHandling,
                        @InterPackage ConsumerHandlerExecutor<GameServer, InterMessage> messagesHandling,
                        @InterPackage EventExecutor eventListening) {
        this.logs = logs;
        SimpleHandlerExecutor<GameServer> actionsHandling1 = actionsHandling;
        this.messagesHandling = messagesHandling;
        EventExecutor eventListening1 = eventListening;

        try {
            messagesHandling.handle(new GameServer(), new HelloMessage());
        } catch (Throwable e) {
            System.out.println(e.getCause());
        }
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        //TODO: Allow Host
    }


    @Inject GameServerDAO gameservers;

    @Override
    public void messageReceived(IoSession session, Object oMsg) throws Exception {
        InterMessage message = (InterMessage) oMsg;
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof GameServer) {
            //TODO(LoadLow) : context management in each Client
            GameServer client = (GameServer) objClient;
            try {
                messagesHandling.handle(client, message);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
            logs.writeInfo("[INFOS] " + client.Name + " recv >> " + message.getClass().getSimpleName());
        } else {
            if (oMsg instanceof HelloMessage) {
                GameServer GameServer = gameservers.getGameServers().stream()
                        .filter(x -> x.Hash.equalsIgnoreCase(((HelloMessage) oMsg).Key)).findFirst().get();
                if (GameServer != null) {
                    session.setAttribute("session", GameServer);
                    GameServer.onConnected(session);
                }
            } else {
                session.close(true);
            }
        }
    }

    @Override
    public void messageSent(IoSession session, Object arg1) throws Exception {
        InterMessage message = (InterMessage) arg1;
        logs.writeDebug("[INFOS] Inter send >> " + message.getClass().getSimpleName());
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof GameServer) {
            GameServer client = (GameServer) objClient;
            client.timeOut();
        }
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof GameServer) {
            GameServer client = (GameServer) objClient;
            client.close();
        }
        session.removeAttribute("session");
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        Object objClient = session.getAttribute("session");
        if (objClient != null && objClient instanceof GameServer) {
            GameServer client = (GameServer) objClient;
            client.close();
        }
    }

}
