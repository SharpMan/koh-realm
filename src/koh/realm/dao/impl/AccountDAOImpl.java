package koh.realm.dao.impl;

import com.google.inject.Inject;
import koh.patterns.services.api.ServiceDependency;
import koh.realm.app.DatabaseSource;
import koh.realm.dao.api.AccountDAO;
import koh.realm.entities.Account;
import koh.realm.utils.sql.ConnectionStatement;
import koh.repositories.BiRecyclingRepository;
import koh.repositories.RepositoryReference;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Neo-Craft
 */
public class AccountDAOImpl extends AccountDAO {

    private final DatabaseSource dbSource;

    private static final int RECYCLE_MINS = 60;

    private final BiRecyclingRepository<Integer, String, Account> accounts;

    @Inject
    public AccountDAOImpl(@ServiceDependency("RealmServices") DatabaseSource dbSource) {
        this.dbSource = dbSource;

        this.accounts = new BiRecyclingRepository<>((acc) -> acc.ID, (acc) -> acc.Username,
                this::loadById, this::loadByUsername,
                this::save, (val) -> val, String::toLowerCase,
                RECYCLE_MINS, TimeUnit.MINUTES);
    }

    @Override
    public RepositoryReference<Account> getCompte(int guid) {
        return accounts.getReferenceByFirst(guid);
    }

    @Override
    public RepositoryReference<Account> getCompteByName(String name) {
        return accounts.getReferenceBySecond(name);
    }

    private final static String UPDATE_BY_ID = "UPDATE `account` SET last_login = ? , last_ip = ? WHERE id = ?;";

    @Override
    public void save(Account acc) {
        try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(UPDATE_BY_ID)){
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
            "FROM account " +
            "LEFT JOIN worlds_characters on account.id = worlds_characters.owner " +
            "WHERE account.username = ?;";

    public Account loadByUsername(String Username) {
        try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(QUERY_BY_USERNAME)) {

            PreparedStatement stmt = conn.getStatement();
            stmt.setString(1, Username);

            ResultSet RS = stmt.executeQuery();


            if(!RS.first() || RS.getString("username") == null) {
                throw new NullPointerException();
            }

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
                        String[] servers = RS.getString("servers").split(",");
                        String[] players = RS.getString("players").split(",");

                        for (int i = 0; i < RS.getString("servers").split(",").length; i++)
                            Characters.put(Short.parseShort(servers[i]), Byte.parseByte(players[i]));
                    }
                }
            };
        }catch(NullPointerException e) {
            throw new NullPointerException();
        } catch (Exception e) {
            System.out.println(e.getMessage());
            //TODO propagate Errors
            e.printStackTrace();
            //Main.Logs().writeInfo("Connexion a la DB Perdue, deconnexion du compte en connexion en attendant la reconnexion de la DB...");
            throw new NullPointerException();
        }
    }

    @Override
    public Account getByKey(String Username) throws Exception {
        return accounts.getBySecond(Username);
    }

    private final static String QUERY_BY_ID = "SELECT account.id,account.username,account.sha_pass_hash,account.password,account.nickname,account.rights,account.secret_question,account.secret_answer,account.last_ip,account.suspended_time,account.last_login, " +
            "GROUP_CONCAT(worlds_characters.server SEPARATOR ',') AS servers, " +
            "GROUP_CONCAT(worlds_characters.number SEPARATOR ',') AS players " +
            "FROM account " +
            "LEFT JOIN worlds_characters on account.id = worlds_characters.owner " +
            "WHERE account.id = ?;";

    private Account loadById(int id) {
        try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(QUERY_BY_ID)) {

            PreparedStatement stmt = conn.getStatement();
            stmt.setInt(1, id);
            ResultSet RS = stmt.executeQuery();

            if (!RS.first() || RS.getString("username") == null)
                throw new NullPointerException();

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
                    LastIP = RS.getString("last_ip");
                    ;
                    try {
                        last_login = RS.getTimestamp("last_login");
                    } catch (Exception e) {
                        last_login = Timestamp.from(Instant.now());
                    }
                    if (RS.getString("servers") != null) {
                        String[] servers = RS.getString("servers").split(",");
                        String[] players = RS.getString("players").split(",");

                        for (int i = 0; i < RS.getString("servers").split(",").length; i++)
                            Characters.put(Short.parseShort(servers[i]), Byte.parseByte(players[i]));
                    }
                }
            };
        }catch(NullPointerException e) {
            throw new NullPointerException();
        } catch (Exception e) {
            //TODO propagate Errors
            e.printStackTrace();
            //Main.Logs().writeInfo("Connexion a la DB Perdue, deconnexion du compte en connexion en attendant la reconnexion de la DB...");
            //throw new Exception();
            throw new NullPointerException();
        }
    }

}
