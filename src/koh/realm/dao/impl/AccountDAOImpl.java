package koh.realm.dao.impl;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import koh.realm.DatabaseSource;
import koh.realm.Main;
import koh.realm.dao.AccountReference;
import koh.realm.dao.api.AccountDAO;
import koh.realm.entities.Account;
import koh.realm.network.RealmLoader;
import koh.realm.utils.sql.ConnectionStatement;

/**
 *
 * @author Neo-Craft
 */
public class AccountDAOImpl extends AccountDAO {

    private final Map<Integer, AccountReference> hashByGuid = new ConcurrentHashMap<>();
    private final Map<String, AccountReference> hashByName = new ConcurrentHashMap<>();
    private final List<Account> loggedAccounts = new CopyOnWriteArrayList<>();
    private final RealmLoader loader = new RealmLoader();

    public AccountDAOImpl() {
        ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate((Runnable) () -> {
            ArrayList<AccountReference> copy = new ArrayList<>();
            copy.addAll(hashByName.values());
            for (AccountReference ref : copy) {
                if (!ref.isLogged() && (System.currentTimeMillis() - ref.lastLogin) > 60 * 1000 * 60) {
                    hashByName.remove(ref.name);
                    hashByGuid.remove(ref.guid);
                }
            }
            copy.clear();
        }, 60 * 1000 * 60, 60 * 1000 * 60, TimeUnit.MILLISECONDS);
    }

    @Override
    public RealmLoader getLoader() {
        return loader;
    }

    @Override
    public Collection<Account> getAccounts() {
        return loggedAccounts;
    }

    @Override
    public void removeAccount(Account c) {
        loggedAccounts.remove(c);
    }

    @Override
    public AccountReference getCompte(int guid) {
        return hashByGuid.get(guid);
    }

    @Override
    public AccountReference getCompteByName(String name) {
        return hashByName.get(name);
    }

    @Override
    public void addAccount(Account compte) {
        if (!loggedAccounts.contains(compte))
            loggedAccounts.add(compte);
    }

    @Override
    public synchronized AccountReference initReference(Account acc) {
        AccountReference ref = hashByName.get(acc.Username.toLowerCase());
        if (ref == null) {
            ref = new AccountReference(acc);
            hashByName.put(acc.Username.toLowerCase(), ref);
            hashByGuid.put(acc.ID, ref);
        }
        return ref;
    }

    private final static String UPDATE_BY_ID = "UPDATE `account` SET last_login = ? , last_ip = ? WHERE id = ?;";

    @Override
    public void save(Account acc) {
        try (ConnectionStatement<PreparedStatement> conn = DatabaseSource.get().prepareStatement(UPDATE_BY_ID)){
            PreparedStatement stmt = conn.getStatement();
            stmt.setTimestamp(1, acc.last_login);
            stmt.setString(2, acc.LastIP);
            stmt.setInt(3, acc.ID);

            stmt.execute();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final static String QUERY_BY_USERNAME = "SELECT account.id,account.username,account.sha_pass_hash,account.password,account.nickname,account.rights,account.secret_question,account.secret_answer,account.last_ip,account.suspended_time,account.last_login, " +
            "GROUP_CONCAT(worlds_characters.server SEPARATOR ',') AS servers, " +
            "GROUP_CONCAT(worlds_characters.number SEPARATOR ',') AS players " +
            "FROM ACCOUNT " +
            "LEFT JOIN worlds_characters on account.id = worlds_characters.owner " +
            "WHERE account.username = ?;";

    @Override
    public Account getByKey(String Username) throws Exception {
        try (ConnectionStatement<PreparedStatement> conn = DatabaseSource.get().prepareStatement(QUERY_BY_USERNAME)) {

            PreparedStatement stmt = conn.getStatement();
            stmt.setString(1, Username);
            ResultSet RS = stmt.executeQuery();

            if(!RS.first() || RS.getString("username") == null)
                return null;

            AccountReference other = this.getCompte(RS.getInt("id"));
            if ((other != null) && (other.isLogged())) {
                other.get().Client.timeOut(); //was disco message
                throw new NullPointerException();
            } else {
                return new Account() {
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Main.Logs().writeInfo("Connexion a la DB Perdue, deconnexion du compte en connexion en attendant la reconnexion de la DB...");
            throw new Exception();
        }
    }

}
