package koh.realm.dao.api;

import koh.realm.dao.DAO;
import koh.realm.dao.impl.CharacterDAOImpl;

public abstract class CharacterDAO implements DAO<Integer, Object> {

    public abstract boolean insertOrUpdate(int Owner, short server, short number);

}
