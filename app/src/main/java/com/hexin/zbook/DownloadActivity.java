package com.hexin.zbook;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadActivity extends AppCompatActivity implements DownloadAdapter.OnActionListener, DownloadAdapter.OnItemClickListener {

    private DownloadViewModel mViewModel;
    private DownloadAdapter mAdapter;
    private DownloadManager mDownloadManager;
    private boolean mIsBulkDownloading = false;

    private View mGlobalProgressContainer;
    private TextView mGlobalDownloadFilename;
    private ProgressBar mGlobalProgressBar;

    private String mCurrentSearchQuery = ""; // <--- 新增：用于保存当前的搜索词

    private final List<LiveData<DownloadManager.DownloadProgress>> mObservedProgresses = new ArrayList<>();
    private final Observer<DownloadManager.DownloadProgress> mProgressObserver = progress -> recalculateTotalSize();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        mViewModel = new ViewModelProvider(this).get(DownloadViewModel.class);
        mDownloadManager = mViewModel.getDownloadManager();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mGlobalProgressContainer = findViewById(R.id.global_progress_container);
        mGlobalDownloadFilename = findViewById(R.id.global_download_filename);
        mGlobalProgressBar = findViewById(R.id.global_download_progress_bar);

        RecyclerView recyclerView = findViewById(R.id.recyclerview_downloads);
        mAdapter = new DownloadAdapter(this, mDownloadManager, this);
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new DownloadAdapter(this, mDownloadManager, this);
        mAdapter.setOnItemClickListener(this); // <--- 新增
        recyclerView.setAdapter(mAdapter);


        observeViewModel();
    }

    private void observeViewModel() {
        mViewModel.getAllAttachments().observe(this, attachments -> {
            for (LiveData<DownloadManager.DownloadProgress> liveData : mObservedProgresses) {
                liveData.removeObserver(mProgressObserver);
            }
            mObservedProgresses.clear();

            mAdapter.setAttachments(attachments);

            // 3. 如果存在搜索词，则立即对新列表应用过滤
            if (mCurrentSearchQuery != null && !mCurrentSearchQuery.isEmpty()) {
                mAdapter.filter(mCurrentSearchQuery);
            }


            if (attachments != null) {
                for (Item attachment : attachments) {
                    LiveData<DownloadManager.DownloadProgress> liveData = mDownloadManager.getDownloadProgress(attachment.key, attachment.filename);
                    liveData.observe(this, mProgressObserver);
                    mObservedProgresses.add(liveData);
                }
            }
            recalculateTotalSize();
        });

        mDownloadManager.getGlobalDownloadProgress().observe(this, globalProgress -> {
            if (globalProgress == null) return;

            mIsBulkDownloading = globalProgress.isBulkDownloading;
            mGlobalProgressContainer.setVisibility(mIsBulkDownloading ? View.VISIBLE : View.GONE);
            invalidateOptionsMenu();

            if (mIsBulkDownloading) {
                mGlobalProgressBar.setIndeterminate(false);
                mGlobalProgressBar.setMax(globalProgress.totalToDownload);
                mGlobalProgressBar.setProgress(globalProgress.downloadedCount);
                mGlobalDownloadFilename.setText(String.format("Downloading: %d / %d",
                        globalProgress.downloadedCount,
                        globalProgress.totalToDownload));
            }
        });
    }

    private  void recalculateTotalSize(){

    }
    private void recalculateTotalSize2() {
        if (mAdapter == null || getSupportActionBar() == null) return;



        long totalSize = 0;
        List<Item> currentAttachments = mAdapter.getAttachments();
        if (currentAttachments != null) {
            for (Item attachment : currentAttachments) {

                DownloadManager.DownloadProgress progress = mDownloadManager.getDownloadProgress(attachment.key, attachment.filename).getValue();
                if (progress != null && progress.state == DownloadManager.DownloadState.DOWNLOADED) {
                    totalSize += progress.totalBytes;

                }

            }
        }
        String formattedSize = Formatter.formatFileSize(this, totalSize);
        // 组合成最终的标题字符串
        String title = String.format("%s / %s", formattedSize);

        // 更新标题
        getSupportActionBar().setTitle(title);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.download_menu, menu);

        // VVVV --- 在这里新增 --- VVVV
        MenuItem searchItem = menu.findItem(R.id.action_search_downloads);
        SearchView searchView = (SearchView) searchItem.getActionView();

        // 1. 监听 SearchView 的展开和关闭
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                // SearchView 展开时，什么都不做
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                // 当用户关闭 SearchView 时，清空搜索词并刷新列表
                mCurrentSearchQuery = "";
                if (mAdapter != null) {
                    mAdapter.filter(mCurrentSearchQuery);
                }
                return true;
            }
        });

        // 2. 恢复搜索状态
        if (mCurrentSearchQuery != null && !mCurrentSearchQuery.isEmpty()) {
            searchItem.expandActionView(); // 展开 SearchView
            searchView.setQuery(mCurrentSearchQuery, false); // 设置搜索词，false 表示不提交查询
            searchView.clearFocus(); // 移除焦点，避免键盘自动弹出
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // 通常我们不需要处理“提交”事件，因为“边输边搜”体验更好
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // 当搜索框的文本发生任何变化时，都调用 Adapter 的 filter 方法

                mCurrentSearchQuery = newText; // <--- 保存搜索词

                if (mAdapter != null) {
                    mAdapter.filter(newText);
                }
                return true;
            }
        });

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem startAllItem = menu.findItem(R.id.action_start_all_downloads);
        if (startAllItem != null) {
            startAllItem.setTitle(mIsBulkDownloading ? "Cancel" : "Download");
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_start_all_downloads) {
            if (mIsBulkDownloading) {
                mViewModel.cancelAllDownloads();
            } else {
                mViewModel.startAllDownloads(mAdapter.getAttachments());
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActionClick(Item item, DownloadManager.DownloadState currentState) {
        switch (currentState) {
            case DOWNLOADING:
            case QUEUED:
                mDownloadManager.cancelDownload(item);
                break;
            case DOWNLOADED:
                mDownloadManager.deleteFile(item);
                break;
            case SKIPPED:
            case DOWNLOADED_BUT_NOT_EXISTS:
            case NOT_DOWNLOADED:
            case FAILED:
            default:
                mDownloadManager.startDownload(item, false);
                break;
        }
    }

    @Override
    public void onItemClick(Item item) {
        DownloadManager.DownloadProgress progress = mDownloadManager.getDownloadProgress(item.key, item.filename).getValue();
        // 只有当文件状态为“已下载”时，才执行打开操作
        if (progress != null && progress.state == DownloadManager.DownloadState.DOWNLOADED) {
            openFile(item);
        }else {
            // 2. 对于所有其他状态（未下载、下载失败、已跳过等），都调用开始下载的方法
            // 我们需要检查它当前是否正在下载或排队中，避免重复操作
            if (progress == null || (progress.state != DownloadManager.DownloadState.DOWNLOADING && progress.state != DownloadManager.DownloadState.QUEUED)) {
                mDownloadManager.startDownload(item, false);
                Toast.makeText(this, "Starting download...", Toast.LENGTH_SHORT).show(); // 给予用户反馈
            } else {
                // 如果已经在下载或排队，可以给个提示
                Toast.makeText(this, "Already in download queue.", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private void openFile(Item item) {
        File file = mDownloadManager.getLocalFileForItem(item.filename, item.key);
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        mViewModel.addRecentItem(item.key);

        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        String mime = getContentResolver().getType(uri);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(intent);

            //mMainViewModel.addRecentItem(item.key);

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No app can handle this file type.", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
