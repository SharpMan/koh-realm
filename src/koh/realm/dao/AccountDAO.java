package koh.realm.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import koh.realm.Main;
import koh.realm.MySQL;
import koh.realm.entities.Account;
import koh.realm.network.RealmClient;
import koh.realm.network.RealmLoader;

/**
 *
 * @author Neo-Craft
 */
public class AccountDAO {

    public static final Map<Integer, AccountReference> Accounts_GUID = Collections.synchronizedMap(new HashMap<Integer, AccountReference>());
    public static final Map<String, AccountReference> Accounts_NAME = Collections.synchronizedMap(new HashMap<String, AccountReference>());
    public static final List<Account> LoggedAccounts = new CopyOnWriteArrayList<>();
    public static RealmLoader Loader = new RealmLoader();

    static {
        Timer thread = new Timer();
        thread.schedule(new TimerTask() {

            @Override
            public void run() {
                ArrayList<AccountReference> copy = new ArrayList<AccountReference>();
                copy.addAll(Accounts_NAME.values());
                for (AccountReference ref : copy) {
                    if (!ref.isLogged() && (System.currentTimeMillis() - ref.lastLogin) > 60 * 1000 * 60) {
                        Accounts_NAME.remove(ref.name);
                        Accounts_GUID.remove(ref.guid);
                    }
                }
                copy.clear();
                copy = null;
            }
        }, 60 * 1000 * 60, 60 * 1000 * 60);
    }

    public static Collection<Account> getAccounts() {
        return LoggedAccounts;
    }

    public static void removeAccount(Account c) {
        LoggedAccounts.remove(c);
    }

    public static AccountReference getCompte(int guid) {
        return Accounts_GUID.get(guid);
    }

    public static AccountReference getCompteByName(String name) {
        return Accounts_NAME.get(name);
    }

    public static void addAccount(Account compte) {
        if (!LoggedAccounts.contains(compte)) {
            LoggedAccounts.add(compte);
        }
    }

    public synchronized static AccountReference initReference(Account acc) {
        AccountReference ref = Accounts_NAME.get(acc.Username.toLowerCase());
        if (ref == null) {
            ref = new AccountReference(acc);
            Accounts_NAME.put(acc.Username.toLowerCase(), ref);
            Accounts_GUID.put(acc.ID, ref);
        }
        return ref;
    }

    public static void Save(Account acc) {
        try {
            PreparedStatement p;
            p = MySQL.prepareQuery("UPDATE `account` SET last_login = ? , last_ip = ? WHERE id = ?;", MySQL.Connection());
            p.setTimestamp(1, acc.last_login);
            p.setString(2, acc.LastIP);
            p.setInt(3, acc.ID);

            p.execute();
            MySQL.closePreparedStatement(p);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static Account Find(String Username, RealmClient Client) throws Exception {
        Account C = null;
        try {

            PreparedStatement p = MySQL.prepareQuery("SELECT account.id,account.username,account.sha_pass_hash,account.password,account.nickname,account.rights,account.secret_question,account.secret_answer,account.last_ip,account.suspended_time,account.last_login, GROUP_CONCAT(worlds_characters.server SEPARATOR ',') AS servers, GROUP_CONCAT(worlds_characters.number SEPARATOR ',') AS players from account LEFT JOIN worlds_characters on account.id = worlds_characters.owner WHERE LOWER(account.username) = LOWER(?);", MySQL.Connection());

            p.setString(1, Username);
            ResultSet RS = p.executeQuery();

            if (RS.first()) {
                if (RS.getString("username") == null) {
                    MySQL.closeResultSet(RS);
                    return null;
                }
                if ((AccountDAO.getCompte(RS.getInt("id")) != null) && (AccountDAO.getCompte(RS.getInt("id")).isLogged())) {
                    C = AccountDAO.getCompte(RS.getInt("id")).get();
                    C.Client.timeOut(); //was disco message
                    MySQL.closeResultSet(RS);
                    throw new NullPointerException();
                } else {
                    try {
                        C = new Account() {
                            {
                                ID = RS.getInt("id");
                                Username = RS.getString("username");
                                SHA_HASH = RS.getString("sha_pass_hash");
                                Password = RS.getString("password");
                                NickName = RS.getString("nickname");
                                Right = RS.getByte("rights");
                                SecretQuestion = RS.getString("secret_question");
                                SecretAnswer = RS.getString("secret_answer");
                                LastIP = RS.getString("last_ip");;
                                try {
                                    last_login = RS.getTimestamp("last_login");
                                } catch (Exception e) {
                                    last_login = Timestamp.from(Instant.now());
                                }
                                if (RS.getString("servers") != null) {
                                    for (int i = 0; i < RS.getString("servers").split(",").length; i++) {
                                        Characters.put(Short.parseShort(RS.getString("servers").split(",")[i]), Byte.parseByte(RS.getString("players").split(",")[i]));
                                    }
                                }
                            }
                        };
                    } catch (com.mysql.jdbc.exceptions.jdbc4.MySQLDataException e) {
                        e.printStackTrace();
                    }

                }
            }

            MySQL.closeResultSet(RS);

        } catch (SQLException e) {
            e.printStackTrace();
            Main.Logs().writeInfo("Connexion a la DB Perdue, deconnexion du compte en connexion en attendant la reconnexion de la DB...");
            throw new Exception();
        }
        return C;
    }

}
