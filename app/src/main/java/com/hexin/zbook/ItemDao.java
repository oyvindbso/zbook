package com.hexin.zbook;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Item> items);

    @Query("SELECT * FROM items")
    LiveData<List<Item>> getAll();

    @Query("SELECT * FROM items WHERE itemType = 'attachment'")
    LiveData<List<Item>> getAttachments();

    @Query("SELECT collectionKey, COUNT(*) as count FROM items WHERE collectionKey IS NOT NULL AND collectionKey != '' GROUP BY collectionKey")
    LiveData<List<CollectionCount>> getCollectionCounts();

    @Query("SELECT * FROM items WHERE parentItem = :parentKey")
    List<Item> getChildren(String parentKey);

    @Query("SELECT * FROM items WHERE ',' || collectionKey || ',' LIKE '%,' || :collectionKey || ',%'")
    List<Item> getItemsByCollection(String collectionKey);

    @Query("SELECT MAX(version) FROM items")
    int getLatestVersion();

    @Query("DELETE FROM items")
    void clearTable();

    @Query("DELETE FROM items WHERE key IN (:keys)")
    void deleteByKeys(List<String> keys);

    @Query("UPDATE items SET filesize = :filesize WHERE key = :itemKey")
    void updateFileSize(String itemKey, long filesize);

    @Query("UPDATE items SET lastOpenedTimestamp = :timestamp WHERE `key` = :itemKey")
    void updateLastOpenedTimestamp(String itemKey, long timestamp);

    @Query("SELECT * FROM items WHERE lastOpenedTimestamp > 0 ORDER BY lastOpenedTimestamp DESC LIMIT 100")
    LiveData<List<Item>> getRecentItems();


}
