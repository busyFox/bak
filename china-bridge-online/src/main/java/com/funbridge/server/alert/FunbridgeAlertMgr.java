package com.funbridge.server.alert;

import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

/**
 * Created by pserent on 12/03/2014.
 * Manager for alert server master !
 * ??????????
 */
@Component(value="alertMgr")
@Scope(value="singleton")
public class FunbridgeAlertMgr extends FunbridgeMgr {
    public static final String ALERT_LEVEL_SEVERE = "SEVERE";
    public static final String ALERT_LEVEL_HIGH = "HIGH";
    public static final String ALERT_LEVEL_MODERATE = "MODERATE";
    public static final String ALERT_LEVEL_LOW = "LOW";

    public static final String ALERT_CATEGORY_STORE = "STORE";

    @Resource(name="mongoTemplate")
    private MongoTemplate mongoTemplate;

    @PostConstruct
    @Override
    public void init() {
    }

    @PreDestroy
    @Override
    public void destroy() {
    }


    @Override
    public void startUp() {
    }

    /**
     * Send mail
     * @param smtphost host used to send mail
     * @param from @mail of sender
     * @param recipients array of recipient
     * @param subject text for subject
     * @param messageHtml body
     * @return true if mail successly send
     */
    private boolean sendMail(String smtphost, String from, String[] recipients, String subject, String messageHtml) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", smtphost);
            Session session = Session.getInstance(props);
            MimeMessage msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(from));
            InternetAddress[] recipAddress = new InternetAddress[recipients.length];
            for (int i = 0; i < recipients.length; i++) {
                recipAddress[i] = new InternetAddress(recipients[i]);
            }
            msg.setRecipients(Message.RecipientType.TO, recipAddress);
            msg.setSubject(subject);
            msg.setSentDate(new Date());
            msg.setContent(messageHtml, "text/html");
            Transport.send(msg);
            log.info("Success send message host:"+smtphost+" - from="+from+" - subject="+subject);
            return true;
        } catch (Exception e) {
            log.error("Error send message host:"+smtphost+" - from="+from+" - subject="+subject,e);
        }
        return false;
    }

    /**
     * Send the alert by mail
     * @param alert
     * @return
     */
    public boolean sendMailAlert(FunbridgeAlert alert) {
        if (alert != null) {
            if (FBConfiguration.getInstance().getIntValue("general.alert.mail.enable", 0) == 1) {
                String subject = "FUNBRIDGE ALERTE "+alert.level+" - ["+alert.category+"] - "+alert.summary;
                String htmlMessage = alert.buildMessageHtml();
                String smtphost = FBConfiguration.getInstance().getStringValue("general.alert.mail.smtphost", null);
                String from = FBConfiguration.getInstance().getStringValue("general.alert.mail.from", null);
                String recipients = FBConfiguration.getInstance().getStringValue("general.alert.mail.recipients", null);
                if (smtphost != null && from != null && recipients != null) {
                    return sendMail(smtphost, from, recipients.split(","), subject, htmlMessage);
                }
            } else {
                log.warn("Alert Mail is disable in config - alert="+alert);
            }
        } else {
            log.error("param alert is null");
        }
        return false;
    }

    /**
     * Create alert and commit it in mongo. Send it by mail.
     * @param category
     * @param level
     * @param summary
     * @param details
     * @param extraData
     * @return
     */
    public boolean addAlert(String category, String level, String summary, String details, Object extraData) {
        if (FBConfiguration.getInstance().getIntValue("general.alert.enable", 0) == 1) {
            // create the alert
            FunbridgeAlert alert = new FunbridgeAlert();
            alert.tsDate = System.currentTimeMillis();
            alert.category = category;
            alert.level = level;
            alert.summary = summary;
            alert.details = details;
            alert.extraData = extraData;

            // commit it in mongo ?mongo???
            try {
                long ts = System.currentTimeMillis();
                mongoTemplate.insert(alert);
                if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
                    log.error("Mongo insert too long ! ts="+(System.currentTimeMillis() - ts+" - alert="+alert));
                }
            }
            catch (Exception e) {
                log.error("Failed to persist alert="+alert, e);
            }
            // send it by mail
            sendMailAlert(alert);

            return true;
        }
        return false;
    }

}


