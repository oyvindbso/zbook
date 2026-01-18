package com.hexin.zbook;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Response;

public class SyncManager {

    private static final String TAG = "SyncManager";
    private static final String PREFS_NAME = "SyncMetadata";
    private static final String KEY_LIBRARY_VERSION = "library_version";
    private static final int PAGE_SIZE = 100;

    private final Context mContext;
    private final AppDatabase mDb;
    private ZoteroApi mZoteroApi;
    private SyncTask mCurrentSyncTask;

    private final MutableLiveData<Boolean> mIsSyncing = new MutableLiveData<>(false);
    private final MutableLiveData<SyncProgress> mSyncProgress = new MutableLiveData<>();

    // Helper class to return multiple values from getTotalCount
    private static class CountResult {
        final int count;
        final int version;

        CountResult(int count, int version) {
            this.count = count;
            this.version = version;
        }
    }

    public SyncManager(Application application, AppDatabase db) {
        mContext = application.getApplicationContext();
        mDb = db;
    }

    public void updateApi(ZoteroApi api) {
        mZoteroApi = api;
    }

    public LiveData<Boolean> isSyncing() {
        return mIsSyncing;
    }

    public LiveData<SyncProgress> getSyncProgress() {
        return mSyncProgress;
    }

    public void startSync(String apiKey) {
        if (mZoteroApi == null) {
            Log.e(TAG, "Sync started before ZoteroApi was initialized.");
            return;
        }
        if (mIsSyncing.getValue() != null && mIsSyncing.getValue()) {
            return;
        }
        mCurrentSyncTask = new SyncTask(apiKey);
        mCurrentSyncTask.execute();
    }

    public void cancelSync() {
        if (mCurrentSyncTask != null) {
            mCurrentSyncTask.cancel(true);
        }
    }

    public void clearSyncMetadata() {
        getPrefs().edit().clear().apply();
    }

