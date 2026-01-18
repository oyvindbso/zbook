package com.hexin.zbook;

import static com.hexin.zbook.Utils.extractYear;

import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import androidx.core.net.ParseException;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import kotlin.text.CharCategory;

public class MainActivity extends AppCompatActivity implements ListAdapter.OnActionClickListener, SearchView.OnQueryTextListener {

    private static final String TAG = "MainActivity";
    private static final String NAV_KEY_ROOT_COLLECTIONS = "ROOT_COLLECTIONS";
    private static final String NAV_KEY_ALL_ITEMS = "ALL_ITEMS";
    private static final String NAV_KEY_RECENT_ITEMS = "RECENT_ITEMS";

    private static final String NAV_KEY_ABOUT = "ABOUT";

    private MainViewModel mMainViewModel;
    private ListAdapter mAdapter;
    private Toolbar mToolbar;
    private SettingsManager mSettingsManager;

    private View mProgressContainer;
    private ProgressBar mProgressBar;
    private TextView mProgressText;


    // ...

    private Deque<NavigationState> mNavigationStack = new ArrayDeque<>();

    private List<Collection> mAllCollectionsCache = new ArrayList<>();
    private List<Item> mAllItemsCache = new ArrayList<>();
    private List<Object> mCurrentlyDisplayedItems = new ArrayList<>();

    private enum SortField { NAME,AUTHOR, PUBLISH_DATE,DATE_ADDED, FILE_SIZE }
    private enum SortOrder { ASC, DESC }

    private SortField mSortField = SortField.NAME;
    private SortOrder mSortOrder = SortOrder.ASC;


    private BroadcastReceiver mSettingsChangedReceiver;

    private static class NavigationState {
        String key;
        String title;

        NavigationState(String key, String title) {
            this.key = key;
            this.title = title;
        }
    }

    private void processPinyinForList(List<Object> list) {
        if (list == null) {
            return;
        }
        for (Object obj : list) {
            if (obj instanceof Item) {
                Item item = (Item) obj;
                if (item.title != null) {
                    item.titlePinyin = PinyinUtils.getFirstSpell(item.title);
                }
            }
        }
    }




    private List<Item> mRecentItemsCache = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettingsManager = SettingsManager.getInstance(this);

        try {
            mSortField = SortField.valueOf(mSettingsManager.getSortField(SortField.NAME.name()));
            mSortOrder = SortOrder.valueOf(mSettingsManager.getSortOrder(SortOrder.ASC.name()));
        } catch (IllegalArgumentException e) {
            // 如果保存的值无效

        }

        setContentView(R.layout.activity_main);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        mProgressContainer = findViewById(R.id.progress_container);
        mProgressBar = findViewById(R.id.progress_bar);
        mProgressText = findViewById(R.id.progress_text);

