package com.example.quickstart;

import android.content.Context;
import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

import java.io.File;
import java.io.IOException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Created by alexander on 20.04.17.
 */

public class SendMailTask extends AsyncTask<String, Void, String> {

    private Context context;
    private GoogleAccountCredential credential;
    private SendMailListener<String> sendMailListener;
    private String accountName;
    private Exception lastException = null;
    private MailAction mailAction;

    SendMailTask(Context context, SendMailListener<String> sendMailListener, GoogleAccountCredential credential, String accountName, MailAction mailAction) {
        this.context = context;
        this.sendMailListener = sendMailListener;
        this.credential = credential;
        this.accountName = accountName;
        this.mailAction = mailAction;
    }

    @Override
    protected void onPreExecute() {
        if (sendMailListener != null) {
            sendMailListener.onStartMailSending();
        }
    }

    /**
     * Background task to call Gmail API.
     * @param params no parameters needed for this task.
     */
    @Override
    protected String doInBackground(String... params) {
        //to - params[0], bcc - params[1], subject - params[2], body - params[3], attachment file - params[4]
        String[] bcc = parseBccList(params[1]);
        Gmail gmailService = createGMailService();

        String draftId = null;
        try {
            if (mailAction.equals(MailAction.SEND)) {
                sendSimpleTextMessage(gmailService, params[0], bcc, params[2], params[3]);
            } else if (mailAction.equals(MailAction.SEND_WITH_ATTACHMENT)) {
                sendTextMessageWithAttachment(gmailService, params[0], bcc, params[2], params[3], params[4]);
            } else if (mailAction.equals(MailAction.SAVE_TO_DRAFT)) {
                draftId = saveMessageToDrafts(gmailService, params[0], bcc, params[2], params[3], params[4]);
            }

            return draftId;
        } catch (Exception e) {
            lastException = e;
            cancel(true);
            return null;
        }
    }

    @Override
    protected void onPostExecute(String output) {
        if (sendMailListener != null) {
            sendMailListener.onEndMailSending(output);
        }
    }

    @Override
    protected void onCancelled() {
        if (sendMailListener != null) {
            sendMailListener.onCancelMailSending(lastException);
        }
    }

    private Gmail createGMailService() {
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        return new Gmail.Builder(transport, jsonFactory, credential).build();
    }

    private void sendSimpleTextMessage(Gmail gmail, String to, String[] bcc, String subject, String body) throws MessagingException, IOException {
        MimeMessage mimeMessage = MailUtils.createEmail(accountName, to, bcc, subject, body);
        Message message = MailUtils.createMessageWithEmail(mimeMessage);
        Message sentMessage = MailUtils.sendMessage(gmail, accountName, message);
    }

    private void sendTextMessageWithAttachment(Gmail gmail, String to, String[] bcc, String subject, String body, String attachmentFilePath) throws MessagingException, IOException {
        File attachment = new File(attachmentFilePath);
        MimeMessage mimeMessage = MailUtils.createEmailWithAttachment(accountName, to, bcc, subject, body, attachment);
        Message message = MailUtils.createMessageWithEmail(mimeMessage);
        Message sentMessage = MailUtils.sendMessage(gmail, accountName, message);
    }

    private String saveMessageToDrafts(Gmail gmail, String to, String[] bcc, String subject, String body, String attachmentFilePath) throws MessagingException, IOException {
        MimeMessage mimeMessage;
        if (attachmentFilePath == null) {
            mimeMessage = MailUtils.createEmail(accountName, to, bcc, subject, body);
        } else {
            File attachment = new File(attachmentFilePath);
            mimeMessage = MailUtils.createEmailWithAttachment(accountName, to, bcc, subject, body, attachment);
        }
        Draft draft = MailUtils.createDraft(gmail, accountName, mimeMessage);

        return draft.getId();
    }

    private String[] parseBccList(String bccListString) {
        if (bccListString != null && bccListString.length() > 0) {
            String emailSeparator = ",";
            String[] emailsList = bccListString.split(emailSeparator);

            return emailsList;
        }

        return null;
    }
}