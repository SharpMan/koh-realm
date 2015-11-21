package koh.realm;

import com.google.inject.Key;
import koh.patterns.services.ServicesProvider;
import koh.realm.app.AppModule;
import koh.realm.app.DatabaseSource;
import koh.realm.app.Logs;
import koh.realm.app.MemoryService;
import koh.realm.intranet.InterServer;
import koh.realm.internet.RealmServer;
import org.reflections.Reflections;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Neo-Craft
 */
public class Main {

    public static void main(String[] args) {
        try {
            long time = System.currentTimeMillis();

            AppModule app = new AppModule();
            ServicesProvider services = app.create(
                    MemoryService.class,
                    DatabaseSource.class,
                    Logs.class,
                    RealmServer.class,
                    InterServer.class
            );

            services.start();

            app.resolver().getInstance(Key.get(Logs.class, ServicesProvider.inGroup("RealmServices")))
                    .writeInfo("RealmServer start in " + (System.currentTimeMillis() - time) + " ms.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

}
