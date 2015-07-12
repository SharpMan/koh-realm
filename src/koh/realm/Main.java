package koh.realm;

import java.nio.file.Files;
import java.nio.file.Paths;
import koh.realm.inter.InterServer;
import koh.realm.network.RealmHandler;
import koh.realm.network.RealmServer;
import koh.realm.utils.Settings;

/**
 *
 * @author Neo-Craft
 */
public class Main {

    private volatile static RealmServer $RealmServer;
    private volatile static InterServer $InterServer;
    private volatile static Logs $Logs;
    public static int MIN_TIMEOUT = 30;
    private static boolean running;
    public static final String binaryKey = "key.dat";
    public static final String salt = "hk2zaar9desn'@CD\"G84vF&zEK\")DT!U";
    public static final String bypassPacket = "StumpPatch.swf";

    public static RealmServer RealmServer() {
        return $RealmServer;
    }

    public static InterServer InterServer() {
        return $InterServer;
    }

    public static Logs Logs() {
        return $Logs;
    }

    public static void main(String[] args) {
        try {
            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    close();
                }

            });

            long time = System.currentTimeMillis();
            Settings.Initialize();
            $Logs = new Logs();
            MySQL.ConnectDatabase();
            MySQL.LoadCache();
            RealmHandler.RawBytes = Files.readAllBytes(Paths.get(Main.bypassPacket));
            RealmHandler.binaryKeys = new String(Files.readAllBytes(Paths.get(Main.binaryKey)), "utf-8").toCharArray();
            $InterServer = new InterServer(Settings.GetIntElement("Inter.Port")).configure().launch();
            $RealmServer = new RealmServer(Settings.GetIntElement("Login.Port")).configure().launch();
            running = true;
            $Logs.writeInfo(new StringBuilder("RealmServer start in ").append(System.currentTimeMillis() - time).append(" ms.").toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean isRunning() {
        return running;
    }

    private static void close() {
        try {
            $InterServer.stop();
            $RealmServer.stop();
            MySQL.disconnectDatabase();
            running = false;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("[INFOS] Server shutdown success.");
        }

    }
}
