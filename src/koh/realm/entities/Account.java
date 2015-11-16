package koh.realm.entities;

import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import koh.realm.refact_network.RealmClient;
import koh.repositories.InUseCheckable;

/**
 *
 * @author Neo-Craft
 */
public class Account implements InUseCheckable {

    public Map<Short, Byte> Characters = new HashMap<>();

    public int ID;
    public String Username;
    public String SHA_HASH, Password;
    public String NickName;
    public byte Right;
    public String SecretQuestion, SecretAnswer, LastIP;
    public Timestamp last_login;
    public long SuspendedTime;

    public RealmClient getClient() {
        return client;
    }

    public void setClient(RealmClient client) {
        this.client = client;
    }

    public RealmClient client;

    public boolean isValidPass(String Pass) {
        return Password.equals(generateHash(SHA_HASH + Key + Pass));
    }

    public byte getPlayers(short server) {
        Byte count = Characters.get(server);
        return count == null ? 0 : count;
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
