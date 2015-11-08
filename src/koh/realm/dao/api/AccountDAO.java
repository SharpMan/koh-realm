package koh.realm.dao.api;

import koh.realm.dao.DAO;
import koh.realm.dao.impl.AccountDAOImpl;
import koh.realm.dao.AccountReference;
import koh.realm.entities.Account;
import koh.realm.network.RealmLoader;

import java.util.Collection;

public abstract class AccountDAO implements DAO<String, Account> {

    private static AccountDAO instance;

    public static AccountDAO get() {
        if(instance == null)
            instance = new AccountDAOImpl();
        return instance;
    }

    public abstract RealmLoader getLoader();

    public abstract Collection<Account> getAccounts();

    public abstract void removeAccount(Account c);

    public abstract AccountReference getCompte(int guid);

    public abstract AccountReference getCompteByName(String name);

    public abstract void addAccount(Account compte);

    public abstract AccountReference initReference(Account acc);

    public abstract void save(Account acc);

}
