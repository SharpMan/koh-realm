package koh.realm.app;

import com.google.inject.*;
import com.google.inject.name.Named;
import koh.inter.InterMessage;
import koh.patterns.event.EventExecutor;
import koh.patterns.event.EventListeningProvider;
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.patterns.handler.ConsumerHandlingProvider;
import koh.patterns.handler.SimpleHandlerExecutor;
import koh.patterns.handler.SimpleHandlingProvider;
import koh.protocol.client.Message;
import koh.protocol.client.codec.Dofus2ProtocolDecoder;
import koh.protocol.messages.handshake.ProtocolRequired;
import koh.protocol.messages.security.RawDataMessage;
import koh.realm.Main;
import koh.realm.entities.GameServer;
import koh.realm.inter.annotations.InterPackage;
import koh.realm.inter.annotations.ReceiveInterMessage;
import koh.realm.refact_network.RealmPackage;
import koh.realm.utils.Settings;
import org.apache.mina.filter.codec.ProtocolDecoder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

class CoreModule extends AbstractModule {

    private final Injector parent;

    public CoreModule(Injector parent) {
        this.parent = parent;
    }

    @Inject @InterPackage
    private ConsumerHandlerExecutor<GameServer, InterMessage> interMessagesExecutor;
    @Inject @InterPackage
    private SimpleHandlerExecutor<GameServer> interActionsExecutor;
    @Inject @InterPackage
    private EventExecutor interEventsExecutor;

    @Override
    protected void configure() {
        parent.createChildInjector(

                new ConsumerHandlingProvider<>(interMessagesExecutor, parent,
                "koh.realm.inter", GameServer.class, ReceiveInterMessage.class, InterMessage.class),

                new SimpleHandlingProvider<>(interActionsExecutor, parent,
                        "koh.realm.inter", GameServer.class),

                new EventListeningProvider(interEventsExecutor, parent, "koh.realm.inter")
        );
    }

}
