package koh.realm.dao.impl;

import com.google.inject.Inject;
import koh.patterns.services.api.ServiceDependency;
import koh.realm.dao.DatabaseSource;
import koh.realm.dao.api.BannedAddressDAO;
import koh.realm.dao.api.GameServerDAO;
import koh.realm.utils.sql.ConnectionResult;
import koh.realm.utils.sql.ConnectionStatement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.util.SystemClock;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by Melancholia on 1/10/16.
 */
public class BannedAddresDAOImp extends BannedAddressDAO {

    private static final Logger logger = LogManager.getLogger(BannedAddresDAOImp.class);

    private final Map<String, Long> addresses = new ConcurrentHashMap<>();

    @Inject
    private @ServiceDependency("RealmServices")
    DatabaseSource dbSource;


    @Override
    public boolean isBanned(String address) {
        return this.addresses.containsKey(address) && this.addresses.get(address) > System.currentTimeMillis();
    }

    @Override
    public Long get(String key)  { //no getByKey bcs catching exception sucks
        return this.addresses.get(key);
    }

    private static final String REPLACE_BY_IP = "REPLACE INTO `suspended_address` VALUES (?,?);";

    @Override
    public void add(String ip , long time){
        this.addresses.put(ip,time);

        try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(REPLACE_BY_IP)){
            PreparedStatement stmt = conn.getStatement();
            stmt.setString(1, ip);
            stmt.setLong(2, time);
            stmt.executeUpdate();
        } catch (Exception e) {
            logger.error(e);
            logger.warn(e.getMessage());
        }
    }

    private static final String FIND_ALL = "SELECT * from suspended_address;";

    private final int loadAll() {
        try (ConnectionResult conn = dbSource.executeQuery(FIND_ALL, 0)) {
            ResultSet cursor = conn.getResult();
            while (cursor.next())
                this.addresses.put(cursor.getString("address"), cursor.getLong("time"));

            return addresses.size();
        } catch (Exception e) {
            logger.error(e);
            logger.warn(e.getMessage());
        }
        return 0;
    }

    private static final String REMOVE_BY_IP = "DELETE FROM `suspended_address` WHERE address = ?;";

    @Override
    public void remove(String key){
        this.addresses.entrySet().stream()
                .filter(en -> en.getKey().equalsIgnoreCase("key"))
                .forEach(en -> {
                        this.addresses.remove(en.getKey());
                    try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(REMOVE_BY_IP)){
                        PreparedStatement stmt = conn.getStatement();
                        stmt.setString(1, en.getKey());
                        stmt.executeUpdate();
                    } catch (Exception e) {
                        logger.error(e);
                        logger.warn(e.getMessage());
                    }
                });
    }

    @Override
    public void start() {
        logger.info(this.loadAll() + " Suspended IPs loaded ");
    }

    @Override
    public void stop() {

    }

    @Override
    public Long getByKey(String key) throws Exception {
        return null;
    }
}
