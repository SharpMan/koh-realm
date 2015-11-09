package koh.realm;

import com.google.inject.*;
import koh.inter.InterMessage;
import koh.protocol.client.Message;
import koh.realm.dao.DAOModule;
import koh.realm.entities.GameServer;
import koh.realm.handlers.HandlersProvider;
import koh.realm.inter.InterServer;
import koh.realm.inter.ReceiveInterMessage;
import koh.realm.network.RealmClient;
import koh.realm.network.RealmServer;
import koh.realm.network.Receive;
import koh.realm.utils.Settings;

public class RealmModule extends AbstractModule {

    private final Injector parent;
    public RealmModule(Injector parent) {
        this.parent = parent;
    }

    @Override
    protected void configure() {
        bind(Logs.class).in(Scopes.SINGLETON);
        bind(DatabaseSource.class).in(Scopes.SINGLETON);

        install(new DAOModule());

        bind(InterServer.class).in(Scopes.SINGLETON);
        bind(RealmServer.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Singleton
    HandlersProvider<GameServer, InterMessage> provideInterMessagesHandlers() {
        HandlersProvider<GameServer, InterMessage> interMessagesHandlers = new HandlersProvider<>(parent,
                "koh.realm.inter", ReceiveInterMessage.class, InterMessage.class);
        parent.createChildInjector(interMessagesHandlers);
        return interMessagesHandlers;
    }

    @Provides
    @Singleton
    HandlersProvider<RealmClient, Message> provideRealmMessagesHandlers() {
        HandlersProvider<RealmClient, Message> realmMessagesHandlers = new HandlersProvider<>(parent,
                "koh.realm.network", Receive.class, Message.class);
        parent.createChildInjector(realmMessagesHandlers);
        return realmMessagesHandlers;
    }

    @Provides
    @Singleton
    Settings provideSettings() {
        return new Settings("Settings.ini");
    }
}
