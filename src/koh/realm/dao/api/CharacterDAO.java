package koh.realm.dao.api;

import com.google.inject.ImplementedBy;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.realm.dao.DatabaseSource;
import koh.realm.dao.DAO;
import koh.realm.dao.impl.CharacterDAOImpl;

@DependsOn(DatabaseSource.class)
public abstract class CharacterDAO implements DAO<Integer, Object>, Service {

    public abstract boolean insertOrUpdate(int Owner, short server, short number);

}
