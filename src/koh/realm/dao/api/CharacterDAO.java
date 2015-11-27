package koh.realm.dao.api;

import com.google.inject.ImplementedBy;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.realm.app.DatabaseSource;
import koh.realm.dao.DAO;
import koh.realm.dao.impl.CharacterDAOImpl;
import koh.realm.dao.impl.GameServerDAOImpl;

@ImplementedBy(CharacterDAOImpl.class)
@DependsOn(DatabaseSource.class)
public abstract class CharacterDAO implements DAO<Integer, Object>, Service {

    public abstract boolean insertOrUpdate(int Owner, short server, short number);

}
