package koh.realm.dao.impl;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import koh.realm.DatabaseSource;
import koh.realm.dao.api.CharacterDAO;
import koh.realm.utils.sql.ConnectionStatement;

/**
 *
 * @author Neo-Craft
 */
public class CharacterDAOImpl extends CharacterDAO {

    private static final String REPLACE_BY_OWNER = "REPLACE INTO `worlds_characters` VALUES (?,?,?);";

    public boolean insertOrUpdate(int Owner, short server, short number) {
        try (ConnectionStatement<PreparedStatement> conn = DatabaseSource.get().prepareStatement(REPLACE_BY_OWNER)){
            PreparedStatement stmt = conn.getStatement();
            stmt.setInt(1, Owner);
            stmt.setShort(2, server);
            stmt.setShort(3, number);
            return stmt.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Object getByKey(Integer key) throws Exception {
        throw new UnsupportedOperationException();
    }
}
