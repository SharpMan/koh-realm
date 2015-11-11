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
import koh.realm.entities.GameServer;
import koh.realm.inter.InterServer;
import koh.realm.inter.annotations.InterPackage;
import koh.realm.inter.annotations.ReceiveInterMessage;
import koh.realm.network.RealmClient;
import koh.realm.network.RealmServer;
import koh.realm.network.annotations.RealmPackage;
import koh.realm.network.annotations.Receive;

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

    @Inject @RealmPackage
    private ConsumerHandlerExecutor<RealmClient, Message> realmMessagesExecutor;
    @Inject @RealmPackage
    private SimpleHandlerExecutor<RealmClient> realmActionsExecutor;
    @Inject @RealmPackage
    private EventExecutor realmEventsExecutor;

    @Override
    protected void configure() {
        parent.createChildInjector(

                new ConsumerHandlingProvider<>(interMessagesExecutor, parent,
                "koh.realm.inter", GameServer.class, ReceiveInterMessage.class, InterMessage.class),

                new SimpleHandlingProvider<>(interActionsExecutor, parent,
                        "koh.realm.inter", GameServer.class),

                new EventListeningProvider(interEventsExecutor, parent, "koh.realm.inter"),

                new ConsumerHandlingProvider<>(realmMessagesExecutor, parent,
                        "koh.realm.network", RealmClient.class, Receive.class, Message.class),

                new SimpleHandlingProvider<>(realmActionsExecutor, parent,
                        "koh.realm.network", RealmClient.class),

                new EventListeningProvider(realmEventsExecutor, parent, "koh.realm.network")

        );

        bind(InterServer.class).asEagerSingleton();
        bind(RealmServer.class).asEagerSingleton();
    }

}
