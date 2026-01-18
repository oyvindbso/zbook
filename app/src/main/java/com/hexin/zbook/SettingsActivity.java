package com.hexin.zbook;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

public class SettingsActivity extends AppCompatActivity {

    public static final String ACTION_SETTINGS_CHANGED = "com.hexin.zbook.ACTION_SETTINGS_CHANGED";

    private SettingsManager mSettingsManager;
    private SettingsViewModel mViewModel;

    private TextInputEditText mZoteroUrlEditText;
    private TextInputEditText mApiKeyEditText;
    private TextInputEditText mUserIdEditText;
    private TextInputEditText mUrlEditText;
    private TextInputEditText mUsernameEditText;
    private TextInputEditText mPasswordEditText;
    private TextInputEditText mThresholdEditText;
    private SwitchMaterial mHideAttachmentsSwitch;
    private Button mTestZoteroButton;
    private Button mTestWebdavButton;
    private Button mSaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSettingsManager = SettingsManager.getInstance(this);
        mViewModel = new ViewModelProvider(this).get(SettingsViewModel.class);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Settings");

        mZoteroUrlEditText = findViewById(R.id.edit_text_zotero_url);
        mApiKeyEditText = findViewById(R.id.edit_text_api_key);
        mUserIdEditText = findViewById(R.id.edit_text_user_id);
        mUrlEditText = findViewById(R.id.edit_text_webdav_url);
        mUsernameEditText = findViewById(R.id.edit_text_webdav_username);
        mPasswordEditText = findViewById(R.id.edit_text_webdav_password);
        mThresholdEditText = findViewById(R.id.edit_text_download_threshold);
        mHideAttachmentsSwitch = findViewById(R.id.switch_hide_attachments);
        mSaveButton = findViewById(R.id.button_save_settings);
        mTestZoteroButton = findViewById(R.id.button_test_zotero);
        mTestWebdavButton = findViewById(R.id.button_test_webdav);

        setupListeners();
        loadSettings();
        observeViewModel();
    }

    private void setupListeners() {
        mHideAttachmentsSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            mSettingsManager.setHideAttachmentsInAllItems(isChecked);
            Intent intent = new Intent(ACTION_SETTINGS_CHANGED);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        });

        mSaveButton.setOnClickListener(v -> saveSettings());
        mTestZoteroButton.setOnClickListener(v -> mViewModel.testZoteroConnection(
                mZoteroUrlEditText.getText().toString().trim(),
                mApiKeyEditText.getText().toString().trim(),
                mUserIdEditText.getText().toString().trim()));

        mTestWebdavButton.setOnClickListener(v -> mViewModel.testWebDavConnection(
                mUrlEditText.getText().toString().trim(),
                mUsernameEditText.getText().toString().trim(),
                mPasswordEditText.getText().toString()));
    }

    private void loadSettings() {
        mZoteroUrlEditText.setText(mSettingsManager.getZoteroApiBaseUrl());
        mApiKeyEditText.setText(mSettingsManager.getApiKey());
        mUserIdEditText.setText(mSettingsManager.getUserId());
        mUrlEditText.setText(mSettingsManager.getWebDavUrl());
        mUsernameEditText.setText(mSettingsManager.getWebDavUsername());
        mPasswordEditText.setText(mSettingsManager.getWebDavPassword());
        mThresholdEditText.setText(String.valueOf(mSettingsManager.getDownloadSizeThresholdMb()));
        mHideAttachmentsSwitch.setChecked(mSettingsManager.getHideAttachmentsInAllItems());
    }

    private void saveSettings() {
        mSettingsManager.setZoteroApiBaseUrl(mZoteroUrlEditText.getText().toString().trim());
        mSettingsManager.setApiKey(mApiKeyEditText.getText().toString().trim());
        mSettingsManager.setUserId(mUserIdEditText.getText().toString().trim());
        mSettingsManager.setWebDavUrl(mUrlEditText.getText().toString().trim());
        mSettingsManager.setWebDavUsername(mUsernameEditText.getText().toString().trim());
        mSettingsManager.setWebDavPassword(mPasswordEditText.getText().toString());

        try {
            int threshold = Integer.parseInt(mThresholdEditText.getText().toString());
            mSettingsManager.setDownloadSizeThresholdMb(threshold);
        } catch (NumberFormatException e) {
            mSettingsManager.setDownloadSizeThresholdMb(-1);
        }

        Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void observeViewModel() {
        mViewModel.getTestResult().observe(this, event -> {
            String message = event.getContentIfNotHandled();
            if (message != null) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            }
        });

        mViewModel.isTesting().observe(this, isTesting -> {
            mTestZoteroButton.setEnabled(!isTesting);
            mTestWebdavButton.setEnabled(!isTesting);
            mSaveButton.setEnabled(!isTesting);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
