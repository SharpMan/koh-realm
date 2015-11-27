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
    Settings provideConfiguration() {
        Settings settings = new Settings("../koh-realm/Settings.ini");

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();

        if(settings.getBoolElement("Logging.Debug")) {
            config.getRootLogger().removeAppender("Console");
            config.getRootLogger().addAppender(config.getAppender("Console"),
                    settings.getBoolElement("Logging.Debug") ? (Level.DEBUG) : (Level.INFO), null);

            config.getLoggerConfig("RealmServer").addAppender(config.getAppender("Console"),
                    Level.DEBUG, null);
        }

        ctx.updateLoggers(config);

        return settings;
    }

}
