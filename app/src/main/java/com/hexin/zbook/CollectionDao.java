package com.hexin.zbook;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CollectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Collection> collections);

    @Query("SELECT * FROM collections")
    LiveData<List<Collection>> getAll();

    @Query("SELECT * FROM collections WHERE parentCollection = :parentKey OR (parentCollection IS NULL AND :parentKey IS NULL)")
    List<Collection> getSubCollectionsSync(String parentKey);

    @Query("SELECT MAX(version) FROM collections")
    int getLatestVersion();

    @Query("DELETE FROM collections")
    void clearTable();

    @Query("DELETE FROM collections WHERE key IN (:keys)")
    void deleteByKeys(List<String> keys);

}
