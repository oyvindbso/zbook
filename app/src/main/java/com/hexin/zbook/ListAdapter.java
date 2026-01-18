package com.hexin.zbook;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    private List<Object> mData = new ArrayList<>();

    private Map<String, Integer> mCollectionCounts = new HashMap<>();
    private OnItemClickListener mItemClickListener;
    private OnActionClickListener mActionClickListener;
    private final LifecycleOwner mLifecycleOwner;
    private final DownloadManager mDownloadManager;
    private Boolean IsInAllItemsView;

    private Boolean IsInRecentItemsView;



    public interface OnItemClickListener {
        void onItemClick(Object item);
    }

    public interface OnActionClickListener {
        void onActionClick(Item item, DownloadManager.DownloadState currentState);
    }

    public ListAdapter(LifecycleOwner lifecycleOwner, DownloadManager downloadManager) {
        mLifecycleOwner = lifecycleOwner;
        mDownloadManager = downloadManager;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }

    public void setIsInAllItemsView(Boolean inAllItemsView) {
        IsInAllItemsView = inAllItemsView;
    }

    public void setIsInRecentItemsView(Boolean b) {
        IsInRecentItemsView = b;
    }
    public void setOnActionClickListener(OnActionClickListener listener) {
        mActionClickListener = listener;
    }

    public void setCollectionCounts(Map<String, Integer> counts) {
        mCollectionCounts = counts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      //  View view = LayoutInflater.from(parent.getContext())
      //          .inflate(R.layout.recyclerview_item, parent, false);
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.download_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Object data = mData.get(position);
        holder.bind(data, mDownloadManager, mLifecycleOwner, mItemClickListener, mActionClickListener, mCollectionCounts);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cleanup();
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    public void setData(List<Object> data) {
        mData = data;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView icon;
        final TextView textView;
        final ProgressBar progressBar;
        final TextView progressText;
        final ImageButton actionButton;
        final ColorStateList defaultTextColor;

        final TextView authorDate;

        private LiveData<DownloadManager.DownloadProgress> progressLiveData;
        private Observer<DownloadManager.DownloadProgress> progressObserver;

        public ViewHolder(View view) {
            super(view);
            icon = view.findViewById(R.id.file_icon);
            textView = view.findViewById(R.id.file_name);
            authorDate = view.findViewById(R.id.item_author_date);
            progressBar = view.findViewById(R.id.download_progress_bar);
            progressText = view.findViewById(R.id.download_progress_text);
            actionButton = view.findViewById(R.id.button_action);
            defaultTextColor = textView.getTextColors();
        }

        void bind(Object data, DownloadManager downloadManager, LifecycleOwner lifecycleOwner, OnItemClickListener itemClickListener, OnActionClickListener actionClickListener, Map<String, Integer> collectionCounts) {
            cleanup();
            textView.setTextColor(defaultTextColor);
            icon.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            progressText.setVisibility(View.GONE);
            actionButton.setVisibility(View.GONE);


            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(data);
                }
            });

            if (data instanceof Item) {
                bindItem((Item) data, downloadManager, lifecycleOwner, actionClickListener);
            } else if (data instanceof Collection) {
                bindCollection((Collection) data, collectionCounts);
            }
            else if (data instanceof String) {
                String text = (String) data;
                textView.setText((String) data);
                authorDate.setVisibility(View.GONE);
                icon.setVisibility(View.GONE);


                // --- 核心修改：根据字符串内容，决定是否显示图标 ---
                if (text.startsWith("All Items")) {
                    icon.setImageResource(R.drawable.icon_cell_library); // 一个代表“全部”的系统图标
                    icon.setVisibility(View.VISIBLE);
                } else if (text.startsWith("Recently Opened")) {
                    icon.setImageResource(android.R.drawable.ic_menu_recent_history); // “历史记录”图标
                    icon.setVisibility(View.VISIBLE);
                } else {
                    // 对于任何其他意外的字符串，保持原有的隐藏图标逻辑
                    icon.setVisibility(View.GONE);
                }
            }
        }

        private void bindCollection(Collection collection, Map<String, Integer> collectionCounts) {
            Integer count = collectionCounts.get(collection.key);
            String countText = (count == null) ? "" : " (" + count + ")";
            textView.setText(collection.name + countText);
            icon.setImageResource(R.drawable.cell_collection);
            authorDate.setVisibility(View.GONE); // 确保 Collection 没有第二行文本
        }

        private void bindItem(Item item, DownloadManager downloadManager, LifecycleOwner lifecycleOwner, OnActionClickListener actionClickListener) {
            textView.setText(item.title);


            // --- 构造 "作者 (发布日期)" 字符串的核心逻辑 ---
            StringBuilder secondaryTextBuilder = new StringBuilder();
            if (item.creators != null && !item.creators.isEmpty()) {
                secondaryTextBuilder.append(item.creators);
            }

            if (item.publicationDate != null && item.publicationDate.length() >= 4) {
                String year = item.publicationDate.substring(0, 4);
                if (secondaryTextBuilder.length() > 0) {
                    secondaryTextBuilder.append(" | ").append(year).append("");
                } else {
                    secondaryTextBuilder.append(year);
                }
            }

            if(item.lastOpenedTimestamp > 0){
                String relativeTime = Utils.formatRelativeTime(item.lastOpenedTimestamp);
                if (secondaryTextBuilder.length() > 0) {
                    secondaryTextBuilder.append(" |").append(relativeTime).append("");
                }  else {
                    secondaryTextBuilder.append(relativeTime);
                }
            }

            if (secondaryTextBuilder.length() > 0) {
                authorDate.setText(secondaryTextBuilder.toString());
                authorDate.setVisibility(View.VISIBLE);
            } else {
                authorDate.setVisibility(View.GONE);
            }
            // --- 核心逻辑结束 ---




            int iconResId;
            if (item.itemType != null) {
                switch (item.itemType) {
                    case "book":
                        iconResId = R.drawable.item_type_book;
                        break;
                    case "journalArticle":
                        iconResId = R.drawable.item_type_journalarticle;
                        break;
                    case "note":
                        iconResId = R.drawable.item_type_note;
                        break;
                    case "attachment":
                        if (item.filename != null && item.filename.toLowerCase().endsWith(".pdf")) {
                            iconResId = R.drawable.item_type_pdf;
                        }else if (item.filename != null && item.filename.toLowerCase().endsWith(".epub")) {
                            iconResId = R.drawable.file_epub;
                        }
                        else {
                            iconResId = R.drawable.item_type_document;
                        }
                        break;
                    default:
                        iconResId = R.drawable.item_type_document;
                        break;
                }
            } else {
                iconResId = R.drawable.item_type_document;
            }
            icon.setImageResource(iconResId);

            if ("attachment".equals(item.itemType)) {
                progressObserver = progress -> {
                    if (progress == null) return;

                    if (progress.state == DownloadManager.DownloadState.DOWNLOADED) {
                        textView.setTextColor(defaultTextColor);
                    } else {
                        textView.setTextColor(defaultTextColor);
                    }

                    boolean isDownloading = progress.state == DownloadManager.DownloadState.DOWNLOADING;
                    boolean isFailed = progress.state == DownloadManager.DownloadState.FAILED;
                    boolean isDownloaded = progress.state == DownloadManager.DownloadState.DOWNLOADED;

                    progressBar.setVisibility(isDownloading ? View.VISIBLE : View.GONE);
                    progressText.setVisibility(View.VISIBLE);
                    actionButton.setVisibility(View.VISIBLE);
                    
                    if (isDownloading) {
                        progressBar.setIndeterminate(progress.totalBytes <= 0);
                        progressBar.setMax(100);
                        int percent = progress.totalBytes > 0 ? (int) (progress.bytesDownloaded * 100 / progress.totalBytes) : 0;
                        progressBar.setProgress(percent);
                        progressText.setText(String.format("%s / %s", Formatter.formatFileSize(itemView.getContext(), progress.bytesDownloaded), Formatter.formatFileSize(itemView.getContext(), progress.totalBytes)));
                    } else if (isFailed) {
                        progressText.setText(progress.error != null ? progress.error : "Failed");
                    } else if (isDownloaded) {
                        long size = progress.totalBytes > 0 ? progress.totalBytes : item.filesize;
                        if(size > 0) progressText.setText(Formatter.formatFileSize(itemView.getContext(), size));
                        else progressText.setText("");
                    } else {
                        if(item.filesize > 0) progressText.setText(Formatter.formatFileSize(itemView.getContext(), item.filesize));
                        else progressText.setText("");
                    }

                    int actionIconRes;
                    switch (progress.state) {
                        case DOWNLOADED:
                            actionIconRes = R.drawable.badge_shareext_failed;
                            break;
                        case DOWNLOADING:
                        case QUEUED:
                            actionIconRes = R.drawable.badge_shareext_failed;
                            break;
                        default:
                            actionIconRes = R.drawable.attachment_detail_download;
                            break;
                    }
                    actionButton.setImageResource(actionIconRes);
                    actionButton.setOnClickListener(v -> {
                        if (actionClickListener != null) {
                            actionClickListener.onActionClick(item, progress.state);
                        }
                    });
                };

                progressLiveData = downloadManager.getDownloadProgress(item.key, item.filename);
                progressLiveData.observe(lifecycleOwner, progressObserver);
            }
        }

        void cleanup() {
            if (progressLiveData != null && progressObserver != null) {
                progressLiveData.removeObserver(progressObserver);
                progressLiveData = null;
                progressObserver = null;
            }
        }
    }

}
