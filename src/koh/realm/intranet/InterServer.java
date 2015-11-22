package koh.realm.intranet;

import com.google.inject.Inject;
import com.google.inject.Injector;
import koh.inter.InterMessage;
import koh.inter.IntercomDecoder;
import koh.inter.IntercomEncoder;
import koh.mina.MinaServer;
import koh.mina.api.MinaListener;
import koh.mina.api.annotations.Receive;
import koh.patterns.ControllersBinder;
import koh.patterns.event.EventExecutor;
import koh.patterns.event.EventListeningProvider;
import koh.patterns.handler.*;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.realm.app.DatabaseSource;
import koh.realm.app.Logs;
import koh.realm.utils.Settings;
import org.apache.mina.core.session.IoSession;

/**
 *
 * @author Neo-Craft
 */

@DependsOn({Logs.class, DatabaseSource.class})
public class InterServer implements Service, MinaListener<GameServerClient> {

    private final MinaServer<GameServerClient, InterMessage> minaServer;
    private final Settings settings;

    private final ConsumerHandlerExecutor<GameServerClient, InterMessage> messagesExecutor;
    private final SimpleHandlerExecutor<GameServerClient> actionsExecutor;
    private final EventExecutor eventsExecutor;

    @Inject
    public InterServer(Settings settings, Logs logs,
                       @InterPackage ConsumerHandlerExecutor<GameServerClient, InterMessage> messagesExecutor,
                       EventExecutor eventsExecutor,
                       @InterPackage SimpleHandlerExecutor<GameServerClient> actionsExecutor,
                       IntercomDecoder decoder,
                       IntercomEncoder encoder) {

        this.messagesExecutor = messagesExecutor;
        this.eventsExecutor = eventsExecutor;
        this.actionsExecutor = actionsExecutor;

        this.settings = settings;
        this.minaServer = new MinaServer<>(this::newClient, actionsExecutor,
                messagesExecutor, this, InterMessage.class);

        minaServer.configure(decoder, encoder, 256, true);
    }

    private GameServerClient newClient(IoSession session) {
        return new GameServerClient(session, eventsExecutor);
    }

    @Override
    public void start() {
        try {
            minaServer.bind(settings.getStringElement("Inter.Host"), settings.getIntElement("Inter.Port"));
            System.out.println(settings.getIntElement("Inter.Port"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        minaServer.dispose();
    }

    @Override
    public void onException(GameServerClient client, Throwable exception) {
        exception.printStackTrace();
        System.out.println(exception.getMessage());
    }

    @Override
    public void onMessageSent(GameServerClient client, Object message) {
        System.out.println("Sent : " + message);
    }

    private static final String HANDLERS_PACKAGE = "koh.realm.intranet.handlers";

    @Override
    public void inject(Injector injector) {
        injector = new ControllersBinder(injector, HANDLERS_PACKAGE).bind();

        injector.createChildInjector(
                new ConsumerHandlingProvider<>(messagesExecutor, injector,
                        HANDLERS_PACKAGE, GameServerClient.class, Receive.class, InterMessage.class),

                new SimpleHandlingProvider<>(actionsExecutor, injector,
                        HANDLERS_PACKAGE, GameServerClient.class),

                new EventListeningProvider(eventsExecutor, injector, HANDLERS_PACKAGE)
        );
    }

    public MinaServer<GameServerClient, InterMessage> getMina() {
        return minaServer;
    }
}
