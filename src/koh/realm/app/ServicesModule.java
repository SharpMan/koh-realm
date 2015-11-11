package koh.realm.app;

import com.google.inject.*;
import koh.realm.dao.DAOModule;
import koh.realm.utils.Settings;

class ServicesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(Logs.class).in(Scopes.SINGLETON);
        bind(DatabaseSource.class).in(Scopes.SINGLETON);

        install(new DAOModule());
    }

    @Provides
    @Singleton
    Settings provideSettings() {
        return new Settings("../koh-realm/Settings.ini");
    }
}
