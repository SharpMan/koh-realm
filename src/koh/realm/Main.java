package koh.realm;

import koh.patterns.services.ServicesProvider;
import koh.realm.app.AppModule;
import koh.realm.app.DatabaseSource;
import koh.realm.app.MemoryService;
import koh.realm.dao.api.AccountDAO;
import koh.realm.dao.api.CharacterDAO;
import koh.realm.dao.api.GameServerDAO;
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

    //TODO create DAO as service, for stopping them in good order

    public static void main(String[] args) {
        try {

            logger.info("Starting realm server ...");
            long time = System.currentTimeMillis();

            AppModule app = new AppModule();
            ServicesProvider services = app.create(
                    MemoryService.class,
                    DatabaseSource.class,

                    AccountDAO.class,
                    GameServerDAO.class,
                    CharacterDAO.class,

                    //RealmServer.class,
                    InterServer.class
            );

            services.start();

            logger.info("RealmServer start in " + (System.currentTimeMillis() - time) + " ms.");

        } catch (Exception e) {
            logger.fatal(e);
            logger.error(e.getMessage());
        }
    }

}
