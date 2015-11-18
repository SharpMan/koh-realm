package koh.realm.refact_network;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import koh.mina.MinaServer;
import koh.mina.api.MinaListener;
import koh.mina.api.annotations.Receive;
import koh.patterns.event.EventExecutor;
import koh.patterns.event.EventListeningProvider;
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.patterns.handler.ConsumerHandlingProvider;
import koh.patterns.handler.SimpleHandlerExecutor;
import koh.patterns.handler.SimpleHandlingProvider;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.protocol.client.Message;
import koh.protocol.client.codec.Dofus2ProtocolDecoder;
import koh.protocol.client.codec.Dofus2ProtocolEncoder;
import koh.realm.app.DatabaseSource;
import koh.realm.app.Logs;
import koh.realm.inter.InterServer;
import koh.realm.utils.Settings;
import org.apache.mina.core.session.IoSession;

import java.io.IOException;

@DependsOn({Logs.class, DatabaseSource.class, InterServer.class})
public class RealmServer implements Service, MinaListener<RealmClient> {

    /**
     * 1 * estimated client optimal size (64)
     */
    private static final int DEFAULT_READ_SIZE = 64;

    /**
     * max used client packet size (realm) + additional size for infos of the next packet
     */
    private static final int MAX_READ_SIZE = 4096 + 0xFF;

    private final MinaServer<RealmClient, Message> minaServer;
    private final Settings settings;

    private final ConsumerHandlerExecutor<RealmClient, Message> messagesExecutor;
    private final SimpleHandlerExecutor<RealmClient> actionsExecutor;
    private final EventExecutor eventsExecutor;

    @Inject
    public RealmServer(Settings settings, Logs logs,
                       @RealmPackage ConsumerHandlerExecutor<RealmClient, Message> messagesExecutor,
                       @RealmPackage EventExecutor eventsExecutor,
                       @RealmPackage SimpleHandlerExecutor<RealmClient> actionsExecutor,
                       Dofus2ProtocolDecoder decoder,
                       Dofus2ProtocolEncoder encoder) {

        this.messagesExecutor = messagesExecutor;
        this.eventsExecutor = eventsExecutor;
        this.actionsExecutor = actionsExecutor;

        this.settings = settings;
        this.minaServer = new MinaServer<>(this::newClient, actionsExecutor,
                messagesExecutor, this, Message.class);

        minaServer.configure(decoder, encoder, DEFAULT_READ_SIZE, MAX_READ_SIZE, 30 * 60, false);
    }

    private RealmClient newClient(IoSession session) {
        return new RealmClient(session, eventsExecutor);
    }

    @Override
    public void start() {
        try {
            minaServer.bind(settings.getStringElement("Login.Host"),
                    settings.getIntElement("Login.Port"));
            System.out.println(settings.getIntElement("Login.Port"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        minaServer.dispose();
    }

    @Override
    public void onException(RealmClient client, Throwable exception) {
        exception.printStackTrace();
        System.out.println(exception.getMessage());
        System.out.println("Error : " + exception.getCause().getMessage());
    }

    @Override
    public void onMessageSent(RealmClient client, Object message) {
        System.out.println("Sent : " + message);
    }

    @Override
    public void inject(Injector injector) {
        injector.createChildInjector(
                new ConsumerHandlingProvider<>(messagesExecutor, injector,
                        "koh.realm.refact_network.handlers", RealmClient.class, Receive.class, Message.class),

                new SimpleHandlingProvider<>(actionsExecutor, injector,
                        "koh.realm.refact_network.handlers", RealmClient.class),

                new EventListeningProvider(eventsExecutor, injector, "koh.realm.refact_network.handlers")
        );
    }
}
