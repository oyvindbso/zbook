package com.hexin.zbook;

import android.app.Application;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;

import java.util.List;

public class SyncRepository {

    private final CollectionDao mCollectionDao;
    private final ItemDao mItemDao;
    private final LiveData<List<Collection>> mAllCollections;
    private final LiveData<List<Item>> mAllItems;
    private final LiveData<List<CollectionCount>> mCollectionCounts;
    private final LiveData<List<Item>> mAttachments;

    public SyncRepository(Application application) {
        AppDatabase db = AppDatabase.getDatabase(application);
        mCollectionDao = db.collectionDao();
        mItemDao = db.itemDao();
        mAllCollections = mCollectionDao.getAll();
        mAllItems = mItemDao.getAll();
        mCollectionCounts = mItemDao.getCollectionCounts();
        mAttachments = mItemDao.getAttachments();
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

    public LiveData<List<Item>> getAttachments() {
        return mAttachments;
    }

    public List<Item> getChildrenOfItemSync(String itemKey) {
        return mItemDao.getChildren(itemKey);
    }


    public List<Item> getItemsByCollectionSync(String collectionKey) {
        return mItemDao.getItemsByCollection(collectionKey);
    }

    public List<Collection> getSubCollectionsSync(String parentKey) {
        return mCollectionDao.getSubCollectionsSync(parentKey);
    }

    public void clearCollectionsTable() {
        new ClearCollectionsAsyncTask(mCollectionDao).execute();
    }

    public void clearAllTables() {
        new ClearDatabaseAsyncTask(mCollectionDao, mItemDao).execute();
    }

    private static class ClearCollectionsAsyncTask extends AsyncTask<Void, Void, Void> {
        private final CollectionDao mDao;
        ClearCollectionsAsyncTask(CollectionDao dao) { mDao = dao; }
        @Override
        protected Void doInBackground(Void... voids) {
            mDao.clearTable();
            return null;
        }
    }

    private static class ClearDatabaseAsyncTask extends AsyncTask<Void, Void, Void> {
        private final CollectionDao mCollectionDao;
        private final ItemDao mItemDao;

        ClearDatabaseAsyncTask(CollectionDao collectionDao, ItemDao itemDao) {
            mCollectionDao = collectionDao;
            mItemDao = itemDao;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            mItemDao.clearTable();
            mCollectionDao.clearTable();
            return null;
        }
    }
}
