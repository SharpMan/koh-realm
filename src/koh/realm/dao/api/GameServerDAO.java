package koh.realm.dao.api;

import com.google.inject.ImplementedBy;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.realm.dao.DatabaseSource;
import koh.realm.dao.DAO;
import koh.realm.dao.impl.GameServerDAOImpl;
import koh.realm.entities.GameServer;

import java.util.stream.Stream;

@DependsOn(DatabaseSource.class)
public abstract class GameServerDAO implements DAO<Short, GameServer>, Service {

    public abstract GameServer getByHash(String hash);

    public abstract int loadAll();

    public abstract Stream<GameServer> getGameServers();

    public abstract void addGameServer(GameServer server);
}
