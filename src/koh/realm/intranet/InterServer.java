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
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.patterns.handler.ConsumerHandlingProvider;
import koh.patterns.handler.SimpleHandlerExecutor;
import koh.patterns.handler.SimpleHandlingProvider;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.realm.dao.DatabaseSource;
import koh.realm.utils.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteToClosedSessionException;

/**
 *
 * @author Neo-Craft
 */

@DependsOn({DatabaseSource.class})
public class InterServer implements Service, MinaListener<GameServerClient> {

    private static final Logger logger = LogManager.getLogger(InterServer.class);

    private MinaServer<GameServerClient, InterMessage> minaServer;

    @Inject
    private Settings settings;

    @Inject
    private @InterPackage ConsumerHandlerExecutor<GameServerClient, InterMessage> messagesExecutor;

    @Inject
    private EventExecutor eventsExecutor;

    @Inject
    private @InterPackage SimpleHandlerExecutor<GameServerClient> actionsExecutor;

    @Inject private IntercomDecoder decoder;

    @Inject private IntercomEncoder encoder;

    private GameServerClient newClient(IoSession session) {
        return new GameServerClient(session, eventsExecutor);
    }

    @Override
    public void start() {
        this.minaServer = new MinaServer<>(this::newClient, actionsExecutor,
                messagesExecutor, this, InterMessage.class);

        minaServer.configure(decoder, encoder, 256, true);
        minaServer.getSessionConfig().setIdleTime(IdleStatus.BOTH_IDLE, 15);

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
        exception.printStackTrace();

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
