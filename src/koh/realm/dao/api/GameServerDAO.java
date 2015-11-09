package koh.realm.dao.api;

import koh.realm.dao.DAO;
import koh.realm.dao.impl.GameServerDAOImpl;
import koh.realm.entities.GameServer;

import java.util.Collection;

public abstract class GameServerDAO implements DAO<Short, GameServer> {

    public abstract GameServer getByHash(String hash);

    public abstract int loadAll();

    //TODO(Alleos) : return Stream<GameServer>
    public abstract Collection<GameServer> getGameServers();

    public abstract void addGameServer(GameServer server);
}