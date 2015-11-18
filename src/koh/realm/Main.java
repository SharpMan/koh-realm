package koh.realm;

import com.google.inject.Key;
import koh.patterns.services.ServicesProvider;
import koh.realm.app.AppModule;
import koh.realm.app.DatabaseSource;
import koh.realm.app.Logs;
import koh.realm.inter.InterServer;
import koh.realm.refact_network.RealmServer;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Neo-Craft
 */
public class Main {

    public static int MIN_TIMEOUT = 30;
    private static boolean running;


    public static final String binaryKey = "key.dat";
    public static final String salt = "hk2zaar9desn'@CD\"G84vF&zEK\")DT!U";
    public static final String bypassPacket = "StumpPatch.swf";


    private static final List<Runnable> runnableList = new ArrayList<>();

    public static void onShutdown(Runnable runnable) {
        runnableList.add(runnable);
    }

    public static void main(String[] args) {
        try {
            registerShutdownHooks();

            long time = System.currentTimeMillis();

            AppModule app = new AppModule();
            ServicesProvider services = app.create(
                    DatabaseSource.class,
                    Logs.class,
                    RealmServer.class,
                    InterServer.class
            );

            services.start();

            running = true;

            app.resolver().getInstance(Key.get(Logs.class, ServicesProvider.inGroup("RealmServices")))
                    .writeInfo("RealmServer start in " + (System.currentTimeMillis() - time) + " ms.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isRunning() {
        return running;
    }

    private static void registerShutdownHooks() {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                for(int i=runnableList.size()-1; i > 0; --i) {
                    try {
                        runnableList.get(i).run();
                    }catch(Throwable tr) {
                        tr.printStackTrace();
                    }
                }
            }

        });
    }

}
