package koh.realm.dao;

public interface DAO<K, V> {

    V getByKey(K key) throws Exception;

}