        mSettingsManager = SettingsManager.getInstance(this);
        mMainViewModel = new ViewModelProvider(this).get(MainViewModel.class);

        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        mAdapter = new ListAdapter(this, mMainViewModel.getDownloadManager());
        recyclerView.setAdapter(mAdapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        setupClickListener();
        observeViewModel();
        setupBroadcastReceiver();

        if (mNavigationStack.isEmpty()) {
            Log.d(TAG, "onCreate: Initial navigation stack is empty. Navigating to root.");
            NavigationState initialState = new NavigationState(NAV_KEY_ROOT_COLLECTIONS, "ZBook");
            mNavigationStack.push(initialState);
            navigateTo(initialState, false);
        }

        mMainViewModel.syncData(mSettingsManager.getApiKey(), mSettingsManager.getUserId());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mSettingsChangedReceiver, new IntentFilter(SettingsActivity.ACTION_SETTINGS_CHANGED));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mSettingsChangedReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false; // We handle search in onQueryTextChange
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText == null || newText.isEmpty()) {
            mAdapter.setData(mCurrentlyDisplayedItems);
        } else {
            String query = newText.toLowerCase();
            List<Object> filteredList = new ArrayList<>();
            for (Object item : mCurrentlyDisplayedItems) {
                if (item instanceof Collection) {
                    if (((Collection) item).name.toLowerCase().contains(query)) {
                        filteredList.add(item);
                    }
                } else if (item instanceof Item) {
                    Item it = (Item) item;

                    // 核心修改：同时检查原始标题和拼音首字母
                    boolean titleMatches = it.title != null && it.title.toLowerCase().contains(query);
                    boolean pinyinMatches = it.titlePinyin != null && it.titlePinyin.toLowerCase().contains(query);

                    if (titleMatches || pinyinMatches) {
                        filteredList.add(item);
                    }

                } else if (item instanceof String) {
                    if (((String) item).toLowerCase().contains(query)) {
                        filteredList.add(item);
                    }
                }
            }
            mAdapter.setData(filteredList);
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem syncItem = menu.findItem(R.id.action_sync);
        if (syncItem != null) {
            if (mMainViewModel.isSyncing().getValue() != null && mMainViewModel.isSyncing().getValue()) {
                // 当正在同步时，显示“取消”图标
                syncItem.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
                syncItem.setTitle("Cancel Sync");
            } else {
                // 当未同步时，显示“同步”图标
                syncItem.setIcon(R.drawable.icons8_refresh_120);
                syncItem.setTitle("Sync");
            }
        }


        // Check the correct sort field item
        if(mSortField == SortField.NAME) menu.findItem(R.id.action_sort_by_name).setChecked(true);
        else if(mSortField == SortField.AUTHOR) menu.findItem(R.id.action_sort_by_author).setChecked(true);
        else if(mSortField == SortField.PUBLISH_DATE) menu.findItem(R.id.action_sort_by_publishdate).setChecked(true);
        else if(mSortField == SortField.DATE_ADDED) menu.findItem(R.id.action_sort_by_date_added).setChecked(true);
        else if(mSortField == SortField.FILE_SIZE) menu.findItem(R.id.action_sort_by_file_size).setChecked(true);

        // Check the correct sort order item
        if(mSortOrder == SortOrder.ASC) menu.findItem(R.id.action_sort_order_asc).setChecked(true);
        else menu.findItem(R.id.action_sort_order_desc).setChecked(true);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_sort_by_name) {
            mSortField = SortField.NAME;
            mSettingsManager.setSortField(mSortField.name()); // <--- 新增
        }
        else if (itemId == R.id.action_sort_by_author) {
            mSortField = SortField.AUTHOR;
            mSettingsManager.setSortField(mSortField.name()); // <--- 新增
        }
        else if (itemId == R.id.action_sort_by_publishdate) {
            mSortField = SortField.PUBLISH_DATE;
            mSettingsManager.setSortField(mSortField.name()); // <--- 新增
        }
        else if (itemId == R.id.action_sort_by_date_added){
            mSortField = SortField.DATE_ADDED;
            mSettingsManager.setSortField(mSortField.name()); // <--- 新增
        }
        else if (itemId == R.id.action_sort_by_file_size) {
            mSortField = SortField.FILE_SIZE;
            mSettingsManager.setSortField(mSortField.name()); // <--- 新增
        }

        else if (itemId == R.id.action_sort_order_asc) {
            mSortOrder = SortOrder.ASC;
            mSettingsManager.setSortOrder(mSortOrder.name()); // <--- 新增
        } else if (itemId == R.id.action_sort_order_desc) {
            mSortOrder = SortOrder.DESC;
            mSettingsManager.setSortOrder(mSortOrder.name());
        }

        else if (itemId == R.id.action_download_center) {
            startActivity(new Intent(this, DownloadActivity.class));
            return true;
        } else if (itemId == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if (itemId == R.id.action_clear_data) {
            mMainViewModel.clearData();
            Toast.makeText(this, "Local data cleared", Toast.LENGTH_SHORT).show();
            return true;
        } else if (itemId == R.id.action_sync) {
            if (mMainViewModel.isSyncing().getValue() != null && mMainViewModel.isSyncing().getValue()) {
                mMainViewModel.cancelSync();
            } else {
                mMainViewModel.syncData(mSettingsManager.getApiKey(), mSettingsManager.getUserId());
            }
            return true;
        }


        if (itemId == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }

        if (itemId == R.id.action_recent_files) {
            navigateTo(new NavigationState(NAV_KEY_RECENT_ITEMS, "Recently Opened"), true);
            return true;
        }

        // If a sort option was selected, mark it as checked and re-render the list
        if(item.getGroupId() == R.id.group_sort_by || item.getGroupId() == R.id.group_sort_order){
            item.setChecked(true);

            if (!mNavigationStack.isEmpty()) navigateTo(mNavigationStack.peek(), false);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setupClickListener() {
        mAdapter.setOnItemClickListener(item -> {
            if (item instanceof String && ((String) item).startsWith("All Items")) {
                navigateTo(new NavigationState(NAV_KEY_ALL_ITEMS, "All Items"), true);
            } if (item instanceof String && ((String) item).startsWith("Recently Opened")) {
                navigateTo(new NavigationState(NAV_KEY_RECENT_ITEMS, "Recently Opened"), true);
            }
            else if (item instanceof Collection) {
                navigateTo(new NavigationState(((Collection) item).key, ((Collection) item).name), true);
            } else if (item instanceof Item) {
                handleItemClick((Item) item);
            }
        });
        mAdapter.setOnActionClickListener(this);

        mToolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupBroadcastReceiver() {
        mSettingsChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent != null && SettingsActivity.ACTION_SETTINGS_CHANGED.equals(intent.getAction())) {
                    if (!mNavigationStack.isEmpty()) {
                        Log.d(TAG, "Settings changed, refreshing current view.");
                        navigateTo(mNavigationStack.peek(), false);
                    }
                }
            }
        };
    }

    private void observeViewModel() {
        mMainViewModel.getAllCollections().observe(this, collections -> {
            mAllCollectionsCache = collections;
            if (!mNavigationStack.isEmpty()) navigateTo(mNavigationStack.peek(), false);
        });

        mMainViewModel.getAllItems().observe(this, items -> {
            mAllItemsCache = items;
            if (!mNavigationStack.isEmpty()){
                String currentKey = mNavigationStack.peek().key;
                 if(NAV_KEY_ROOT_COLLECTIONS.equals(currentKey)) renderTopLevelCollectionsList();
                 else if(NAV_KEY_ALL_ITEMS.equals(currentKey)) renderAllItemsList();
            }
        });

        mMainViewModel.getCollectionCounts().observe(this, counts -> {
            Map<String, Integer> countMap = new HashMap<>();
            for (CollectionCount count : counts) {
                countMap.put(count.collectionKey, count.count);
            }
            mAdapter.setCollectionCounts(countMap);
        });

        mMainViewModel.isSyncing().observe(this, isSyncing -> {
            mProgressContainer.setVisibility(isSyncing ? View.VISIBLE : View.GONE);
            invalidateOptionsMenu();
        });

        mMainViewModel.getSyncProgress().observe(this, progress -> {
            if (progress != null) {
                mProgressText.setText(progress.statusText);
                mProgressBar.setIndeterminate(!progress.isDeterminate);
                if (progress.isDeterminate) {
                    mProgressBar.setMax(progress.total);
                    mProgressBar.setProgress(progress.currentProgress);
                }
            }
        });

        // ... in observeViewModel()
        mMainViewModel.getRecentItems().observe(this, recentItems -> {
            mRecentItemsCache = recentItems;
            if (!mNavigationStack.isEmpty() && NAV_KEY_RECENT_ITEMS.equals(mNavigationStack.peek().key)) {
                renderRecentItemsList(); // 如果当前就在“最近”列表，则刷新它
            }
            invalidateOptionsMenu(); // 刷新菜单，以更新“最近”条目的可见性
        });
        // ...
    }

    private boolean isCollection(String key) {
        for (Collection c : mAllCollectionsCache) {
            if (c.key.equals(key)) {
                return true;
            }
        }
        return false;
    }

    private void handleItemClick(Item item) {
        DownloadManager.DownloadProgress progress = mMainViewModel.getDownloadManager().getDownloadProgress(item.key, item.filename).getValue();



        if ("attachment".equals(item.itemType)) {

            // 如果已经下载，则打开文件
            if (progress != null && progress.state == DownloadManager.DownloadState.DOWNLOADED) {
                openFile(item);
            }
            // 如果它当前没有正在下载或排队，则开始下载
            else if (progress == null || (progress.state != DownloadManager.DownloadState.DOWNLOADING && progress.state != DownloadManager.DownloadState.QUEUED)) {
                mMainViewModel.getDownloadManager().startDownload(item, false); // 'false' 表示这不是一个“批量下载”
                // (可选，但推荐) 给用户一个即时反馈
                Toast.makeText(this, "Download started: " + item.filename, Toast.LENGTH_SHORT).show();
            }
            // 如果它正在下载或排队中，则本次点击不执行任何操作

        }

        else {
            if (!isCollection(item.key)) {
                 mMainViewModel.getChildrenOfItem(item.key, children -> {
                    if (children != null && !children.isEmpty()) {
                        navigateTo(new NavigationState(item.key, item.title), true);
                    }
                });
            }
        }
    }

    private void openFile(Item item) {
        File file = mMainViewModel.getDownloadManager().getLocalFileForItem(item.filename, item.key);
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        mMainViewModel.addRecentItem(item.key);

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
    public void onActionClick(Item item, DownloadManager.DownloadState currentState) {
        DownloadManager downloadManager = mMainViewModel.getDownloadManager();
        switch (currentState) {
            case DOWNLOADING:
            case QUEUED:
                downloadManager.cancelDownload(item);
                break;
            case DOWNLOADED:
                downloadManager.deleteFile(item);
                break;
            default:
                downloadManager.startDownload(item, false);
                break;
        }
    }

    private void navigateTo(NavigationState newState, boolean addToStack) {
        if (newState == null) return;
        if (addToStack) mNavigationStack.push(newState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(mNavigationStack.size() > 1);

        if (NAV_KEY_ROOT_COLLECTIONS.equals(newState.key)) renderTopLevelCollectionsList();
        else if (NAV_KEY_ALL_ITEMS.equals(newState.key)) renderAllItemsList();
        else if (NAV_KEY_RECENT_ITEMS.equals(newState.key))  renderRecentItemsList();
        else renderCollectionOrItemContent(newState.key, newState.title);
    }


    private void renderRecentItemsList() {
        // 使用缓存的 mRecentItemsCache 作为数据源
        List<Object> listItems = new ArrayList<>(mRecentItemsCache);

        // 我们仍然可以对“最近打开”列表，应用当前的排序规则
        //sortList(listItems);

        mAdapter.setIsInRecentItemsView(true); // <--- 新增
        mAdapter.setIsInAllItemsView(false);   // <--- 新增，确保状态互斥


        // 设置工具栏标题
        mToolbar.setTitle("Recently Opened (" + mRecentItemsCache.size() + ")");
        // 更新当前显示的条目和 Adapter

        mCurrentlyDisplayedItems = new ArrayList<>(listItems);
        mAdapter.setData(listItems);
    }
        // 更新当前显示
    private void sortList(List<Object> list) {
        // 1. 定义日期格式。你需要根据 Zotero API 返回的日期格式来选择。
        // Zotero 通常使用 ISO 8601 格式，例如 "2023-10-26T10:00:00Z"。
        // SimpleDateFormat 对于这种带'T'和'Z'的格式处理起来比较麻烦，
        // 在 Android API 26+ 中使用 Instant.parse() 会更简单。
        // 但为了兼容性，我们还是用 SimpleDateFormat，并对字符串做一些处理。
        String pattern = "yyyy-MM-dd";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern, Locale.getDefault());


        Collections.sort(list, (o1, o2) -> {
            boolean isO1String = o1 instanceof String;
            boolean isO2String = o2 instanceof String;
            if (isO1String && !isO2String) return -1;
            if (!isO1String && isO2String) return 1;
            if (isO1String && isO2String) return 0;

            int result = 0;
            switch (mSortField) {
                case NAME:
                    String name1 = (o1 instanceof Collection) ? ((Collection) o1).name : ((Item) o1).title;
                    String name2 = (o2 instanceof Collection) ? ((Collection) o2).name : ((Item) o2).title;
                    result = name1.compareToIgnoreCase(name2);
                    break;
                // ... case NAME: ...

                case AUTHOR:
                    String author1 = (o1 instanceof Item) ? ((Item) o1).creators : "";
                    String author2 = (o2 instanceof Item) ? ((Item) o2).creators : "";
                    if (author1 == null) author1 = "";
                    if (author2 == null) author2 = "";
                    // Collections don't have authors, sort them first
                    if (o1 instanceof Collection) return -1;
                    if (o2 instanceof Collection) return 1;
                    result = author1.compareToIgnoreCase(author2);
                    break;

                // ... case DATE_ADDED: ...

                case PUBLISH_DATE:
                    try {
                        String dateStr1 = ((Item) o1).publicationDate;
                        String dateStr2 = ((Item) o2).publicationDate;

                        // --- 开始健壮的日期比较逻辑 ---

                        // 1. 优先提取年份进行比较
                        Integer year1 = Utils.extractYear(dateStr1);
                        Integer year2 = Utils.extractYear(dateStr2);

                        // 2. 处理一个有年份一个没有的情况
                        if (year1 != null && year2 == null) {
                            result = 1; // 有年份的排在前面 (更明确的信息)
                            break;
                        }
                        if (year1 == null && year2 != null) {
                            result = -1; // 有年份的排在前面
                            break;
                        }

                        // 3. 如果两个都有年份，直接比较年份
                        if (year1 != null && year2 != null) {
                            // 如果年份不相等，直接得出结论
                            if (!year1.equals(year2)) {
                                result = year1.compareTo(year2);
                                break;
                            }
                            // 如果年份相等，回退到字符串比较，以便处理 "2024-05" vs "2024-06"
                            // 这种格式良好的字符串比较是有效的
                        }

                        // 4. 回退：如果两个都没有有效年份，或年份相同，则进行字符串比较
                        // 确保 null 安全
                        if (dateStr1 == null) dateStr1 = "";
                        if (dateStr2 == null) dateStr2 = "";
                        result = dateStr1.compareTo(dateStr2);
                    }catch (Exception e) {

                    }
                    break;


                case DATE_ADDED:
                    try {
                        String date1 = ((Item) o1).dateAdded;
                        String date2 = ((Item) o2).dateAdded;

                        // --- 开始健壮的日期比较逻辑 ---

                        // 1. 优先提取年份进行比较
                        Integer dyear1 = Utils.extractYear(date1);
                        Integer dyear2 = Utils.extractYear(date2);

                        // 2. 处理一个有年份一个没有的情况
                        if (dyear1 != null && dyear2 == null) {
                            result = 1; // 有年份的排在前面 (更明确的信息)
                            break;
                        }
                        if (dyear1 == null && dyear2 != null) {
                            result = -1; // 有年份的排在前面
                            break;
                        }

                        // 3. 如果两个都有年份，直接比较年份
                        if (dyear1 != null && dyear2 != null) {
                            // 如果年份不相等，直接得出结论
                            if (!dyear1.equals(dyear2)) {
                                result = dyear1.compareTo(dyear2);
                                break;
                            }
                            // 如果年份相等，回退到字符串比较，以便处理 "2024-05" vs "2024-06"
                            // 这种格式良好的字符串比较是有效的
                        }

                        // 4. 回退：如果两个都没有有效年份，或年份相同，则进行字符串比较
                        // 确保 null 安全
                        if (date1 == null) date1 = "";
                        if (date2 == null) date2 = "";
                        result = date1.compareTo(date2);
                    }catch (Exception e){

                    }
                    break;
                case FILE_SIZE:
                    long size1 = (o1 instanceof Item && "attachment".equals(((Item)o1).itemType)) ? ((Item) o1).filesize : -1;
                    long size2 = (o2 instanceof Item && "attachment".equals(((Item)o2).itemType)) ? ((Item) o2).filesize : -1;
                    result = Long.compare(size1, size2);
                    break;
            }
            return mSortOrder == SortOrder.ASC ? result : -result;
        });
    }

    private void renderTopLevelCollectionsList() {
        List<Object> listItems = new ArrayList<>();
        if (mRecentItemsCache != null && !mRecentItemsCache.isEmpty()) {
            String recentItemsText = "Recently Opened (" + mRecentItemsCache.size() + ")";
            listItems.add(recentItemsText);
        }

        int allItemsCount;
        if (mSettingsManager.getHideAttachmentsInAllItems()) {
            int count = 0;
            for (Item item : mAllItemsCache) {
                if (!"attachment".equals(item.itemType)) {
                    count++;
                }
            }
            allItemsCount = count;
        } else {
            allItemsCount = mAllItemsCache.size();
        }
        String allItemsText = "All Items (" + allItemsCount + ")";
        listItems.add(allItemsText);

        List<Collection> topLevelCollections = new ArrayList<>();
        for (Collection c : mAllCollectionsCache) {
            if (c.parentCollection == null || c.parentCollection.isEmpty()) {
                topLevelCollections.add(c);
            }
        }
        listItems.addAll(topLevelCollections);
        sortList(listItems);
        mToolbar.setTitle("ZBook (" + topLevelCollections.size() + " collections)");
        
        mCurrentlyDisplayedItems = new ArrayList<>(listItems);
        mAdapter.setData(listItems);
    }

    private void renderAllItemsList() {
        List<Item> itemsToDisplay;
        if (mSettingsManager.getHideAttachmentsInAllItems()) {
            itemsToDisplay = new ArrayList<>();
            for (Item item : mAllItemsCache) {
                if (!"attachment".equals(item.itemType)) {
                    itemsToDisplay.add(item);
                }
            }
        } else {
            itemsToDisplay = new ArrayList<>(mAllItemsCache);
        }

        List<Object> listItems = new ArrayList<>(itemsToDisplay);
        sortList(listItems);

        processPinyinForList(listItems);


        mToolbar.setTitle("All Items (" + listItems.size() + ")");
        mCurrentlyDisplayedItems = new ArrayList<>(listItems);
        mAdapter.setData(listItems);

    }

    private void renderCollectionOrItemContent(String key, String title) {
        if(isCollection(key)){
            mMainViewModel.getCollectionContent(key, content -> {
                List<Object> combinedList = new ArrayList<>();
                if (content.subCollections != null) combinedList.addAll(content.subCollections);
                if (content.items != null) combinedList.addAll(content.items);
                sortList(combinedList);
                mToolbar.setTitle(title + " (" + combinedList.size() + ")");
                mCurrentlyDisplayedItems = new ArrayList<>(combinedList);
                mAdapter.setData(combinedList);
            });
        } else {
             mMainViewModel.getChildrenOfItem(key, children -> {
                if (children != null) {
                    List<Object> listItems = new ArrayList<>(children);
                    sortList(listItems);
                    mToolbar.setTitle(title + " (" + children.size() + ")");
                    mCurrentlyDisplayedItems = new ArrayList<>(listItems);
                    mAdapter.setData(listItems);
                }
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (mNavigationStack.size() > 1) {
            mNavigationStack.pop();
            navigateTo(mNavigationStack.peek(), false);
        } else {
            super.onBackPressed();
        }
    }
}
