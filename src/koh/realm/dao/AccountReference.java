package koh.realm.dao;

import koh.concurrency.MutexReference;
import koh.realm.dao.api.AccountDAO;
import koh.realm.entities.Account;

/**
 *
 * @author Alleos13
 */
public class AccountReference extends MutexReference<Account> {

    private String name;
    private int guid;

    public AccountReference(Account account) {
        this.set(account);
    }

    public String getName() {
        return name;
    }

    public int getGuid() {
        return guid;
    }

    @Override
    protected void onSet(Account account) {
        this.guid = account.ID;
        this.name = account.Username;
    }

    @Override
    protected void onUnset() {
        //TODO REMOVE Entity -> DAO dependency
        if(this.alive()) {
        }
    }

}
