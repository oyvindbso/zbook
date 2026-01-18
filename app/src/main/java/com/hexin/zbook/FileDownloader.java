package com.hexin.zbook;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class FileDownloader {

    public interface OnDownloadListener {
        void onDownloadComplete(File file);
        void onDownloadFailed();
    }

    public static void download(Context context, String fileUrl, String fileName, OnDownloadListener listener) {
        new DownloadTask(context, listener).execute(fileUrl, fileName);
    }

    private static class DownloadTask extends AsyncTask<String, Void, File> {
        private Context mContext;
        private OnDownloadListener mListener;

        DownloadTask(Context context, OnDownloadListener listener) {
            mContext = context;
            mListener = listener;
        }

        @Override
        protected File doInBackground(String... strings) {
            String fileUrl = strings[0];
            String fileName = strings[1];
            File cacheDir = mContext.getCacheDir();
            File file = new File(cacheDir, fileName);

            try {
                URL url = new URL(fileUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e("FileDownloader", "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage());
                    return null;
                }

                try (InputStream input = connection.getInputStream();
                     FileOutputStream output = new FileOutputStream(file)) {

                    byte[] data = new byte[4096];
                    int count;
                    while ((count = input.read(data)) != -1) {
                        output.write(data, 0, count);
                    }
                }
                return file;

            } catch (IOException e) {
                Log.e("FileDownloader", "Error downloading file", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(File result) {
            if (mListener != null) {
                if (result != null) {
                    mListener.onDownloadComplete(result);
                } else {
                    mListener.onDownloadFailed();
                }
            }
        }
    }
}
