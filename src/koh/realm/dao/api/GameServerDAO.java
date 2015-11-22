package koh.realm.dao.api;

import koh.realm.dao.DAO;
import koh.realm.dao.impl.GameServerDAOImpl;
import koh.realm.entities.GameServer;

import java.util.Collection;
import java.util.stream.Stream;

public abstract class GameServerDAO implements DAO<Short, GameServer> {

    public abstract GameServer getByHash(String hash);

    public abstract int loadAll();

    public abstract Stream<GameServer> getGameServers();

    public abstract void addGameServer(GameServer server);
}
