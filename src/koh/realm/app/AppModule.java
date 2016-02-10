package koh.realm.app;

import com.google.inject.*;
import koh.patterns.services.ServicesProvider;
import koh.patterns.services.api.Service;
import koh.realm.utils.Settings;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.reflections.Reflections;

import java.util.Map;

public class AppModule extends AbstractModule {

    static {
        Reflections.log = null;
    }

    private Injector app;

    public AppModule() {
        this.app = Guice.createInjector(this);
    }

    public final ServicesProvider create(Service... services) {
        ServicesProvider provider = new ServicesProvider("RealmServices", services);
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

}
