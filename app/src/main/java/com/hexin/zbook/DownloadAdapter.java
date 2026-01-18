package com.hexin.zbook;

import android.content.res.ColorStateList;
import android.graphics.Color;
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
import java.util.List;

public class DownloadAdapter extends RecyclerView.Adapter<DownloadAdapter.ViewHolder> {

    private List<Item> mAttachments = new ArrayList<>();

    private List<Item> mAllAttachments = new ArrayList<>(); // <--- 新增
    private final LifecycleOwner mLifecycleOwner;
    private final DownloadManager mDownloadManager;
    private OnActionListener mListener;

    public interface OnActionListener {
        void onActionClick(Item item, DownloadManager.DownloadState currentState);
    }

    private void processPinyinForAttachments(List<Item> attachments) {
        if (attachments == null) {
            return;
        }
        for (Item item : attachments) {
            if (item.filename != null) {
                // 我们复用之前在 Item.java 中添加的 transient 字段 titlePinyin,
                // 来存储 filename 的拼音首字母，以用于搜索过滤。
                item.titlePinyin = PinyinUtils.getFirstSpell(item.filename);
            }
        }
    }


    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    private OnItemClickListener mItemClickListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        mItemClickListener = listener;
    }


    public DownloadAdapter(LifecycleOwner lifecycleOwner, DownloadManager downloadManager, OnActionListener listener) {
        mLifecycleOwner = lifecycleOwner;
        mDownloadManager = downloadManager;
        mListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.download_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Item attachment = mAttachments.get(position);
        holder.bind(attachment, mDownloadManager, mLifecycleOwner, mListener, mItemClickListener);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cleanup();
    }

    @Override
    public int getItemCount() {
        return mAttachments.size();
    }

    public void setAttachments(List<Item> attachments) {
        List<Item> processedAttachments = new ArrayList<>(attachments != null ? attachments : new ArrayList<>());
        processPinyinForAttachments(processedAttachments);
        mAllAttachments = new ArrayList<>(processedAttachments);
        mAttachments = new ArrayList<>(processedAttachments);

        notifyDataSetChanged();
    }

    public List<Item> getAttachments() {
        return mAttachments;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView fileIcon;
        final TextView fileName;
        final ProgressBar progressBar;
        final TextView progressText;
        final ImageButton actionButton;
        final ColorStateList defaultFileNameColor;
        final ColorStateList defaultProgressColor;

        private LiveData<DownloadManager.DownloadProgress> progressLiveData;
        private Observer<DownloadManager.DownloadProgress> progressObserver;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.file_icon);
            fileName = itemView.findViewById(R.id.file_name);
            progressBar = itemView.findViewById(R.id.download_progress_bar);
            progressText = itemView.findViewById(R.id.download_progress_text);
            actionButton = itemView.findViewById(R.id.button_action);
            defaultFileNameColor = fileName.getTextColors();
            defaultProgressColor = progressText.getTextColors();
        }




        void bind(Item attachment, DownloadManager downloadManager, LifecycleOwner lifecycleOwner, OnActionListener listener, OnItemClickListener itemClickListener) {
            cleanup();

            fileName.setText(attachment.filename != null ? attachment.filename : "(No Filename)");

            if (attachment.filename != null && attachment.filename.toLowerCase().endsWith(".pdf")) {
                fileIcon.setImageResource(R.drawable.item_type_pdf);
            }else if (attachment.filename != null && attachment.filename.toLowerCase().endsWith(".epub")) {
                fileIcon.setImageResource(R.drawable.file_epub);
            }else {
                fileIcon.setImageResource(R.drawable.item_type_document);
            }


            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(attachment);
                }
            });

            progressObserver = progress -> {
                if (progress == null) return;

                if (progress.state == DownloadManager.DownloadState.DOWNLOADED) {
                    fileName.setTextColor(defaultFileNameColor); // Green
                } else {
                    fileName.setTextColor(defaultFileNameColor);
                }

                boolean isDownloading = progress.state == DownloadManager.DownloadState.DOWNLOADING;
                boolean isSkipped = progress.state == DownloadManager.DownloadState.SKIPPED;
                boolean isFailed = progress.state == DownloadManager.DownloadState.FAILED;

                progressBar.setVisibility(isDownloading ? View.VISIBLE : View.GONE);
                progressText.setVisibility(View.VISIBLE);
                progressText.setTextColor(defaultProgressColor);

                if (isDownloading) {
                    progressBar.setIndeterminate(progress.totalBytes <= 0);
                    progressBar.setMax(100);
                    int percent = progress.totalBytes > 0 ? (int) (progress.bytesDownloaded * 100 / progress.totalBytes) : 0;
                    progressBar.setProgress(percent);
                    progressText.setText(String.format("%s / %s",
                        Formatter.formatFileSize(itemView.getContext(), progress.bytesDownloaded),
                        Formatter.formatFileSize(itemView.getContext(), progress.totalBytes)));
                } else if (isSkipped) {
                     String sizeInfo = progress.totalBytes > 0 ? " (" + Formatter.formatFileSize(itemView.getContext(), progress.totalBytes) + ")" : "";
                    progressText.setText("Skipped" + sizeInfo);
                } else if (isFailed) {
                    progressText.setText(progress.error != null ? progress.error : "Failed");
                    progressText.setTextColor(Color.RED);
                } else if (progress.state == DownloadManager.DownloadState.DOWNLOADED) {
                    long size = progress.totalBytes > 0 ? progress.totalBytes : attachment.filesize;
                     if (size > 0) progressText.setText("" + Formatter.formatFileSize(itemView.getContext(), size) + "");
                     else progressText.setText("Downloaded");
                } else {
                    long size = attachment.filesize > 0 ? attachment.filesize : progress.totalBytes;
                    if (size > 0) {
                        progressText.setText(Formatter.formatFileSize(itemView.getContext(), size));
                    } else {
                        progressText.setText("");
                    }
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
                    case DOWNLOADED_BUT_NOT_EXISTS:
                    case SKIPPED:
                    case FAILED:
                    case NOT_DOWNLOADED:
                    default:
                        actionIconRes = R.drawable.attachment_detail_download;
                        break;
                }
                actionButton.setImageResource(actionIconRes);
                actionButton.setOnClickListener(v -> listener.onActionClick(attachment, progress.state));
            };

            progressLiveData = downloadManager.getDownloadProgress(attachment.key, attachment.filename);
            progressLiveData.observe(lifecycleOwner, progressObserver);
        }

        void cleanup() {
            if (progressLiveData != null && progressObserver != null) {
                progressLiveData.removeObserver(progressObserver);
            }
        }
    }

    public void filter(String query) {
        mAttachments.clear();
        if (query == null || query.isEmpty()) {
            mAttachments.addAll(mAllAttachments);
        } else {
            query = query.toLowerCase();
            for (Item item : mAllAttachments) {
                // 核心修改：同时检查原始文件名和拼音首字母
                boolean filenameMatches = item.filename != null && item.filename.toLowerCase().contains(query);
                boolean pinyinMatches = item.titlePinyin != null && item.titlePinyin.toLowerCase().contains(query);

                if (filenameMatches || pinyinMatches) {
                    mAttachments.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }
}
