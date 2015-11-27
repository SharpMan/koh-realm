package koh.realm.dao.impl;

import com.google.inject.Inject;
import koh.patterns.services.api.ServiceDependency;
import koh.realm.app.DatabaseSource;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.entities.GameServer;
import koh.realm.utils.sql.ConnectionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 *
 * @author Neo-Craft
 */
public class GameServerDAOImpl extends GameServerDAO {

    private static final Logger logger = LogManager.getLogger(GameServerDAO.class);

    private final Map<Short, GameServer> gameServers = new ConcurrentHashMap<>();

    private final DatabaseSource dbSource;

    @Inject
    public GameServerDAOImpl(@ServiceDependency("RealmServices") DatabaseSource dbSource) {
        System.out.println("New GameServerDAO !");
        this.dbSource = dbSource;
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
                        Address = cursor.getString("address");
                        Name = cursor.getString("name");
                        Port = cursor.getShort("port");
                        RequiredRole = cursor.getByte("requiredRole");
                        Hash = cursor.getString("hash");
                    }
                });

            return gameServers.size();
        } catch (Exception e) {
            logger.error(e);
            logger.warn(e.getMessage());
        }
        return 0;
    }

    @Override
    public GameServer getByHash(String hash) {
        for (GameServer a : gameServers.values())
            if (a.Hash.equals(hash))
                return a;
        return null;
    }

    @Override
    public Stream<GameServer> getGameServers() {
        return gameServers.values().stream();
    }

    @Override
    public void addGameServer(GameServer server) {
        gameServers.put(server.ID, server);
    }

    @Override
    public GameServer getByKey(Short guid) throws Exception {
        return gameServers.get(guid);
    }

    @Override
    public void start() {
        logger.info(this.loadAll() + " WorldServers loaded ");
    }

    @Override
    public void stop() {
    }
}
