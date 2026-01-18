package com.hexin.zbook;

import android.content.Context;
import android.content.SharedPreferences;

public class SyncMetadataManager {

    private static final String PREFS_NAME = "SyncMetadataPrefs";
    private static final String KEY_LAST_ITEM_VERSION = "lastItemVersion";
    private static final String KEY_LAST_COLLECTION_VERSION = "lastCollectionVersion";

    private final SharedPreferences mPrefs;

    public SyncMetadataManager(Context context) {
        mPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public int getLastItemSyncVersion() {
        return mPrefs.getInt(KEY_LAST_ITEM_VERSION, 0);
    }

    public void setLastItemSyncVersion(int version) {
        mPrefs.edit().putInt(KEY_LAST_ITEM_VERSION, version).apply();
    }

    public int getLastCollectionSyncVersion() {
        return mPrefs.getInt(KEY_LAST_COLLECTION_VERSION, 0);
    }

    public void setLastCollectionSyncVersion(int version) {
        mPrefs.edit().putInt(KEY_LAST_COLLECTION_VERSION, version).apply();
    }
    
    public void clearAll() {
        mPrefs.edit().clear().apply();
    }
}
