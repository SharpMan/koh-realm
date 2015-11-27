package koh.realm.intranet;

import com.google.inject.Binding;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
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
import koh.realm.dao.api.AccountDAO;
import koh.realm.dao.api.CharacterDAO;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.utils.Settings;
import org.apache.logging.log4j.*;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteToClosedSessionException;

import java.util.Map;

/**
 *
 * @author Neo-Craft
 */

@DependsOn({GameServerDAO.class, AccountDAO.class, CharacterDAO.class})
public class InterServer implements Service, MinaListener<GameServerClient> {

    private static final Logger logger = LogManager.getLogger(InterServer.class);

    private final MinaServer<GameServerClient, InterMessage> minaServer;
    private final Settings settings;

    private final ConsumerHandlerExecutor<GameServerClient, InterMessage> messagesExecutor;
    private final SimpleHandlerExecutor<GameServerClient> actionsExecutor;
    private final EventExecutor eventsExecutor;

    @Inject
    public InterServer(Settings settings,
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
            logger.info("InterServer bound on {}:{}",
                    settings.getStringElement("Inter.Host"), settings.getIntElement("Inter.Port"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        minaServer.dispose();
    }

    private static final Marker EXC_MARKER = MarkerManager.getMarker("INTER_EXC_CATCH");

    @Override
    public void onException(GameServerClient client, Throwable exception) {
        if(exception instanceof WriteToClosedSessionException)
            return; //ignore

        logger.error(EXC_MARKER, exception.getMessage(), exception);
    }

    private static final Marker MSG_SENT_MARKER = MarkerManager.getMarker("INTER_MSG_SENT");

    @Override
    public void onMessageSent(GameServerClient client, Object message) {
        if(message == null)
            return;

        logger.debug(MSG_SENT_MARKER, message);

    }

    private static final Marker MSG_RECVD_MARKER = MarkerManager.getMarker("INTER_MSG_RECV");

    @Override
    public void onReceived(GameServerClient client, Object message) {
        if(message == null)
            return;

        logger.debug(MSG_RECVD_MARKER, message);
    }

    private static final String HANDLERS_PACKAGE = "koh.realm.intranet.handlers";

    @Override
    public void inject(Injector injector) {
        Map<Key<?>,Binding<?>> map = injector.getBindings();
        for(Map.Entry<Key<?>, Binding<?>> e : map.entrySet()) {
            System.out.println(e.getKey() + ": " + e.getValue());
        }

        injector = new ControllersBinder(injector, HANDLERS_PACKAGE).bind();

        injector = injector.createChildInjector(
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