    private SharedPreferences getPrefs() {
        return mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    private class SyncTask extends AsyncTask<Void, SyncProgress, String> {
        private final String mApiKey;
        private int libraryVersion;

        SyncTask(String apiKey) {
            mApiKey = apiKey;
        }

        @Override
        protected void onPreExecute() {
            mIsSyncing.postValue(true);
            libraryVersion = getPrefs().getInt(KEY_LIBRARY_VERSION, 0);
            Log.d(TAG, "Starting sync from library version: " + libraryVersion);
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                int itemsLatestVersion = syncItems(libraryVersion);
                int collectionsLatestVersion = syncCollections(libraryVersion);
                int deletedLatestVersion = 0;

                if (libraryVersion > 0) {
                    deletedLatestVersion = syncDeletions(libraryVersion);
                }

                return String.valueOf(Math.max(Math.max(itemsLatestVersion, collectionsLatestVersion), deletedLatestVersion));

            } catch (Exception e) {
                Log.e(TAG, "Sync failed", e);
                publishProgress(new SyncProgress("Sync failed: " + e.getMessage(), false, 0, 0));
                return null;
            }
        }

        private int syncItems(int sinceVersion) throws Exception {
            CountResult countResult = getTotalCount(true, sinceVersion);
            int totalItems = countResult.count;
            int lastVersion = countResult.version;
            int start = 0;

            publishProgress(new SyncProgress("Syncing items...", totalItems > 0, 0, totalItems));
            if(totalItems == 0) return lastVersion;

            while (true) {
                if (isCancelled()) return sinceVersion;
                Log.d(TAG, "Executing getItems: since=" + sinceVersion + ", start=" + start);
                Response<List<Item>> response = mZoteroApi.getItems(3, mApiKey, sinceVersion, start, PAGE_SIZE, 0, "-annotation").execute();
                if (!response.isSuccessful()) {
                    throw new Exception("Failed to get items: " + response.code() + " " + response.message());
                }

                lastVersion = Math.max(lastVersion, getVersionFromHeader(response, lastVersion));
                List<Item> items = response.body();
                if (items == null || items.isEmpty()) break;

                List<Item> itemsToUpsert = new ArrayList<>();
                List<String> itemKeysToDelete = new ArrayList<>();
                for(Item item : items){
                    if(item.deleted){
                        itemKeysToDelete.add(item.key);
                    } else {
                        itemsToUpsert.add(item);
                    }
                }

                if (!itemsToUpsert.isEmpty()) mDb.itemDao().insertAll(itemsToUpsert);
                if (!itemKeysToDelete.isEmpty()) mDb.itemDao().deleteByKeys(itemKeysToDelete);
                
                start += items.size();
                publishProgress(new SyncProgress("Synced " + start + " / " + totalItems + " items...", true, start, totalItems));
            }
            return lastVersion;
        }

        private int syncCollections(int sinceVersion) throws Exception {
            CountResult countResult = getTotalCount(false, sinceVersion);
            int totalCollections = countResult.count;
            int lastVersion = countResult.version;
            int start = 0;

            publishProgress(new SyncProgress("Syncing collections...", totalCollections > 0, 0, totalCollections));
            if(totalCollections == 0) return lastVersion;

            while (true) {
                if (isCancelled()) return sinceVersion;
                Log.d(TAG, "Executing getCollections: since=" + sinceVersion + ", start=" + start);
                Response<List<Collection>> response = mZoteroApi.getCollections(3, mApiKey, sinceVersion, start, PAGE_SIZE, 1).execute();
                if (!response.isSuccessful()) {
                    throw new Exception("Failed to get collections: " + response.code() + " " + response.message());
                }

                lastVersion = Math.max(lastVersion, getVersionFromHeader(response, lastVersion));
                List<Collection> collections = response.body();
                if (collections == null || collections.isEmpty()) break;

                List<Collection> collectionsToUpsert = new ArrayList<>();
                List<String> collectionKeysToDelete = new ArrayList<>();
                for(Collection collection : collections){
                    if(collection.deleted){
                        collectionKeysToDelete.add(collection.key);
                    } else {
                        collectionsToUpsert.add(collection);
                    }
                }

                if (!collectionsToUpsert.isEmpty()) mDb.collectionDao().insertAll(collectionsToUpsert);
                if (!collectionKeysToDelete.isEmpty()) mDb.collectionDao().deleteByKeys(collectionKeysToDelete);

                start += collections.size();
                publishProgress(new SyncProgress("Synced " + start + " / " + totalCollections + " collections...", true, start, totalCollections));
            }
            return lastVersion;
        }

        private int syncDeletions(int sinceVersion) throws Exception {
            publishProgress(new SyncProgress("Checking for deleted data...", false, 0, 0));
            if (isCancelled()) return sinceVersion;

            Log.d(TAG, "Executing getDeleted: since=" + sinceVersion);
            Response<Deleted> response = mZoteroApi.getDeleted(3, mApiKey, sinceVersion).execute();
            
            if(response.code() == 304){
                Log.d(TAG, "/deleted endpoint returned 304 Not Modified. No deletions to process.");
                return getVersionFromHeader(response, sinceVersion);
            }

            if (!response.isSuccessful() || response.body() == null) {
                 Log.w(TAG, "Failed to get deleted data: " + response.message());
                 return sinceVersion;
            }

            int lastVersion = getVersionFromHeader(response, sinceVersion);
            Deleted deleted = response.body();
            Log.d(TAG, "/deleted endpoint response: collections=" + (deleted.collections != null ? TextUtils.join(",", deleted.collections) : "null") + ", items=" + (deleted.items != null ? TextUtils.join(",", deleted.items) : "null"));

            if (deleted.collections != null && !deleted.collections.isEmpty()) {
                Log.d(TAG, "Preparing to delete " + deleted.collections.size() + " collections from local DB.");
                mDb.collectionDao().deleteByKeys(deleted.collections);
            }

            if (deleted.items != null && !deleted.items.isEmpty()) {
                Log.d(TAG, "Preparing to delete " + deleted.items.size() + " items from local DB.");
                mDb.itemDao().deleteByKeys(deleted.items);
            }
            return lastVersion;
        }

        private int getVersionFromHeader(Response<?> response, int fallback) {
            String versionHeader = response.headers().get("Last-Modified-Version");
            if (versionHeader != null) {
                try {
                    return Integer.parseInt(versionHeader);
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Could not parse Last-Modified-Version header: " + versionHeader);
                }
            }
            return fallback;
        }

        private CountResult getTotalCount(boolean isItems, Integer since) throws Exception {
            Response<Void> response;
            if (isItems) {
                Log.d(TAG, "Executing getItemsCount: since=" + since);
                response = mZoteroApi.getItemsCount(3, mApiKey, since, 0).execute();
            } else {
                Log.d(TAG, "Executing getCollectionsCount: since=" + since);
                response = mZoteroApi.getCollectionsCount(3, mApiKey, since, 0).execute();
            }

            if (response.isSuccessful() || response.code() == 304) {
                String totalResults = response.headers().get("Total-Results");
                int count = (response.isSuccessful() && totalResults != null) ? Integer.parseInt(totalResults) : 0;
                int version = getVersionFromHeader(response, since != null ? since : 0);
                return new CountResult(count, version);
            }
            
            throw new Exception("Failed to get count: " + response.code() + " " + response.message());
        }

        @Override
        protected void onProgressUpdate(SyncProgress... values) {
            if (values.length > 0) mSyncProgress.postValue(values[0]);
        }

        @Override
        protected void onPostExecute(String newVersion) {
            if (newVersion != null) {
                int version = Integer.parseInt(newVersion);
                 if (version > libraryVersion) {
                    getPrefs().edit().putInt(KEY_LIBRARY_VERSION, version).apply();
                    Log.d(TAG, "Sync finished. New library version: " + newVersion);
                 }
            }
            mIsSyncing.postValue(false);
            mCurrentSyncTask = null;
        }

        @Override
        protected void onCancelled() {
            mIsSyncing.postValue(false);
            mCurrentSyncTask = null;
            mSyncProgress.postValue(new SyncProgress("Sync cancelled", false, 0, 0));
        }
    }

    public static class SyncProgress {
        public final String statusText; public final boolean isDeterminate; public final int currentProgress; public final int total;
        public SyncProgress(String s, boolean i, int c, int t) { statusText = s; isDeterminate = i; currentProgress = c; total = t; }
    }
}
