package koh.realm.dao;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import koh.realm.dao.api.AccountDAO;
import koh.realm.dao.api.CharacterDAO;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.dao.impl.AccountDAOImpl;
import koh.realm.dao.impl.CharacterDAOImpl;
import koh.realm.dao.impl.GameServerDAOImpl;

public class DAOModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AccountDAO.class).to(AccountDAOImpl.class).in(Scopes.SINGLETON);
        bind(CharacterDAO.class).to(CharacterDAOImpl.class).in(Scopes.SINGLETON);
        bind(GameServerDAO.class).to(GameServerDAOImpl.class).in(Scopes.SINGLETON);
    }

}
