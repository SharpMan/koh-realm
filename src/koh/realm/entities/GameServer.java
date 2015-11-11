package koh.realm.entities;

import koh.inter.InterMessage;
import koh.inter.MessageEnum;
import koh.inter.messages.PlayerCreatedMessage;
import koh.patterns.handler.api.HandlerEmitter;
import koh.patterns.handler.context.Context;
import koh.protocol.client.enums.ServerStatusEnum;
import koh.protocol.client.types.GameServerInformations;
import koh.protocol.messages.connection.ServerStatusUpdateMessage;
import koh.realm.Main;
import koh.realm.dao.api.CharacterDAO;
import koh.realm.dao.impl.CharacterDAOImpl;
import koh.realm.inter.contexts.Authenticating;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author Neo-Craft
 */
public class GameServer implements HandlerEmitter {

    public short ID;
    public String Name, Adress, Hash;
    public short Port;
    public byte RequiredRole;
    public ServerStatusEnum State = ServerStatusEnum.OFFLINE;

    public IoSession session;

    public void parsePacket(InterMessage message) {
        if (message == null) {
            return;
        }
        switch (MessageEnum.valueOf(message.getMessageId())) {
            case HelloMessage:
                break;
            case PlayerCommingMessage:
                break;
            case ExpulseAccount:
                break;
            case PlayerCreated:
                //CharacterDAO.get().insertOrUpdate(((PlayerCreatedMessage) message).Owner, ID, (short)((PlayerCreatedMessage) message).Count);
                return;
        }

    }

    public void setState(ServerStatusEnum State) {
        this.State = State;
        //Main.RealmServer().SendPacket(new ServerStatusUpdateMessage(new GameServerInformations(ID, State, (byte) (State == ServerStatusEnum.FULL ? 1 : 0), true, (byte) 1, 0)));
    }

    /**
     * 
     * @param packet
     */
    public void sendPacket(InterMessage packet) {
        if (packet == null || session == null || !session.isConnected()) {
            return;
        }
        session.write(packet);
    }

    public void timeOut() {
        close();
    }

    public void onConnected(IoSession session) {
        this.session = session;
        setState(ServerStatusEnum.ONLINE);
        System.out.println("[INFOS] GameServer " + Name + " Online");
    }

    public void close() {
        if (!session.isConnected()) {
            session.close(true);
            this.session = null;
            setState(ServerStatusEnum.OFFLINE);
        }
    }

    @Override
    public void setHandlerContext(Context context) {

    }

    @Override
    public Context getHandlerContext() {
        return new Authenticating();
    }
}
