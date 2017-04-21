package com.example.quickstart;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.GmailScopes;

import java.util.Arrays;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

/**
 * Created by alexander on 20.04.17.
 */

public class ComposeMailFragment extends Fragment implements View.OnClickListener {

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    public static final String PREF_ACCOUNT_NAME = "account_name";
    private static final String[] SCOPES = {GmailScopes.GMAIL_SEND, GmailScopes.GMAIL_COMPOSE};

    private EditText toEditText;
    private EditText bccEditText;
    private EditText subjectEditText;
    private EditText bodyEditText;
    private View sendEmailButton;
    private View saveToDraftButton;

    private GoogleAccountCredential mCredential;
    private ComposeMailActivity activity;
    private SendMailListener sendMailListener;
    private MailAction mailAction;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ComposeMailActivity) {
            this.activity = (ComposeMailActivity) activity;
        }
        if (activity instanceof SendMailListener) {
            sendMailListener = (SendMailListener) activity;
        }
        // Initialize credentials and service object.
        mCredential = GoogleAccountCredential.usingOAuth2(activity, Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_compose_mail, null);
        initViews(rootView);

        return rootView;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.send_tv:
                if (isFieldValid(toEditText, getString(R.string.to_error_message))) {
                    mailAction = MailAction.SEND;
                    callGMailApi();
                }
                break;
//            case R.id.save_to_draft_btn:
//                mailAction = MailAction.SAVE_TO_DRAFT;
//                callGMailApi();
//                break;
        }
    }

    private void initViews(View rootView) {
        toEditText = (EditText) rootView.findViewById(R.id.to_mail_et);
        bccEditText = (EditText) rootView.findViewById(R.id.bcc_mail_et);
        subjectEditText = (EditText) rootView.findViewById(R.id.subject_mail_et);
        bodyEditText = (EditText) rootView.findViewById(R.id.body_mail_et);

//        sendEmailButton = rootView.findViewById(R.id.send_btn);
//        saveToDraftButton = rootView.findViewById(R.id.save_to_draft_btn);
//        sendEmailButton.setOnClickListener(this);
//        saveToDraftButton.setOnClickListener(this);
    }

    public boolean isFieldValid(EditText editText, String errorMessage) {
        if (editText.getText().length() == 0) {
            Toast.makeText(activity, errorMessage, Toast.LENGTH_LONG).show();
            return false;
        }

        return true;
    }

    /**
     * Attempts to set the account used with the API credentials. If an account
     * name was previously saved it will use that one; otherwise an account
     * picker dialog will be shown to the user. Note that the setting the
     * account to use with the credentials object requires the app to have the
     * GET_ACCOUNTS permission, which is requested here if it is not already
     * present. The AfterPermissionGranted annotation indicates that this
     * function will be rerun automatically whenever the GET_ACCOUNTS permission
     * is granted.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(activity, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = activity.getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                callGMailApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(this, "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS, Manifest.permission.GET_ACCOUNTS);
        }
    }

    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    public void callGMailApi() {
        if (!AdditionalUtils.isGooglePlayServicesAvailable(activity)) {
            AdditionalUtils.acquireGooglePlayServices(activity);
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!AdditionalUtils.isDeviceOnline(activity)) {
            activity.showException(getString(R.string.network_absence_error_message));
        } else {
            String accountName = activity.getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            SendMailTask sendMailTask = new SendMailTask(activity, sendMailListener, mCredential, accountName, mailAction);

//            File[] externalStorageDirectory = Environment.getExternalStorageDirectory().listFiles();
//            File attachment = new File(externalStorageDirectory[externalStorageDirectory.length - 1].getAbsolutePath());
            sendMailTask.execute(toEditText.getText().toString(), bccEditText.getText().toString(), subjectEditText.getText().toString(),
                    bodyEditText.getText().toString(), null);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != Activity.RESULT_OK) {
                    activity.showException(getString(R.string.play_services_absence_message));
                } else {
                    callGMailApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = activity.getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        callGMailApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == Activity.RESULT_OK) {
                    callGMailApi();
                }
                break;
        }
    }
}