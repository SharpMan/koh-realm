package koh.realm.entities;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import koh.realm.network.RealmClient;

/**
 *
 * @author Neo-Craft
 */
public class Account {

    public static boolean COMPTE_LOGIN(Account to_compare, String name, String pass) {
        if (to_compare != null && to_compare.isValidPass(pass)) {
            return true;
        } else {
            return false;
        }

    }

    public Map<Short, Byte> Characters = new HashMap<>();

    public int ID;
    public String Username;
    public String SHA_HASH, Password;
    public String NickName;
    public byte Right;
    public String SecretQuestion, SecretAnswer, LastIP;
    public Timestamp last_login;
    public long SuspendedTime;
    public RealmClient Client;

    public boolean isValidPass(String Pass) {
        return Password.equals(generateHash(SHA_HASH + Key + Pass));
    }

    public byte getPlayers(short server) {
        if (Characters.containsKey(server)) {
            return Characters.get(server);
        }
        return 0;
    }

    public boolean isBanned() { //TODO: Banip
        if (SuspendedTime == -1) {
            return true;
        }
        if (SuspendedTime < (long) System.currentTimeMillis() / 1000) {
            SuspendedTime = 0;
            //FIXME SAVE NewTime Maybe ?
            return false;
        }
        return true;
    }

    public int getDaysNumberBanned() {
        return (int) ((SuspendedTime - ((long) System.currentTimeMillis() / 1000)) / (24 * 3600));
    }

    public static final String Key = "!@#$%^&*()_+=-{}][;\";/?<>.,";

    public static String generateHash(String toHash) {
        MessageDigest md = null;
        byte[] hash = null;
        try {
            md = MessageDigest.getInstance("SHA-512");
            hash = md.digest(toHash.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return convertToHex(hash);
    }

    /**
     * Converts the given byte[] to a hex string.
     *
     * @param raw the byte[] to convert
     * @return the string the given byte[] represents
     */
    private static String convertToHex(byte[] raw) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < raw.length; i++) {
            sb.append(Integer.toString((raw[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    public void totalClear() {
        try{
            Characters.clear();
            Characters = null;
            ID = 0;
            Username = null;
            SHA_HASH = null;
            Password = null;
            NickName = null;
            Right = 0;
            SecretQuestion = null;
            SecretAnswer = null;
            LastIP = null;
            last_login = null;
            SuspendedTime = 0;
            Client = null;
            this.finalize();
        }
        catch(Throwable tr){
            tr.printStackTrace();
        }
    }

}
