package com.hexin.zbook;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DownloadViewModel extends AndroidViewModel {

    private final DownloadManager mDownloadManager;
    private final SyncRepository mRepository;
    private final LiveData<List<Item>> mAllAttachments;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(); // <---
    private final ItemDao mItemDao;
    public DownloadViewModel(Application application) {
        super(application);
        AppDatabase db = AppDatabase.getDatabase(application);
        mItemDao = db.itemDao();
        mDownloadManager = DownloadManager.getInstance(application);
        mRepository = new SyncRepository(application);
        // This needs to fetch the complete Item object, including filesize
        mAllAttachments = mRepository.getAttachments();
    }

    public LiveData<List<Item>> getAllAttachments() {
        return mAllAttachments;
    }

    public DownloadManager getDownloadManager() {
        return mDownloadManager;
    }

    public void startAllDownloads(List<Item> attachments) {
        mDownloadManager.startAllDownloads(attachments);
    }

    public void cancelAllDownloads() {
        mDownloadManager.cancelAllDownloads();
    }



    public void addRecentItem(String itemKey) {
        // 为了避免阻塞主线程，我们在一个后台线程中执行数据库的写入操作
        executor.execute(() -> {
            mItemDao.updateLastOpenedTimestamp(itemKey, System.currentTimeMillis());
        });
    }


}
