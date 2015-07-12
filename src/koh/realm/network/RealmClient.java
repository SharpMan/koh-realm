package koh.realm.network;

import java.net.InetSocketAddress;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import koh.cypher.RSACiphers;
import koh.inter.messages.ExpulseAccountMessage;
import koh.inter.messages.PlayerCommingMessage;
import koh.protocol.client.BufUtils;
import koh.protocol.client.Message;
import koh.protocol.client.enums.IdentificationFailureReason;
import koh.protocol.client.enums.ServerConnectionError;
import koh.protocol.client.enums.ServerStatusEnum;
import koh.protocol.client.types.GameServerInformations;
import koh.protocol.messages.connection.CredentialsAcknowledgementMessage;
import koh.protocol.messages.connection.IdentificationFailedMessage;
import koh.protocol.messages.connection.IdentificationMessage;
import koh.protocol.messages.connection.IdentificationSuccessMessage;
import koh.protocol.messages.connection.LoginQueueStatusMessage;
import koh.protocol.messages.connection.SelectedServerDataMessage;
import koh.protocol.messages.connection.SelectedServerRefusedMessage;
import koh.protocol.messages.connection.ServerSelectionMessage;
import koh.protocol.messages.connection.ServersListMessage;
import koh.realm.Main;
import koh.realm.dao.AccountDAO;
import koh.realm.dao.AccountReference;
import koh.realm.dao.GameServerDAO;
import koh.realm.entities.Account;
import koh.realm.entities.GameServer;
import koh.realm.utils.Util;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author Neo-Craft
 */
public final class RealmClient {

    public enum State {

        CHECK_ACCOUNT,
        ON_GAMESERVER_LIST;
    }

    private final IoSession session;
    public State ClientState = State.CHECK_ACCOUNT;
    private IdentificationMessage toThreat;
    public AccountReference Compte;
    public boolean showQueue;
    private RSACiphers ciphers;

    RealmClient(IoSession session) {
        this.session = session;
        try {
            this.ciphers = new RSACiphers(2048);
        } catch (Exception e) {
            Main.Logs().writeError("Can't instantiate RSA ciphers because : {}" + e.getMessage());
        }
    }

    public String getIP() {
        return ((InetSocketAddress) this.session.getRemoteAddress()).getAddress().toString();
    }

    public void parsePacket(Message message) {
        if (message == null) {
            return;
        }
        switch (ClientState) {
            case CHECK_ACCOUNT:
                toThreat = (IdentificationMessage) message;
                AccountDAO.Loader.addClient(this);
                if (toThreat != null && AccountDAO.Loader.getPosition(this) != 1) {
                    this.sendPacket(new LoginQueueStatusMessage((short) AccountDAO.Loader.getPosition(this), (short) AccountDAO.Loader.getTotal()));
                    this.showQueue = true;
                }
                break;
            case ON_GAMESERVER_LIST:
                switch (message.getMessageId()) {
                    case ServerSelectionMessage.MESSAGE_ID:
                        GameServer Server = GameServerDAO.getGameServer(((ServerSelectionMessage) message).getServerId());
                        if (Server == null) {
                            this.sendPacket(new SelectedServerRefusedMessage(((ServerSelectionMessage) message).getServerId(), ServerConnectionError.DUE_TO_STATUS, ServerStatusEnum.STATUS_UNKNOWN));
                            break;
                        } else if (Server.State != ServerStatusEnum.ONLINE) {
                            this.sendPacket(new SelectedServerRefusedMessage(((ServerSelectionMessage) message).getServerId(), ServerConnectionError.DUE_TO_STATUS, Server.State));
                            break;
                        } else if (Server.RequiredRole > this.Compte.get().Right) {
                            this.sendPacket(new SelectedServerRefusedMessage(((ServerSelectionMessage) message).getServerId(), ServerConnectionError.ACCOUNT_RESTRICTED, Server.State));
                            break;
                        }
                        String Ticket = Util.genTicketID(32).toString();
                        Server.sendPacket(new PlayerCommingMessage(Ticket, getIP(), this.Compte.get().ID, this.Compte.get().NickName, this.Compte.get().SecretQuestion, this.Compte.get().SecretAnswer, this.Compte.get().LastIP, this.Compte.get().Right, this.Compte.get().last_login));
                        this.Compte.get().LastIP = this.getIP();
                        this.Compte.get().last_login = Timestamp.from(Instant.now());
                        AccountDAO.Save(this.Compte.get());
                        //TODO : Bool createNewCharacter Size
                        this.sendPacket(new SelectedServerDataMessage(Server.ID, Server.Adress, Server.Port, true, Ticket));
                        this.timeOut();

                        break;
                }
                break;
        }
    }

