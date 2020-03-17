package com.funbridge.server.common;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Date;
import java.util.Properties;

/**
 * Created by bplays on 26/10/16.
 */
@Component(value="mailMgr")
@Scope(value="singleton")
public class MailMgr extends FunbridgeMgr{

    @PostConstruct
    public void init() {
        log.info("init");
    }

    @PreDestroy
    public void destroy() {}

    @Override
    public void startUp() {

   }


    /**
     * Send email with default parameters
     * ????????????
     * @param recipients
     * @param subject
     * @param message
     * @return
     */
   public boolean sendDefaultMail(String[] recipients, String subject, String message){
       return sendMail("smtp.goto-games.net", "touch@funbridge.com", recipients, subject, message, null, null);
   }

    /**
     * Send mail
     * @param smtphost host used to send mail
     * @param from @mail of sender
     * @param recipients array of recipient
     * @param subject text for subject
     * @param message body
     * @param attachmentPath path to the file to attach
     * @return true if mail successfully sent
     */
    public boolean sendMail(String smtphost, String from, String[] recipients, String subject, String message, String attachmentPath) {
        return sendMail(smtphost, from, recipients, subject, message, attachmentPath, null);
    }

    /**
     * Send mail
     * @param smtphost host used to send mail
     * @param from @mail of sender
     * @param recipients array of recipient
     * @param subject text for subject
     * @param message body
     * @param attachmentPaths paths to the files to attach
     * @return true if mail successfully sent
     */
    public boolean sendMail(String smtphost, String from, String[] recipients, String subject, String message, String... attachmentPaths) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtphost);
            Session session = Session.getInstance(props);
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from, "Funbridge"));
            InternetAddress[] recipAddress = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                recipAddress[i] = new InternetAddress(recipients[i]);
            }
            msg.setRecipients(Message.RecipientType.TO, recipAddress);
            msg.setSubject(subject);
            msg.setSentDate(new Date());

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(message);
            messageBodyPart.setHeader("Content-Type", "text/html");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            for (String attachmentPath : attachmentPaths) {
                if (attachmentPath != null && !attachmentPath.isEmpty()) {
                    messageBodyPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(attachmentPath);
                    messageBodyPart.setDataHandler(new DataHandler(source));
                    messageBodyPart.setFileName(attachmentPath.substring(attachmentPath.lastIndexOf("/") + 1));
                    multipart.addBodyPart(messageBodyPart);
                }
            }

            msg.setContent(multipart);

            Transport.send(msg);
            if (log.isDebugEnabled()) {
                log.debug("Success send message host:" + smtphost + " - from=" + from + " - subject=" + subject);
            }
            return true;
        } catch (Exception e) {
            log.error("Error send message host:"+smtphost+" - from="+from+" - subject="+subject,e);
        }
        return false;
    }

    /**
     * Send html mail
     * @param smtphost host used to send mail
     * @param from @mail of sender
     * @param recipients array of recipient
     * @param subject text for subject
     * @param html body
     * @param attachmentPath path to the file to attach
     * @return true if mail successfully sent
     */
    public static boolean sendHtmlMail(String smtphost, String from, String[] recipients, String subject, String html, String attachmentPath) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtphost);
            Session session = Session.getInstance(props);
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from, "Funbridge"));
            InternetAddress[] recipAddress = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                recipAddress[i] = new InternetAddress(recipients[i]);
            }
            msg.setRecipients(Message.RecipientType.TO, recipAddress);
            msg.setSubject(subject);
            msg.setSentDate(new Date());

            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(html, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            if(attachmentPath != null && !attachmentPath.isEmpty()){
                messageBodyPart = new MimeBodyPart();
                DataSource source = new FileDataSource(attachmentPath);
                messageBodyPart.setDataHandler(new DataHandler(source));
                messageBodyPart.setFileName(attachmentPath.substring(attachmentPath.lastIndexOf("/")+1));
                multipart.addBodyPart(messageBodyPart);
            }

            msg.setContent(multipart);

            Transport.send(msg);
            System.out.println("Success send message host:" + smtphost + " - from=" + from + " - subject=" + subject);
            return true;
        } catch (Exception e) {
            System.out.println("Error send message host:"+smtphost+" - from="+from+" - subject="+subject);
        }
        return false;
    }
}
