package koh.realm.dao.api;

import koh.patterns.services.api.Service;
import koh.realm.dao.DAO;

/**
 * Created by Melancholia on 1/10/16.
 */
public abstract class BannedAddressDAO implements DAO<String, Long>, Service {

    public abstract boolean isBanned(String address);

    public abstract Long get(String key);

    public abstract void add(String ip, long time);

    public abstract void remove(String key);
}
