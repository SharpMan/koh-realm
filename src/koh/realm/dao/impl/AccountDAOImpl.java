package koh.realm.dao.impl;

import com.google.inject.Inject;
import koh.patterns.services.api.ServiceDependency;
import koh.realm.dao.DatabaseSource;
import koh.realm.dao.api.AccountDAO;
import koh.realm.entities.Account;
import koh.realm.utils.sql.ConnectionStatement;
import koh.repositories.BiRecyclingRepository;
import koh.repositories.RepositoryReference;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author Neo-Craft
 */
public class AccountDAOImpl extends AccountDAO {

    private static final Logger logger = LogManager.getLogger(AccountDAO.class);

    private static final int RECYCLE_MINS = 60;

    private final BiRecyclingRepository<Integer, String, Account> accounts;

    @Inject
    private @ServiceDependency("RealmServices") DatabaseSource dbSource;

    public AccountDAOImpl() {
        this.accounts = new BiRecyclingRepository<>((acc) -> acc.id, (acc) -> acc.username,
                this::loadById, this::loadByUsername,
                this::save, (val) -> val, String::toLowerCase,
                RECYCLE_MINS, TimeUnit.MINUTES);
    }

    @Override
    public RepositoryReference<Account> getAccount(int guid) {
        return accounts.getReferenceByFirst(guid);
    }

    @Override
    public RepositoryReference<Account> getAccount(String name) {
        return accounts.getReferenceBySecond(name);
    }



    private static final String UPDATE_SUSPENSION_BY_ID = "UPDATE `account` SET suspended_time = ? WHERE id = ?;";

    @Override
    public void updateBlame(Account acc) {
        try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(UPDATE_SUSPENSION_BY_ID)){
            PreparedStatement stmt = conn.getStatement();
            stmt.setLong(1, acc.suspendedTime);
            stmt.setInt(2, acc.id);

            stmt.execute();

            logger.debug("Account [{}] {} suspension time saved", acc.id, acc.username);
        } catch (Exception e) {
            logger.error(e);
            logger.warn(e.getMessage());
        }
    }

    private final static String UPDATE_BY_ID = "UPDATE `account` SET last_login = ? , last_ip = ? WHERE id = ?;";

    @Override
    public void save(Account acc) {
        try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(UPDATE_BY_ID)){
            PreparedStatement stmt = conn.getStatement();
            stmt.setTimestamp(1, acc.last_login);
            stmt.setString(2, acc.lastIP);
            stmt.setInt(3, acc.id);

            stmt.execute();

            logger.debug("Account [{}] {} saved", acc.id, acc.username);
        } catch (Exception e) {
            logger.error(e);
            logger.warn(e.getMessage());
        }
    }

    @Override
    public RepositoryReference<Account> getLoadedAccount(int guid) {
        return accounts.getReferenceByFirst(guid);
    }

    private final static String QUERY_BY_USERNAME = "SELECT account.id,account.reg_server,account.username,account.sha_pass_hash,account.password,account.nickname,account.rights,account.secret_question,account.secret_answer,account.last_ip,account.suspended_time,account.last_login, " +
            "GROUP_CONCAT(worlds_characters.server SEPARATOR ',') AS servers, " +
            "GROUP_CONCAT(worlds_characters.number SEPARATOR ',') AS players " +
            "FROM account " +
            "LEFT JOIN worlds_characters on account.id = worlds_characters.owner " +
            "WHERE account.username = ?;";

    public Account loadByUsername(String username) {
        try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(QUERY_BY_USERNAME)) {

            PreparedStatement stmt = conn.getStatement();
            stmt.setString(1, username);

            ResultSet RS = stmt.executeQuery();

            if(!RS.first())
                return null;

            return new Account() {
                {
                    id = RS.getInt("id");
                    username = RS.getString("username");
                    SHA_HASH = RS.getString("sha_pass_hash");
                    password = RS.getString("password");
                    nickName = RS.getString("nickname");
                    suspendedTime = RS.getLong("suspended_time");
                    right = RS.getByte("rights");
                    reg_server = RS.getByte("reg_server");
                    secretQuestion = RS.getString("secret_question");
                    secretAnswer = RS.getString("secret_answer");
                    lastIP = RS.getString("last_ip");;
                    try {
                        last_login = RS.getTimestamp("last_login");
                    } catch (Exception e) {
                        last_login = Timestamp.from(Instant.now());
                    }
                    if (RS.getString("servers") != null) {
                        String[] servers = RS.getString("servers").split(",");
                        String[] players = RS.getString("players").split(",");

                        for (int i = 0; i < RS.getString("servers").split(",").length; i++)
                            characters.put(Short.parseShort(servers[i]), Byte.parseByte(players[i]));
                    }
                }
            };
        }catch (Exception e) {
            logger.error(e);
            logger.warn(e.getMessage());
        }
        return null;
    }

    @Override
    public Account getByKey(String Username) throws Exception {
        return accounts.getBySecond(Username);
    }

    private final static String QUERY_BY_ID = "SELECT account.id,account.reg_server,account.username,account.sha_pass_hash,account.password,account.nickname,account.rights,account.secret_question,account.secret_answer,account.last_ip,account.suspended_time,account.last_login, " +
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

            if (!RS.first())
                return null;

            return new Account() {
                {
                    id = RS.getInt("id");
                    username = RS.getString("username");
                    SHA_HASH = RS.getString("sha_pass_hash");
                    password = RS.getString("password");
                    nickName = RS.getString("nickname");
                    right = RS.getByte("rights");
                    reg_server = RS.getByte("reg_server");
                    secretQuestion = RS.getString("secret_question");
                    secretAnswer = RS.getString("secret_answer");
                    lastIP = RS.getString("last_ip");
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
                            characters.put(Short.parseShort(servers[i]), Byte.parseByte(players[i]));
                    }
                }
            };
        }catch (Exception e) {
            logger.error(e);
            logger.warn(e.getMessage());
        }
        return null;
    }

    @Override
    public void start() {
    }

    @Override
    public void stop() {
        accounts.dispose();
        accounts.values().stream().forEach((account) -> account.sync(() -> {
            if(account.loaded())
                this.save(account.get());
        }));
    }
}
