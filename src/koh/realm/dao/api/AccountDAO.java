package koh.realm.dao.api;

import com.google.inject.ImplementedBy;
import koh.patterns.services.api.DependsOn;
import koh.patterns.services.api.Service;
import koh.realm.app.DatabaseSource;
import koh.realm.dao.DAO;
import koh.realm.dao.impl.AccountDAOImpl;
import koh.realm.entities.Account;
import koh.repositories.RepositoryReference;

@ImplementedBy(AccountDAOImpl.class)
@DependsOn(DatabaseSource.class)
public abstract class AccountDAO implements DAO<String, Account>, Service {

    public abstract RepositoryReference<Account> getAccount(int guid);

    public abstract RepositoryReference<Account> getAccount(String name);

    public abstract void save(Account acc);

    public abstract RepositoryReference<Account> getLoadedAccount(int guid);

}
