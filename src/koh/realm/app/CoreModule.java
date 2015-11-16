package koh.realm.app;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import koh.inter.InterMessage;
import koh.patterns.event.EventExecutor;
import koh.patterns.event.EventListeningProvider;
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.patterns.handler.ConsumerHandlingProvider;
import koh.patterns.handler.SimpleHandlerExecutor;
import koh.patterns.handler.SimpleHandlingProvider;
import koh.realm.entities.GameServer;
import koh.realm.inter.annotations.InterPackage;
import koh.realm.inter.annotations.ReceiveInterMessage;

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
