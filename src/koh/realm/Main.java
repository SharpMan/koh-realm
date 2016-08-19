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

import java.io.PrintStream;

/**
 *
 * @author Neo-Craft
 */
public class Main {

    private static final Logger logger = LogManager.getLogger(Main.class);

    public static final RealmServer REALM = new RealmServer();

    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
        return new PrintStream(realPrintStream) {
            public void print(final String string) {
                realPrintStream.print(string);
                logger.error(string);
            }
        };
    }

    public static void main(String[] args) {
        try {
            System.setErr(createLoggingProxy(System.err));
            AppModule app = new AppModule();
            ServicesProvider services = app.create(
                    new DatabaseSource(),
                    new MemoryService(),

                    REALM,
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
