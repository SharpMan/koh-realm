package koh.realm.entities;

import koh.protocol.client.enums.ServerStatusEnum;
import koh.protocol.client.types.GameServerInformations;
import koh.realm.intranet.GameServerClient;
import koh.realm.intranet.events.ServerStatusChangedEvent;

/**
 *
 * @author Neo-Craft
 */
public class GameServer {

    public short ID;
    public String Name, Address, Hash;
    public short Port;
    public byte RequiredRole;

    private volatile ServerStatusEnum status = ServerStatusEnum.OFFLINE;
    private GameServerClient client;

    public GameServerClient getClient() {
        return client;
    }

    public void setClient(GameServerClient client) {
        this.client = client;
    }

    public ServerStatusEnum getStatus() {
        return status;
    }

    public void setStatus(ServerStatusEnum status) {
        this.status = status;

        if (client != null)
            client.emit(new ServerStatusChangedEvent(client, this));
    }

    public void setOffline() {
        if (client != null)
            setStatus(ServerStatusEnum.OFFLINE);
        this.client = null;
    }

    public GameServerInformations toInformations() {
        return new GameServerInformations(
                ID, status, (byte) (status == ServerStatusEnum.FULL ? 1 : 0), true, (byte) 1, 0
        );
    }

}
