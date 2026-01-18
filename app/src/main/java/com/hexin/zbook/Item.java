package com.hexin.zbook;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "items")
public class Item {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "key")
    public String key;

    @ColumnInfo(name = "version")
    public int version;

    @ColumnInfo(name = "itemType")
    public String itemType;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "collectionKey")
    public String collectionKey;

    @ColumnInfo(name = "parentItem")
    public String parentItem;

    @ColumnInfo(name = "dateAdded")
    public String dateAdded;

    @ColumnInfo(name = "dateModified")
    public String dateModified;

    // Attachment specific fields
    @ColumnInfo(name = "filename")
    public String filename;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "filesize")
    public long filesize;
    // 这个字段将不会被 Room 存入数据库
    public transient String titlePinyin;
    
    @ColumnInfo(name = "creators")
    public String creators;


    @ColumnInfo(name = "publicationDate")
    public String publicationDate;

    @ColumnInfo(name = "lastOpenedTimestamp", defaultValue = "0")
    public long lastOpenedTimestamp;

 
    // ... 其他字段
    // Transient field to indicate if the item is trashed, not stored in the DB.
    public boolean deleted;
}
