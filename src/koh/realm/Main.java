package koh.realm;

import koh.patterns.services.ServicesProvider;
import koh.realm.app.AppModule;
import koh.realm.app.Loggers;
import koh.realm.dao.DatabaseSource;
import koh.realm.app.MemoryService;
import koh.realm.internet.RealmServer;
import koh.realm.intranet.InterServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Neo-Craft
 */
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static void main(String[] args) {
        try {

            AppModule app = new AppModule();
            ServicesProvider services = app.create(
                    new DatabaseSource(),
                    new MemoryService(),

                    new RealmServer(),
                    new InterServer(),

                    new Loggers()
            );

            services.start(app.resolver());

        } catch (Exception e) {
            logger.fatal(e);
            logger.error(e.getMessage());
        }
    }

}
