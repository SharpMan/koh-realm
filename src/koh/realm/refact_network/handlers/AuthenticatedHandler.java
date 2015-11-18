package koh.realm.refact_network.handlers;

import com.google.inject.Inject;
import koh.inter.messages.PlayerCommingMessage;
import koh.mina.api.annotations.Disconnect;
import koh.mina.api.annotations.InactiveTimeout;
import koh.mina.api.annotations.Receive;
import koh.patterns.event.api.EventListener;
import koh.patterns.handler.api.Handler;
import koh.patterns.handler.context.Ctx;
import koh.patterns.handler.context.RequireContexts;
import koh.protocol.client.Message;
import koh.protocol.client.enums.IdentificationFailureReason;
import koh.protocol.client.enums.ServerConnectionError;
import koh.protocol.client.enums.ServerStatusEnum;
import koh.protocol.messages.connection.IdentificationFailedMessage;
import koh.protocol.messages.connection.SelectedServerDataMessage;
import koh.protocol.messages.connection.SelectedServerRefusedMessage;
import koh.protocol.messages.connection.ServerSelectionMessage;
import koh.realm.dao.api.AccountDAO;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.entities.Account;
import koh.realm.entities.GameServer;
import koh.realm.refact_network.RealmClient;
import koh.realm.refact_network.RealmContexts;
import koh.realm.utils.Util;

import java.sql.Timestamp;
import java.time.Instant;

@RequireContexts(@Ctx(RealmContexts.Authenticated.class))
public class AuthenticatedHandler implements Handler, EventListener {

    private final static Message timeOutMessage = new IdentificationFailedMessage(IdentificationFailureReason.TIME_OUT);

    @Inject private GameServerDAO serverDAO;
    @Inject private AccountDAO accountDAO;

    @InactiveTimeout
    public void onInactivityTimeout(RealmClient client) {
        System.out.println("Client timed out : " + client.getRemoteAddress());
        client.disconnect(timeOutMessage);
    }

    @Receive
    public void onSelect(RealmClient client, ServerSelectionMessage message) throws Exception {
        GameServer server;
        try {
            server = serverDAO.getByKey(message.getServerId());
        }catch(Exception ignored) {
            client.write(new SelectedServerRefusedMessage(
                    message.getServerId(), ServerConnectionError.NO_REASON, ServerStatusEnum.STATUS_UNKNOWN));
            return;
        }
        if (server == null) {
            client.write(new SelectedServerRefusedMessage(
                    message.getServerId(), ServerConnectionError.NO_REASON, ServerStatusEnum.STATUS_UNKNOWN));
            return;
        }
        if (server.State != ServerStatusEnum.ONLINE) {
            client.write(new SelectedServerRefusedMessage(message.getServerId(), ServerConnectionError.DUE_TO_STATUS,
                    server.State));
            return;
        }
        if (server.RequiredRole > client.getAccount().get().Right) {
            client.write(new SelectedServerRefusedMessage(message.getServerId(), ServerConnectionError.ACCOUNT_RESTRICTED,
                    server.State));
            return;
        }

        Account acc = client.getAccount().get();
        String ticket = Util.genTicketID(32).toString();

        server.sendPacket(new PlayerCommingMessage(ticket, client.getRemoteAddress().getAddress().toString(), acc.ID,
                acc.NickName, acc.SecretQuestion, acc.SecretAnswer, acc.LastIP, acc.Right, acc.last_login));

        client.getAccount().sync(() -> {
            acc.LastIP = client.getRemoteAddress().getAddress().toString();
            acc.last_login = Timestamp.from(Instant.now());
            accountDAO.save(acc);
        });
        //TODO : Bool createNewCharacter Size

        client.disconnect(new SelectedServerDataMessage(server.ID, server.Address, server.Port, true, ticket));
    }

    @Disconnect
    public void onDisconnect(RealmClient client) {
        System.out.println("Client disconnected : " + client.getRemoteAddress());
        client.disconnect(false);
    }
}