    /**
     *
     * @param packet
     */
    public void sendPacket(Message packet) {
        if (packet == null) {
            return;
        }
        session.write(packet);
    }

    public void timeOut() {
        close();
    }

    public void threatWaiting() {
        if (toThreat == null) {
            return;
        }
        try {
            if (this.showQueue) {
                this.sendPacket(new LoginQueueStatusMessage((short) 0, (short) 0));
            }
            this.sendPacket(new CredentialsAcknowledgementMessage());
            IoBuffer a = IoBuffer.wrap(this.toThreat.getCredentials());
            String AccountName = BufUtils.readUTF(a);
            String Pass = BufUtils.readUTF(a);

            a.clear();

            AccountReference ref = AccountDAO.getCompteByName(AccountName);

            if (ref != null && ref.isLogged()) {
                Account to_compare = ref.get();
                if (!Account.COMPTE_LOGIN(to_compare, AccountName, Pass)) {
                    this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.UNKNOWN_AUTH_ERROR));
                    return;
                }
                Compte = ref;

                if (Compte.get().isBanned()) {
                    this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.BANNED));
                } else {
                    this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.TOO_MANY_ON_IP)); //Already Connected
                    Compte.get().Client.timeOut(); //Todo WAS Disconnected account message
                }

            } else //Si le compte n'a pas été reconnu
            {
                Account to_compare;
                try {
                    to_compare = AccountDAO.Find(AccountName, this);
                } catch (NullPointerException e) {
                    this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.TOO_MANY_ON_IP));
                    return;
                } catch (Exception ex) {
                    this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.TIME_OUT));
                    return;
                }

                if (Account.COMPTE_LOGIN(to_compare, AccountName, Pass)) {

                    Compte = AccountDAO.initReference(to_compare);

                    if (Compte.isLogged()) {
                        this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.TOO_MANY_ON_IP)); //Already Connected
                        Compte.get().Client.timeOut(); //Todo WAS Disconnected account message
                        return;
                    }
                    if (to_compare.isBanned()) {
                        /*TODO : DayNumber if (to_compare.getDaysBanned() >= 1) {
                         PacketsManager.SEND_BANNED_FORDAYS(_out, to_compare.getDaysBanned() + "");
                         BlockNextPackets = true;
                         return;
                         }*/
                        this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.BANNED));
                        return;
                    }
                    GameServerDAO.getGameServers().stream().forEach((g) -> {
                        g.sendPacket(new ExpulseAccountMessage(to_compare.ID));
                    });
                    try {
                        AccountDAO.addAccount(to_compare);
                    } catch (NullPointerException e) {
                        this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.BANNED));
                        return;
                    }

                    Compte.setLogged(to_compare);
                    //SQLManager.LOAD_CHARACTERS_BY_ACCOUNT(_compte.get());
                    Compte.get().Client = this;
                    setTimeout(90000);
                    this.toThreat = null;
                    this.ClientState = State.ON_GAMESERVER_LIST;
                    this.sendPacket(new IdentificationSuccessMessage(Compte.get().Username, Compte.get().NickName, Compte.get().ID,/*CommunatyID*/ 0, Compte.get().Right > 0, Compte.get().SecretQuestion, Instant.now().getEpochSecond() * 1000, false));
                    this.sendPacket(new ServersListMessage(new ArrayList<GameServerInformations>() {
                        {
                            GameServerDAO.getGameServers().stream().forEach((G) -> {
                                add(new GameServerInformations(G.ID, G.State, (byte) (G.State == ServerStatusEnum.FULL ? 1 : 0), true, Compte.get().getPlayers(G.ID), 0));
                            });
                        }
                    }));

                } else //Si le compte n'a pas été reconnu
                {
                    this.sendPacket(new IdentificationFailedMessage(IdentificationFailureReason.WRONG_CREDENTIALS));
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    public void setTimeout(int ms) {
        //TODO : Timer 
    }

    public void close() {
        if (session != null && !session.isClosing()) {
            session.close(true);
        }
        if (toThreat != null) {
            toThreat = null;
            AccountDAO.Loader.onClientDisconnect(this);
        }
        if (Compte != null) {
            Compte.setLogged(null);
            Compte = null;
        }
    }
}
