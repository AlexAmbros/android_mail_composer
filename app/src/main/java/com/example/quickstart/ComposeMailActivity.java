package com.example.quickstart;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;

/**
 * Created by alexander on 20.04.17.
 */

public class ComposeMailActivity extends Activity implements SendMailListener<String> {

    private ViewFlipper viewFlipper;
    private ProgressBar loadingProgressBar;
    private TextView exceptionTextView;
    private View cancelButton;
    private View sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_loading);

        initViews();
        initActionBar();
        addFragment();
    }

    /**
     * Called when an activity launched here (specifically, AccountPicker
     * and authorization) exits, giving you the requestCode you started it with,
     * the resultCode it returned, and any additional data from it.
     * @param requestCode code indicating which activity result is incoming.
     * @param resultCode code indicating the result of the incoming
     *     activity result.
     * @param data Intent (containing result data) returned by incoming
     *     activity result.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.fragment_container);
        fragment.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onStartMailSending() {
        exceptionTextView.setText("");
        viewFlipper.setDisplayedChild(1);
    }

    @Override
    public void onEndMailSending(String result) {
        viewFlipper.setDisplayedChild(0);
    }

    @Override
    public void onCancelMailSending(Exception exception) {
        if (exception != null) {
            if (exception instanceof GooglePlayServicesAvailabilityIOException) {
                AdditionalUtils.showGooglePlayServicesAvailabilityErrorDialog(ComposeMailActivity.this,
                        ((GooglePlayServicesAvailabilityIOException) exception).getConnectionStatusCode());
            } else if (exception instanceof UserRecoverableAuthIOException) {
                startActivityForResult(((UserRecoverableAuthIOException) exception).getIntent(),
                        ComposeMailFragment.REQUEST_AUTHORIZATION);
            } else {
                showException("The following error occurred:\n" + exception.getMessage());
            }
        } else {
            showException("Request cancelled.");
        }
    }

    private void initViews() {
        viewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        loadingProgressBar = (ProgressBar) findViewById(R.id.loading_progress_bar);
        exceptionTextView = (TextView) findViewById(R.id.exception_text_view);
    }

    protected void initActionBar() {
        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            LayoutInflater layoutInflater = LayoutInflater.from(this);
            View customView = layoutInflater.inflate(R.layout.action_bar_compose_mail, null);

            cancelButton = customView.findViewById(R.id.cancel_tv);
            sendButton = customView.findViewById(R.id.send_tv);

            actionBar.setCustomView(customView);
        }
    }

    private void addFragment() {
        Fragment fragment = new ComposeMailFragment();
        getFragmentManager().beginTransaction().add(R.id.fragment_container, fragment).commit();

        View.OnClickListener actionBarClickListener = (View.OnClickListener) fragment;
        cancelButton.setOnClickListener(actionBarClickListener);
        sendButton.setOnClickListener(actionBarClickListener);
    }

    public void showException(String exception) {
        exceptionTextView.setText(exception);
        viewFlipper.setDisplayedChild(2);
    }
}