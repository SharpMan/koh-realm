package koh.realm;

import com.google.inject.Guice;
import com.google.inject.Injector;
import koh.realm.inter.InterServer;
import koh.realm.network.RealmServer;

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
            registerShutdownHook(() -> {
                for(int i=runnableList.size()-1; i > 0; --i) {
                    try {
                        runnableList.get(i).run();
                    }catch(Throwable tr) {
                        tr.printStackTrace();
                    }
                }
            });

            long time = System.currentTimeMillis();

            Injector guice = Guice.createInjector();
            Injector appModule = guice.createChildInjector(new RealmModule(guice));

            appModule.getInstance(InterServer.class)
                    .configure().launch();

            appModule.getInstance(RealmServer.class)
                    .configure().launch();

            running = true;

            appModule.getInstance(Logs.class)
                    .writeInfo("RealmServer start in " + (System.currentTimeMillis() - time) + " ms.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isRunning() {
        return running;
    }

    private static void registerShutdownHook(Runnable runnable) {
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                if(runnable != null)
                    runnable.run();
            }

        });
    }

}
