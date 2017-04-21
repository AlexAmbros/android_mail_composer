package com.example.quickstart;

import com.google.api.client.util.Base64;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by alexander on 20.04.17.
 */

public class MailUtils {

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param from email address of the sender, the mailbox account
     * @param to email address of the receiver
     * @param bcc
     * @param subject subject of the email
     * @param bodyText body text of the email
     * @return the MimeMessage to be used to send email
     * @throws MessagingException
     */
    public static MimeMessage createEmail(String from, String to, String[] bcc, String subject, String bodyText) throws MessagingException {
        MimeMessage email = createMailHeader(from, to, bcc, subject);
        email.setSubject(subject);

        //set text as body
        email.setText(bodyText);
        return email;
    }

    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param from Email address of the sender, the mailbox account.
     * @param to Email address of the receiver.
     * @param subject Subject of the email.
     * @param bodyText Body text of the email.
     * @param file Path to the file to be attached.
     * @return MimeMessage to be used to send email.
     * @throws MessagingException
     */
    public static MimeMessage createEmailWithAttachment(String from, String to, String[] bcc, String subject, String bodyText, File file)
            throws MessagingException, IOException {
        MimeMessage email = createMailHeader(from, to, bcc, subject);

        //add body and attachment
        MimeBodyPart mimeBodyPart = new MimeBodyPart();
        mimeBodyPart.setContent(bodyText, "text/plain");

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(mimeBodyPart);

        mimeBodyPart = new MimeBodyPart();
        DataSource source = new FileDataSource(file);

        mimeBodyPart.setDataHandler(new DataHandler(source));
        mimeBodyPart.setFileName(file.getName());

        multipart.addBodyPart(mimeBodyPart);
        email.setContent(multipart);

        return email;
    }

    private static MimeMessage createMailHeader(String from, String to, String[] bcc, String subject) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        if (bcc != null && bcc.length > 0) {
            for (String emailAddress : bcc) {
                email.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(emailAddress));
            }
        }

        email.setSubject(subject);

        return email;
    }

    /**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     * @return a message containing a base64url encoded email
     * @throws IOException
     * @throws MessagingException
     */
    public static Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param message Email to be sent.
     * @return The sent message
     * @throws MessagingException
     * @throws IOException
     */
    public static Message sendMessage(Gmail service, String userId, Message message) throws MessagingException, IOException {
        message = service.users().messages().send(userId, message).execute();
        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message;
    }

    /**
     * Create draft email.
     *
     * @param service an authorized Gmail API instance
     * @param userId user's email address. The special value "me"
     * can be used to indicate the authenticated user
     * @param emailContent the MimeMessage used as email within the draft
     * @return the created draft
     * @throws MessagingException
     * @throws IOException
     */
    public static Draft createDraft(Gmail service, String userId, MimeMessage emailContent) throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        Draft draft = new Draft();
        draft.setMessage(message);
        draft = service.users().drafts().create(userId, draft).execute();

        System.out.println("Draft id: " + draft.getId());
        System.out.println(draft.toPrettyString());
        return draft;
    }

    /**
     * Delete draft email.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param draftId ID of Draft to deleted.
     * @throws IOException
     */
    public static void deleteDraft(Gmail service, String userId, String draftId) throws IOException{
        service.users().drafts().delete(userId, draftId).execute();
        System.out.println("Draft with id: " + draftId + " deleted successfully.");
    }
}