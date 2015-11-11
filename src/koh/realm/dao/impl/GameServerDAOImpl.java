package koh.realm.dao.impl;

import com.google.inject.Inject;
import koh.realm.app.DatabaseSource;
import koh.realm.app.Logs;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.entities.GameServer;
import koh.realm.utils.sql.ConnectionResult;

import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author Neo-Craft
 */
public class GameServerDAOImpl extends GameServerDAO {

    private final Map<Short, GameServer> gameServers = new ConcurrentHashMap<>();

    private final DatabaseSource dbSource;

    @Inject
    public GameServerDAOImpl(DatabaseSource dbSource, Logs logs) {
        this.dbSource = dbSource;

        //logs.writeInfo(this.loadAll() + " WorldServers loaded ");
    }

    private static final String FIND_ALL = "SELECT * from realmlist;";

    @Override
    public int loadAll() {
        try (ConnectionResult conn = dbSource.executeQuery(FIND_ALL, 0)) {
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

            return gameServers.size();
        } catch (Exception e) {
            System.err.println(e.getMessage());
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
