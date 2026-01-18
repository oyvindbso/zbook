package com.hexin.zbook;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainViewModel extends AndroidViewModel {

    private final SyncManager mSyncManager;
    private final SyncRepository mRepository;
    private final DownloadManager mDownloadManager;
    private final SettingsManager mSettingsManager;
    private final LiveData<List<Collection>> mAllCollections;
    private final LiveData<List<Item>> mAllItems;
    private final LiveData<List<CollectionCount>> mCollectionCounts;
    private LiveData<List<Item>> mRecentItems;
    // New container class for collection content

    private final ItemDao mItemDao; // <--- 新增
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // <--- 新增

    public static class CollectionContent {
        public final List<Collection> subCollections;
        public final List<Item> items;

        CollectionContent(List<Collection> subCollections, List<Item> items) {
            this.subCollections = subCollections;
            this.items = items;
        }
    }

    // New unified listener
    public interface OnCollectionContentLoadedListener {
        void onContentLoaded(CollectionContent content);
    }

    public MainViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        mItemDao = db.itemDao(); // <--- 新增

        mSettingsManager = SettingsManager.getInstance(application);
        mSyncManager = new SyncManager(application, db);
        mRepository = new SyncRepository(application);
        mDownloadManager = DownloadManager.getInstance(application);
        mAllCollections = mRepository.getAllCollections();
        mAllItems = mRepository.getAllItems();
        mCollectionCounts = mRepository.getCollectionCounts();
        mRecentItems = mItemDao.getRecentItems(); // <--- 新增
    }

    public LiveData<List<Item>> getRecentItems() {
        return mRecentItems;
    }

    public void addRecentItem(String itemKey) {
        // 为了避免阻塞主线程，我们在一个后台线程中执行数据库的写入操作
        executor.execute(() -> {
            mItemDao.updateLastOpenedTimestamp(itemKey, System.currentTimeMillis());
        });
    }
    public LiveData<Boolean> isSyncing() {
        return mSyncManager.isSyncing();
    }

    public LiveData<SyncManager.SyncProgress> getSyncProgress() {
        return mSyncManager.getSyncProgress();
    }

    public LiveData<List<Collection>> getAllCollections() {
        return mAllCollections;
    }

    public LiveData<List<Item>> getAllItems() {
        return mAllItems;
    }

    public LiveData<List<CollectionCount>> getCollectionCounts() {
        return mCollectionCounts;
    }

    public DownloadManager getDownloadManager() {
        return mDownloadManager;
    }

    public void getChildrenOfItem(String itemKey, final OnItemsLoadedListener listener) {
        new GetChildrenAsyncTask(mRepository, listener).execute(itemKey);
    }

    // New unified method to get all content for a collection
    public void getCollectionContent(String collectionKey, final OnCollectionContentLoadedListener listener) {
        new GetCollectionContentAsyncTask(mRepository, listener).execute(collectionKey);
    }

    public void syncData(String apiKey, String userId) {
        String baseUrl = mSettingsManager.getZoteroApiBaseUrl();
        if (!baseUrl.endsWith("/")) baseUrl += "/";
        String userUrl = baseUrl + "users/" + userId + "/";
        
        ApiClient.resetClient(userUrl);
        ZoteroApi api = ApiClient.getClient().create(ZoteroApi.class);
        mSyncManager.updateApi(api);
        mSyncManager.startSync(apiKey);
    }

    public void cancelSync() {
        mSyncManager.cancelSync();
    }

    public void clearData() {
        mRepository.clearAllTables();
        mSyncManager.clearSyncMetadata();
    }

    public interface OnItemsLoadedListener {
        void onItemsLoaded(List<Item> items);
    }

    // This async task remains for fetching item children (attachments/notes)
    private static class GetChildrenAsyncTask extends AsyncTask<String, Void, List<Item>> {
        private SyncRepository mSyncRepository;
        private OnItemsLoadedListener mListener;

        GetChildrenAsyncTask(SyncRepository repo, OnItemsLoadedListener listener) {
            mSyncRepository = repo;
            mListener = listener;
        }

        @Override
        protected List<Item> doInBackground(String... params) {
            return mSyncRepository.getChildrenOfItemSync(params[0]);
        }

        @Override
        protected void onPostExecute(List<Item> result) {
            if (mListener != null) {
                mListener.onItemsLoaded(result);
            }
        }
    }

    // New unified AsyncTask
    private static class GetCollectionContentAsyncTask extends AsyncTask<String, Void, CollectionContent> {
        private final SyncRepository mSyncRepository;
        private final OnCollectionContentLoadedListener mListener;

        GetCollectionContentAsyncTask(SyncRepository repo, OnCollectionContentLoadedListener listener) {
            mSyncRepository = repo;
            mListener = listener;
        }

        @Override
        protected CollectionContent doInBackground(String... params) {
            String key = params[0];
            List<Collection> subCollections = mSyncRepository.getSubCollectionsSync(key);
            List<Item> items = mSyncRepository.getItemsByCollectionSync(key);
            return new CollectionContent(subCollections, items);
        }

        @Override
        protected void onPostExecute(CollectionContent result) {
            if (mListener != null) {
                mListener.onContentLoaded(result);
            }
        }
    }
}
