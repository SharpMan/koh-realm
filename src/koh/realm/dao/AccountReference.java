package koh.realm.dao;

import koh.realm.dao.api.AccountDAO;
import koh.realm.entities.Account;

/**
 *
 * @author Alleos13
 */
public class AccountReference {

    private Account account;
    public final int guid;
    public final String name;
    private boolean logged = false;

    public AccountReference(Account account) {
        this.guid = account.ID;
        this.name = account.Username;
    }

    public Account get() {
        return account;
    }

    public boolean isLogged() {
        return account != null;
    }
    public long lastLogin = 0;

    public synchronized void setLogged(Account logged) {
        if (logged != null) {
            if (account != null) {
                throw new NullPointerException("Already logged with another account inst");
            }
            lastLogin = System.currentTimeMillis();
        } else if (account != null) {
            //AccountDAO.get().removeAccount(account);
        }
        if (logged == null) {
            account.totalClear();
        }
        account = logged;
    }
}
