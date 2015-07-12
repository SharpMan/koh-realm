package koh.realm.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import koh.realm.MySQL;

/**
 *
 * @author Neo-Craft
 */
public class CharacterDAO {

    public static boolean Insert(int Owner, short server, short number) {
        try {
            PreparedStatement p;
            if (number == 1) {
                p = MySQL.prepareQuery("DELETE from `worlds_characters` WHERE owner = ? AND server = ?;", MySQL.Connection());
                p.setInt(1, Owner);
                p.setShort(2, server);
                p.execute();
                
                p = MySQL.prepareQuery("INSERT INTO `worlds_characters` VALUES (?,?,?);", MySQL.Connection());
                p.setInt(1, Owner);
                p.setShort(2, server);
                p.setShort(3, number);
            } else {
                p = MySQL.prepareQuery("UPDATE `worlds_characters` SET number = ? WHERE owner = ? AND server = ?;", MySQL.Connection());
                p.setInt(2, Owner);
                p.setShort(3, server);
                p.setShort(1, number);
            }
            p.execute();
            MySQL.closePreparedStatement(p);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

}
