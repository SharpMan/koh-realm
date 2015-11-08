package koh.realm.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import koh.realm.DatabaseSource;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.entities.GameServer;
import koh.realm.utils.Settings;
import koh.realm.utils.sql.ConnectionResult;
import koh.realm.utils.sql.ConnectionStatement;

/**
 *
 * @author Neo-Craft
 */
public class GameServerDAOImpl extends GameServerDAO {

    private final Map<Short, GameServer> gameServers = new ConcurrentHashMap<>();

    private static final String FIND_ALL = "SELECT * from realmlist;";

    @Override
    public int loadAll() {
        try (ConnectionResult conn = DatabaseSource.get().executeQuery(FIND_ALL, 0)) {
            ResultSet cursor = conn.getResult();
            while (cursor.next())
                addGameServer(new GameServer() {
                    {
                        ID = cursor.getShort("id");
                        Adress = cursor.getString("address");
                        Name = cursor.getString("name");
                        Port = cursor.getShort("port");
                        RequiredRole = cursor.getByte("requiredRole");
                        Hash = cursor.getString("hash");
                    }
                });

            return cursor.getRow();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    @Override
    public GameServer getByHash(String hash) {
        for (GameServer a : gameServers.values())
            if (a.Hash.equalsIgnoreCase(hash))
                return a;
        return null;
    }

    @Override
    public Collection<GameServer> getGameServers() {
        return gameServers.values();
    }

    @Override
    public void addGameServer(GameServer server) {
        gameServers.remove(server.ID);
        gameServers.put(server.ID, server);
    }

    @Override
    public GameServer getByKey(Short guid) throws Exception {
        return gameServers.get(guid);
    }
}
