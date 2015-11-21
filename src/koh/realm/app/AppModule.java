package koh.realm.app;

import com.google.inject.*;
import koh.patterns.services.ServicesProvider;
import koh.patterns.services.api.Service;
import koh.realm.utils.Settings;

public class AppModule extends AbstractModule {

    private Injector app;

    public AppModule() {
        this.app = Guice.createInjector(this);
    }

    @SafeVarargs
    public final ServicesProvider create(Class<? extends Service>... services) {
        ServicesProvider provider = new ServicesProvider("RealmServices", app, services);
        app = app.createChildInjector(provider);
        return provider;
    }

    public Injector resolver() {
        return app;
    }

    @Override
    protected void configure() {
        install(new CoreModule());
    }

    @Provides
    @Singleton
    Settings provideSettings() {
        return new Settings("../koh-realm/Settings.ini");
    }

}
