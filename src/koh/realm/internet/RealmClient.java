package koh.realm.internet;

import koh.mina.api.MinaClient;
import koh.patterns.event.Event;
import koh.patterns.event.EventExecutor;
import koh.patterns.handler.context.Context;
import koh.protocol.client.MessageTransaction;
import koh.realm.entities.Account;
import koh.realm.internet.events.ClientContextChangedEvent;
import koh.repositories.RepositoryReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.session.IoSession;

import java.util.function.Consumer;

public class RealmClient extends MinaClient {

    private static final Logger logger = LogManager.getLogger("RealmServer");

    private final EventExecutor eventsEmitter;

    public RealmClient(IoSession session, EventExecutor eventsEmitter) {
        super(session, RealmContexts.AUTHENTICATING);
        this.eventsEmitter = eventsEmitter;
    }

    public MessageTransaction startTransaction() {
        return new MessageTransaction(session);
    }

    public void transact(Consumer<MessageTransaction> transaction) {
        try(MessageTransaction trans = this.startTransaction()) {
            transaction.accept(trans);
        }
    }

    @Override
    public void setHandlerContext(Context context) {
        Event<RealmClient> toFire = new ClientContextChangedEvent(this, this.getHandlerContext(), context);
        try {
            super.setHandlerContext(context);
        } finally {
            eventsEmitter.fire(toFire);
            
            this.log((logger) -> logger.info("Context changed to " + context.getClass().getSimpleName()));
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

    public void log(Consumer<Logger> writer) {
        ThreadContext.put("clientAddress", this.getRemoteAddress().getAddress().getHostAddress());
        if(account != null && account.loaded()) {
            ThreadContext.put("accountName", account.get().Username);
            ThreadContext.put("accountId", Integer.toString(account.get().ID));
        }
        try {
            writer.accept(logger);
        } finally {
            ThreadContext.remove("clientAddress");
            if(account != null && account.loaded()) {
                ThreadContext.remove("accountName");
                ThreadContext.remove("accountId");
            }
        }
    }

    @Override
    public CloseFuture disconnect(boolean waitPendingMessages) {
        CloseFuture closing = super.disconnect(waitPendingMessages);

        this.authenticationToken = null;
        if (account != null && account.loaded())
            account.get().setClient(null);
        this.account = null;

        return closing;
    }
}
