package com.hexin.zbook;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "collections")
public class Collection {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "key")
    public String key;

    @ColumnInfo(name = "version")
    public int version;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "parentCollection")
    public String parentCollection;

    // Transient field to indicate if the item is trashed, not stored in the DB.
    public boolean deleted;
}
