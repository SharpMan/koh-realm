package koh.realm.refact_network;

import koh.mina.api.MinaClient;
import koh.protocol.client.MessageTransaction;
import org.apache.mina.core.session.IoSession;

public class RealmClient extends MinaClient {

    public RealmClient(IoSession session) {
        super(session, RealmContexts.CHECKING_PROTOCOL);
    }

    public MessageTransaction startTransaction() {
        return new MessageTransaction(session);
    }

}
