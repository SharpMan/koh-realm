package koh.realm.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import koh.realm.MySQL;
import koh.realm.entities.GameServer;
import koh.realm.utils.Settings;

/**
 *
 * @author Neo-Craft
 */
public class GameServerDAO {

    private static final Map<Short, GameServer> GameServers = Collections.synchronizedMap(new HashMap<>());

    public static int Find_All() {
        int n = 0;
        try {
            ResultSet RS = MySQL.executeQuery("SELECT * from realmlist;", Settings.GetStringElement("Database.Name"), 0);
            while (RS.next()) {
                addGameServer(
                        new GameServer() {
                            {
                                ID = RS.getShort("id");
                                Adress = RS.getString("address");
                                Name = RS.getString("name");
                                Port = RS.getShort("port");
                                RequiredRole = RS.getByte("requiredRole");
                                Hash = RS.getString("hash");
                            }
                        });
                n++;
            }
            MySQL.closeResultSet(RS);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            return n;
        }
    }

    public static GameServer getGameServer(short guid) {
        if (GameServers.containsKey(guid)) {
            return GameServers.get(guid);
        } else {
            return null;
        }
    }

    public static GameServer getGameServerByHash(String hash) {
        for (GameServer a : GameServers.values()) {
            if (a.Hash.equalsIgnoreCase(hash)) {
                return a;
            }
        }
        return null;
    }

    public static Collection<GameServer> getGameServers() {
        return GameServers.values();
    }

    public static void addGameServer(GameServer compte) {
        GameServers.remove(compte.ID);
        GameServers.put(compte.ID, compte);
    }

}
