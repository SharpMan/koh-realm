package koh.realm.dao.api;

import koh.realm.dao.DAO;
import koh.realm.entities.Account;
import koh.repositories.RepositoryReference;

public abstract class AccountDAO implements DAO<String, Account> {

    public abstract RepositoryReference<Account> getAccount(int guid);

    public abstract RepositoryReference<Account> getAccount(String name);

    public abstract void save(Account acc);

    public abstract RepositoryReference<Account> getLoadedAccount(int guid);

}
