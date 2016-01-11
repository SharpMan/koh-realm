package koh.realm.entities;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import koh.realm.internet.RealmClient;
import koh.repositories.InUseCheckable;

/**
 *
 * @author Neo-Craft
 */
public class Account implements InUseCheckable {

    public Map<Short, Byte> characters = new HashMap<>();

    public int id;
    public String username;
    public String SHA_HASH, password;
    public String nickName;
    public byte right;
    public String secretQuestion, secretAnswer, lastIP;
    public Timestamp last_login;
    public long suspendedTime;

    public RealmClient getClient() {
        return client;
    }

    public void setClient(RealmClient client) {
        this.client = client;
    }

    public RealmClient client;

    public boolean isValidPass(String Pass) {
        return password.equals(generateHash(SHA_HASH + KEY + Pass));
    }

    public byte getPlayers(short server) {
        Byte count = characters.get(server);
        return count == null ? 0 : count;
    }

    public boolean isBanned() {
        if (suspendedTime == -1) {
            return true;
        }
        if (suspendedTime == 0 || suspendedTime <  System.currentTimeMillis()) {
            return false;
        }
        return true;
    }


    public static final String KEY = "!@#$%^&*()_+=-{}][;\";/?<>.,";

    public static String generateHash(String toHash) {
        byte[] hash = null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
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
        StringBuilder sb = new StringBuilder();
        for (byte aRaw : raw) {
            sb.append(Integer.toString((aRaw & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    @Override
    public boolean inUse() {
        return client != null && client.connected();
    }
}
