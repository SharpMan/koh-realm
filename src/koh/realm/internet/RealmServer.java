package koh.realm.internet;

import com.google.inject.Inject;
import com.google.inject.Injector;
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
import koh.protocol.client.Message;
import koh.protocol.client.codec.Dofus2ProtocolDecoder;
import koh.protocol.client.codec.Dofus2ProtocolEncoder;
import koh.realm.dao.DatabaseSource;
import koh.realm.dao.api.AccountDAO;
import koh.realm.dao.api.CharacterDAO;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.intranet.InterServer;
import koh.realm.utils.Settings;
import org.apache.logging.log4j.*;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.write.WriteToClosedSessionException;

import java.io.IOException;
import java.util.function.Consumer;

@DependsOn({DatabaseSource.class, InterServer.class})
public class RealmServer implements Service, MinaListener<RealmClient> {

    private static final Logger logger = LogManager.getLogger("RealmServer");

    /**
     * 1 * estimated client optimal size (64)
     */
    private static final int DEFAULT_READ_SIZE = 64;

    /**
     * max used client packet size (realm) + additional size for infos of the next packet
     */
    private static final int MAX_READ_SIZE = 4096 + 0xFF;

    private MinaServer<RealmClient, Message> minaServer;

    @Inject
    private Settings settings;

    @Inject
    private @RealmPackage ConsumerHandlerExecutor<RealmClient, Message> messagesExecutor;

    @Inject
    private @RealmPackage SimpleHandlerExecutor<RealmClient> actionsExecutor;

    @Inject
    private EventExecutor eventsExecutor;

    @Inject
    private Dofus2ProtocolDecoder decoder;

    @Inject
    private Dofus2ProtocolEncoder encoder;

    private RealmClient newClient(IoSession session) {
        return new RealmClient(session, eventsExecutor);
    }

    @Override
    public void start() {
        this.minaServer = new MinaServer<>(this::newClient, actionsExecutor,
                messagesExecutor, this, Message.class);

        minaServer.configure(decoder, encoder, DEFAULT_READ_SIZE, MAX_READ_SIZE, 30 * 60, false);

        try {
            minaServer.bind(settings.getStringElement("Login.Host"),
                    settings.getIntElement("Login.Port"));
            logger.info("RealmServer bound on {}:{}",
                    settings.getStringElement("Login.Host"), settings.getIntElement("Login.Port"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void stop() {
        minaServer.dispose();
    }

    private static final Marker EXC_MARKER = MarkerManager.getMarker("REALM_EXC_CATCH");

    @Override
    public void onException(RealmClient client, Throwable exception) {
        if(exception instanceof WriteToClosedSessionException)
            return; //ignore

        client.log((logger) -> logger.error(EXC_MARKER, exception.getMessage(), exception));
    }

    private static final Marker MSG_SENT_MARKER = MarkerManager.getMarker("REALM_MSG_SENT");

    @Override
    public void onMessageSent(RealmClient client, Object message) {
        if(message == null)
            return;

        client.log((logger) -> logger.debug(MSG_SENT_MARKER, message));
    }

    private static final Marker MSG_RECVD_MARKER = MarkerManager.getMarker("REALM_MSG_RECV");

    @Override
    public void onReceived(RealmClient client, Object message) {
        if(message == null)
            return;

        client.log((logger) -> logger.info(MSG_RECVD_MARKER, message));

    }

    private static final String HANDLERS_PACKAGE = "koh.realm.internet.handlers";

    @Override
    public void inject(Injector injector) {
        injector = new ControllersBinder(injector, HANDLERS_PACKAGE).bind();

        injector.createChildInjector(
                new ConsumerHandlingProvider<>(messagesExecutor, injector,
                        HANDLERS_PACKAGE, RealmClient.class, Receive.class, Message.class),

                new SimpleHandlingProvider<>(actionsExecutor, injector,
                        HANDLERS_PACKAGE, RealmClient.class),

                new EventListeningProvider(eventsExecutor, injector, HANDLERS_PACKAGE)
        );
    }

    public MinaServer<RealmClient, Message> getMina() {
        return minaServer;
    }
}
