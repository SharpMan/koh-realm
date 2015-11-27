package koh.realm.dao.impl;

import com.google.inject.Inject;
import koh.patterns.services.api.ServiceDependency;
import koh.realm.app.DatabaseSource;
import koh.realm.dao.api.CharacterDAO;
import koh.realm.utils.sql.ConnectionStatement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;

/**
 *
 * @author Neo-Craft
 */
public class CharacterDAOImpl extends CharacterDAO {

    private static final Logger logger = LogManager.getLogger(CharacterDAO.class);

    private final DatabaseSource dbSource;

    @Inject
    public CharacterDAOImpl(@ServiceDependency("RealmServices") DatabaseSource dbSource) {
        this.dbSource = dbSource;
    }

    private static final String REPLACE_BY_OWNER = "REPLACE INTO `worlds_characters` VALUES (?,?,?);";

    public boolean insertOrUpdate(int Owner, short server, short number) {
        try (ConnectionStatement<PreparedStatement> conn = dbSource.prepareStatement(REPLACE_BY_OWNER)){
            PreparedStatement stmt = conn.getStatement();
            stmt.setInt(1, Owner);
            stmt.setShort(2, server);
            stmt.setShort(3, number);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            logger.error(e);
            logger.warn(e.getMessage());
        }
        return false;
    }

    @Override
    public Object getByKey(Integer key) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
