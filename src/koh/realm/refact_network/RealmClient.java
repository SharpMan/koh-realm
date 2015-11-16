package koh.realm.refact_network;

import com.google.inject.Inject;
import koh.mina.api.MinaClient;
import koh.patterns.event.Event;
import koh.patterns.event.EventExecutor;
import koh.patterns.handler.context.Context;
import koh.protocol.client.MessageTransaction;
import koh.realm.entities.Account;
import koh.realm.refact_network.events.ClientContextChangedEvent;
import koh.repositories.RepositoryReference;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.session.IoSession;

public class RealmClient extends MinaClient {

    public RealmClient(IoSession session) {
        super(session, RealmContexts.AUTHENTICATING);
    }

    public MessageTransaction startTransaction() {
        return new MessageTransaction(session);
    }

    private static @Inject @RealmPackage EventExecutor eventsEmitter;

    @Override
    public void setHandlerContext(Context context) {
        Event<RealmClient> toFire = new ClientContextChangedEvent(this, this.getHandlerContext(), context);
        try {
            super.setHandlerContext(context);
        }finally {
            eventsEmitter.fire(toFire);
        }
    }

    private volatile AuthenticationToken authenticationToken;

    public AuthenticationToken getAuthenticationToken() {
        return authenticationToken;
    }

    public void setAuthenticationToken(AuthenticationToken authenticationToken) {
        this.authenticationToken = authenticationToken;
    }

    private volatile RepositoryReference<Account> account;

    public RepositoryReference<Account> getAccount() {
        return account;
    }

    public void setAccount(RepositoryReference<Account> account) {
        this.account = account;
    }

    @Override
    public synchronized CloseFuture disconnect(boolean waitPendingMessages) {
        CloseFuture closing = super.disconnect(waitPendingMessages);

        this.authenticationToken = null;
        if (account != null && account.loaded())
            account.get().setClient(null);
        this.account = null;

        return closing;
    }
}
