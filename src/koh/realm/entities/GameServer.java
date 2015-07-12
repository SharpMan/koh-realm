package koh.realm.entities;

import java.util.ArrayList;
import koh.inter.InterMessage;
import koh.inter.MessageEnum;
import koh.inter.messages.PlayerCreatedMessage;
import koh.protocol.client.Message;
import koh.protocol.client.enums.ServerStatusEnum;
import koh.protocol.client.types.GameServerInformations;
import koh.protocol.messages.connection.ServerStatusUpdateMessage;
import koh.protocol.messages.connection.ServersListMessage;
import koh.realm.Main;
import koh.realm.dao.AccountDAO;
import koh.realm.dao.CharacterDAO;
import koh.realm.dao.GameServerDAO;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author Neo-Craft
 */
public class GameServer {

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
            case PlayerCreated:
                CharacterDAO.Insert(((PlayerCreatedMessage) message).Owner, ID, (short)((PlayerCreatedMessage) message).Count);
                return;
        }

    }

    public void setState(ServerStatusEnum State) {
        this.State = State;
        Main.RealmServer().SendPacket(new ServerStatusUpdateMessage(new GameServerInformations(ID, State, (byte) (State == ServerStatusEnum.FULL ? 1 : 0), true, (byte) 1, 0)));
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

}
