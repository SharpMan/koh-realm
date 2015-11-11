package koh.realm.app;

import com.google.inject.*;
import koh.commons.ImprovedCachedThreadPool;
import koh.inter.InterMessage;
import koh.patterns.event.EventExecutor;
import koh.patterns.handler.ConsumerHandlerExecutor;
import koh.patterns.handler.SimpleHandlerExecutor;
import koh.protocol.client.Message;
import koh.realm.entities.GameServer;
import koh.realm.inter.annotations.InterPackage;
import koh.realm.network.RealmClient;
import koh.realm.network.annotations.RealmPackage;

public class AppModule extends AbstractModule {

    private final Injector services;

    public AppModule() {
        this.services = Guice.createInjector()
                .createChildInjector(new ServicesModule());
    }

    public Injector launch() {
        Injector base = services.createChildInjector(this);
        AbstractModule core = new CoreModule(base);
        base.injectMembers(core);
        return base.createChildInjector(core);
    }

    @Override
    protected void configure() {
    }

    @InterPackage @Provides
    @Singleton
    ConsumerHandlerExecutor<GameServer, InterMessage> provideInterMessagesExecutor() {
        return new ConsumerHandlerExecutor<>();
    }

    @InterPackage @Provides
    @Singleton
    SimpleHandlerExecutor<GameServer> provideInterActionsExecutor() {
        return new SimpleHandlerExecutor<>();
    }

    @InterPackage @Provides
    @Singleton
    EventExecutor provideInterEventsExecutor() {
        return new EventExecutor(new ImprovedCachedThreadPool("InterEventsExecutor", 10, 50));
    }

    @RealmPackage @Provides
    @Singleton
    EventExecutor provideRealmEventsExecutor() {
        return new EventExecutor(new ImprovedCachedThreadPool("InterEventsExecutor", 10, 50));
    }

    @RealmPackage @Provides
    @Singleton
    ConsumerHandlerExecutor<RealmClient, Message> provideRealmMessagesExecutor() {
        return new ConsumerHandlerExecutor<>();
    }

    @RealmPackage @Provides
    @Singleton
    SimpleHandlerExecutor<RealmClient> provideRealmActionsExecutor() {
        return new SimpleHandlerExecutor<>();
    }

}
