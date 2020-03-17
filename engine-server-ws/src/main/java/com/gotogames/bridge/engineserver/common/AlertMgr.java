package com.gotogames.bridge.engineserver.common;

import com.gotogames.bridge.engineserver.request.data.QueueData;
import com.gotogames.bridge.engineserver.user.UserVirtualEngine;
import com.gotogames.common.bridge.BridgeGame;
import com.gotogames.common.bridge.PBNConvertion;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pserent on 19/05/2015.
 */
@Component(value="alertMgr")
@Scope(value="singleton")
public class AlertMgr {
    private Logger log = LogManager.getLogger(this.getClass());
    @Resource(name = "mongoTemplate")
    private MongoTemplate mongoTemplate;
    private Scheduler scheduler;
    public boolean reportTaskRunning = false;
    public boolean checkTaskRunning = false;
    public long tsLastRunTaskCheck = System.currentTimeMillis();

    public Logger getLog() {
        return log;
    }

    @PostConstruct
    public void init() {
        log.info("init");
    }

    public void startup() {
        log.info("startup");
        // report timer
        String reportTime = EngineConfiguration.getInstance().getStringValue("general.alert.report.time", "07:00");
        int hour = 7, minute=0;
        String[] temp = reportTime.split(":");
        try {
            if (temp.length == 2) {
                hour = Integer.parseInt(temp[0]);
                minute = Integer.parseInt(temp[1]);
            }
        } catch (Exception e) {
            log.error("Error to parse report time="+reportTime);
        }
        if (hour < 0 || hour > 23) {hour=7;}
        if (minute < 0 || minute > 59) {minute=0;}
        try {
            SchedulerFactory schedulerFactory = new StdSchedulerFactory();
            scheduler = schedulerFactory.getScheduler();

            // schedule step1 - triger every day at 00:00:01
            JobDetail jobReportTask = JobBuilder.newJob(AlertReportTask.class).withIdentity("reportTask", "EngineAlert").build();
            CronTrigger triggerReportTask = TriggerBuilder.newTrigger().withIdentity("triggerReportTask", "EngineAlert").withSchedule(CronScheduleBuilder.cronSchedule("0 "+minute+" "+hour+" * * ?")).build();
            Date dateNextJobReportTask = scheduler.scheduleJob(jobReportTask, triggerReportTask);
            log.warn("Sheduled for jobReportTask=" + jobReportTask.getKey() + " run at="+dateNextJobReportTask+" - cron expression=" + triggerReportTask.getCronExpression() + " - next fire=" + triggerReportTask.getNextFireTime());

            // schedule check alert - triger every 10min
            JobDetail jobCheckAlert = JobBuilder.newJob(AlertCheckTask.class).withIdentity("checkAlert", "EngineAlert").build();
            CronTrigger triggerCheckAlert = TriggerBuilder.newTrigger().withIdentity("triggerCheckAlert", "EngineAlert").withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?")).build();
            Date dateNextJobCheckAlert = scheduler.scheduleJob(jobCheckAlert, triggerCheckAlert);
            log.warn("Sheduled for jobCheckAlert=" + jobCheckAlert.getKey() + " run at="+dateNextJobCheckAlert+" - cron expression=" + triggerCheckAlert.getCronExpression() + " - next fire=" + triggerCheckAlert.getNextFireTime());
        } catch (Exception e) {
            log.error("Exception to init scheduler", e);
        }
    }

    @PreDestroy
    public void destroy() {
        log.info("End destroy");
    }

    /**
     * Return next TS for next job on check alert
     * @return
     */
    public long getDateNextJobCheckAlert() {
        try {
            Trigger trigger = scheduler.getTrigger(TriggerKey.triggerKey("triggerCheckAlert", "EngineAlert"));
            if (trigger != null) {
                return trigger.getNextFireTime().getTime();
            }
        } catch (Exception e) {
            log.error("Failed to get trigger", e);
        }
        return 0;
    }

    /**
     * Build the report data and replace all variable define in the map with the value associated
     * @param varValReplace
     * @return
     */
    private String buildMailData(String mailType, Map<String, String> varValReplace) {
        String strReport = null;
        String msgFile = EngineConfiguration.getInstance().getStringResolveEnvVariableValue("general.alert."+mailType+".htmlFile", null);
        if (msgFile != null) {
            try {
                strReport = FileUtils.readFileToString(new File(msgFile));
                Pattern pattern;
                pattern = Pattern.compile("\\{(.+?)\\}");
                Matcher matcher = pattern.matcher(strReport);
                StringBuffer sb = new StringBuffer();
                while (matcher.find()) {
                    String val = varValReplace.get(matcher.group(1));
                    if (val != null) {
                        matcher.appendReplacement(sb, "");
                        sb.append(val);
                    }
                }
                matcher.appendTail(sb);
                strReport = sb.toString();
            } catch (Exception e) {
                log.error("Exception to build message !", e);
            }
        }
        return strReport;
    }

    /**
     * Send mail for check alert. Threshold is exceeded !
     * @param ts1
     * @param ts2
     * @param alerts
     * @return
     */
    public int processCheckAlert() {
        long tsCurrent = System.currentTimeMillis();
        List<AlertDataGroupMapping> alerts = countAlertBetweenDateGroupByEngine(tsLastRunTaskCheck, tsCurrent);
        if (log.isDebugEnabled()) {
            log.debug("Check task - ts1="+Constantes.timestamp2StringDateHour(tsLastRunTaskCheck)+" - ts2="+Constantes.timestamp2StringDateHour(tsCurrent)+" - nb alerts="+alerts.size());
        }
        if (alerts != null && alerts.size() >= getThresholdCheckAlert()) {
            log.warn("Check task Threshold exceeded (" + getThresholdCheckAlert() + ") ! - ts1=" + Constantes.timestamp2StringDateHour(tsLastRunTaskCheck) + " - ts2=" + Constantes.timestamp2StringDateHour(tsCurrent) + " - nb alerts=" + alerts.size());

            Map<String, String> mapData = new HashMap<>();
            mapData.put("DATE1", Constantes.timestamp2StringDateHour(tsLastRunTaskCheck));
            mapData.put("DATE2", Constantes.timestamp2StringDateHour(tsCurrent));
            mapData.put("NB_ALERT", "" + alerts.size());
            mapData.put("NB_ALERT_TOTAL", "" + countAllAlert());
            String strTAB_ENGINE_VERSION_NB_ALERTES = "";
            for (AlertDataGroupMapping e : alerts) {
                strTAB_ENGINE_VERSION_NB_ALERTES += "<tr><td>" + e._id + "</td><td>" + e.nb + "</td></tr>";
            }
            mapData.put("TAB_ENGINE_VERSION_NB_ALERTES", strTAB_ENGINE_VERSION_NB_ALERTES);
            String reportData = buildMailData("checkAlert", mapData);
            if (reportData != null) {
                try {
                    String smtphost = EngineConfiguration.getInstance().getStringValue("general.alert.mail.smtphost", null);
                    String from = EngineConfiguration.getInstance().getStringValue("general.alert.mail.from", null);
                    String recipients = EngineConfiguration.getInstance().getStringResolveEnvVariableValue("general.alert.mail.recipients", null);
                    String mailSubject = EngineConfiguration.getInstance().getStringValue("general.alert.checkAlert.subject", null);
                    mailSubject += " - " + alerts.size() + " alerte(s)";
                    boolean bMsgSend = sendMail(smtphost, from, recipients.split(","), mailSubject, reportData);
                    log.info("Message send=" + bMsgSend);
                } catch (Exception e) {
                    log.error("Exception to prepare message !", e);
                }
            }
        }
        tsLastRunTaskCheck = tsCurrent;
        return alerts!=null?alerts.size():-1;
    }

    /**
     * Send mail. Replace in htmlfile each variable with value present in map.
     * @param date format dd/MM/yyyy
     */
    public boolean sendMailReport(String date) {
        if (EngineConfiguration.getInstance().getIntValue("general.alert.report.enable", 1) == 1) {
            try {
                long ts1 = Constantes.stringDate2Timestamp(date);
                long ts2 = ts1 + (24 * 60 * 60 * 1000);
                long nbAlertsDay = countAlertBetweenDate(ts1, ts2);
                long nbAlerts = countAllAlert();
                Map<String, String> mapData = new HashMap<>();
                mapData.put("DATE", date);
                mapData.put("NB_ALERTS_DAY", "" + nbAlertsDay);
                mapData.put("NB_TOTAL_ALERTS", "" + nbAlerts);
                List<AlertData> alerts = listAlert(0, 0, 1);
                if (alerts != null && alerts.size() > 0) {
                    mapData.put("DATE_LAST_ALERT", Constantes.timestamp2StringDateHour(alerts.get(0).date));
                } else {
                    mapData.put("DATE_LAST_ALERT", "-");
                }
                List<AlertDataGroupMapping> alertDataGroupMappingList = countAlertBetweenDateGroupByEngine(ts1, ts2);
                String strTAB_ENGINE_VERSION_NB_ALERTES = "";
                if (alertDataGroupMappingList != null && alertDataGroupMappingList.size() > 0) {
                    for (AlertDataGroupMapping e : alertDataGroupMappingList) {
                        strTAB_ENGINE_VERSION_NB_ALERTES += "<tr><td>"+e._id+"</td><td>"+e.nb+"</td></tr>";
                    }
                }
                mapData.put("TAB_ENGINE_VERSION_NB_ALERTES", strTAB_ENGINE_VERSION_NB_ALERTES);
                String reportData = buildMailData("report", mapData);
                if (reportData != null) {
                    try {
                        String smtphost = EngineConfiguration.getInstance().getStringValue("general.alert.mail.smtphost", null);
                        String from = EngineConfiguration.getInstance().getStringValue("general.alert.mail.from", null);
                        String recipients = EngineConfiguration.getInstance().getStringResolveEnvVariableValue("general.alert.mail.recipients", null);
                        String mailSubject = EngineConfiguration.getInstance().getStringValue("general.alert.report.subject", null);
                        mailSubject += " - " + nbAlertsDay + " alerte(s)";
                        boolean bMsgSend = sendMail(smtphost, from, recipients.split(","), mailSubject, reportData);
                        log.info("Message send=" + bMsgSend);
                        return bMsgSend;
                    } catch (Exception e) {
                        log.error("Exception to prepare message !", e);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to sendMailReport for date=" + date, e);
            }
        } else {
            log.warn("sendMailReport is disabled");
        }
        return false;
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
     * Send alert blocage with this data
     * @param message
     * @param data
     * @return
     */
    public boolean sendAlertBlocage(String message, QueueData data) {
        if (EngineConfiguration.getInstance().getIntValue("general.alert.mail.enable", 1) == 1) {
            try {
                String subject = "[ENGINE ALERTE] - "+EngineConfiguration.getInstance().getStringValue("general.alert.mail.subject", "PROD Blocage")+" - " + Constantes.timestamp2StringDateHour(data.timestamp);
                String htmlMessage = "Message=" + message + "<br/><br/>";
                htmlMessage += "Date=" + Constantes.timestamp2StringDateHour(data.timestamp) + "<br/>";
                htmlMessage += "Deal=" + data.getDeal() + "<br/>";
                htmlMessage += "Game=" + data.getGame() + "<br/>";
                htmlMessage += "Options=" + data.getOptions() + "<br/>";
                htmlMessage += "Conventions=" + data.getConventions() + "<br/>";
                htmlMessage += "RequestType=" + data.getRequestType() + "(BID:"+Constantes.REQUEST_TYPE_BID+", CARD:"+Constantes.REQUEST_TYPE_CARD+", BIDINFO:"+Constantes.REQUEST_TYPE_BID_INFO+", PAR:"+Constantes.REQUEST_TYPE_PAR+")<br/>";
                htmlMessage += "Engine Version=" + data.getEngineVersion() + "<br/>";
                htmlMessage += "Result=" + data.resultValue + "<br/>";
                htmlMessage += "Client process=" + data.engineComputingToString() + "<br/>";
                htmlMessage += "Request=" + data.getRequest() + "<br/>";
                String bids = "";
                String cards = "";
                if (data.getGame().indexOf("PAPAPA") > 0) {
                    bids = data.getGame().substring(0, data.getGame().indexOf("PAPAPA") + 6);
                    cards = data.getGame().substring(data.getGame().indexOf("PAPAPA") + 6);
                } else {
                    bids = data.getGame();
                }
                BridgeGame bg = BridgeGame.create(data.getDeal().substring(2), data.getDeal().charAt(0), data.getDeal().charAt(1), bids, cards);
                Map<String, String> metaData = new HashMap<>();
                metaData.put("Player_profile", data.getConventions());
                metaData.put("engineVersion", "" + data.getEngineVersion());
                String pbn = PBNConvertion.gameToPBN(bg, metaData, "<br>");
                htmlMessage += "<br/>PBN=<br/>" + pbn + "<br/>";
                String smtphost = EngineConfiguration.getInstance().getStringValue("general.alert.mail.smtphost", null);
                String from = EngineConfiguration.getInstance().getStringValue("general.alert.mail.from", null);
                String recipients = EngineConfiguration.getInstance().getStringResolveEnvVariableValue("general.alert.mail.recipients", null);
                log.error("Send Alerte - smtphost=" + smtphost + " - recipients=" + recipients + " - htmlMessage=" + htmlMessage);
                if (smtphost != null && from != null && recipients != null) {
                    return sendMail(smtphost, from, recipients.split(","), subject, htmlMessage);
                }
            } catch (Exception e) {
                log.error("Failed to send message="+message+" - data=" + data, e);
            }
        } else {
            log.warn("Alert Mail is disable in config - message=" + message + " - data=" + data);
        }
        return false;
    }

    /**
     * Send alert blocage with this data
     * @param message
     * @param data
     * @return
     */
    public boolean sendAlertEngineNoResult(List<UserVirtualEngine> listEngine) {
        if (EngineConfiguration.getInstance().getIntValue("general.alert.mailEngineNoResult.enable", 1) == 1 && listEngine != null && listEngine.size() > 0) {
            String subject = "[ENGINE ALERTE NO RESULT] - "+EngineConfiguration.getInstance().getStringValue("general.alert.mailEngineNoResult.subject", "PROD EngineNoResult")+" - " + listEngine.size()+" engine(s)";
            String htmlMessage = "Nb engines with no result since a long delay : " + listEngine.size()+ "<br/><br/>";
            int idxEngine = 1;
            for (UserVirtualEngine e : listEngine) {
                htmlMessage += "<b>element["+idxEngine+"]</b> : "+e.toString()+"<br/>";
                idxEngine++;
            }
            try {
                String smtphost = EngineConfiguration.getInstance().getStringValue("general.alert.mailEngineNoResult.smtphost", null);
                String from = EngineConfiguration.getInstance().getStringValue("general.alert.mailEngineNoResult.from", null);
                String recipients = EngineConfiguration.getInstance().getStringResolveEnvVariableValue("general.alert.mailEngineNoResult.recipients", null);
                log.error("Send Alerte EngineNoResult - smtphost=" + smtphost + " - recipients=" + recipients + " - htmlMessage=" + htmlMessage);
                if (smtphost != null && from != null && recipients != null) {
                    return sendMail(smtphost, from, recipients.split(","), subject, htmlMessage);
                }
            } catch (Exception e) {
                log.error("Failed to send htmlMessage="+htmlMessage, e);
            }
        } else {
            log.warn("Alert EngineNoResult is disable in config");
        }
        return false;
    }

    public boolean isCheckTaskEnable() {
        return EngineConfiguration.getInstance().getIntValue("general.alert.checkTaskEnable", 1) == 1;
    }

    public int getThresholdCheckAlert() {
        return EngineConfiguration.getInstance().getIntValue("general.alert.checkTaskThreshold", 10);
    }

    /**
     * Save alert in BDD
     * @param message
     * @param engineLogin
     * @param data
     * @return
     */
    public AlertData saveAlert(String message, String engineLogin, QueueData data) {
        if (data != null) {
            try {
                AlertData alertData = new AlertData();
                alertData.date = data.timestamp;
                alertData.deal = data.getDeal();
                alertData.game = data.getGame();
                alertData.options = data.getOptions();
                alertData.conventions = data.getConventions();
                alertData.requestType = data.getRequestType();
                alertData.engineVersion = data.getEngineVersion();
                alertData.result = data.resultValue;
                alertData.engineLogin = engineLogin;
                alertData.request = data.getRequest();
                alertData.message = message;
                String bids = "";
                String cards = "";
                if (data.getGame() != null && data.getGame().indexOf("PAPAPA") > 0) {
                    bids = data.getGame().substring(0, data.getGame().indexOf("PAPAPA") + 6);
                    cards = data.getGame().substring(data.getGame().indexOf("PAPAPA") + 6);
                } else {
                    bids = data.getGame();
                }
                BridgeGame bg = BridgeGame.create(data.getDeal().substring(2), data.getDeal().charAt(0), data.getDeal().charAt(1), bids, cards);
                Map<String, String> metaData = new HashMap<>();
                metaData.put("Player_profile", data.getConventions());
                metaData.put("engineVersion", "" + data.getEngineVersion());
                String pbn = PBNConvertion.gameToPBN(bg, metaData, "<br>");
                alertData.pbn = pbn;
                mongoTemplate.insert(alertData);
                log.warn("Save Alerte - alerte="+alertData+" - pbn="+alertData.pbn);
                return alertData;
            } catch (Exception e) {
                log.error("saveAlert failed message="+message+" - engineLogin="+engineLogin+" - data="+data, e);
            }
        }
        return null;
    }

    /**
     * List alerts
     * @param engineVersion
     * @param offset
     * @param nbMax
     * @return
     */
    public List<AlertData> listAlert(int engineVersion, int offset, int nbMax) {
        Query q = new Query();
        if (engineVersion > 0) {
            q.addCriteria(Criteria.where("engineVersion").is(engineVersion));
        }
        q.with(new Sort(Sort.Direction.DESC, "date"));
        if (offset > 0) {
            q.skip(offset);
        }
        if (nbMax > 0) {
            q.limit(nbMax);
        }
        return mongoTemplate.find(q, AlertData.class);
    }

    /**
     * Count alerts between 2 dates. date1 > date2.
     * @param date1
     * @param date2
     * @return
     */
    public long countAlertBetweenDate(long date1, long date2) {
        Query q = new Query();
        q.addCriteria(Criteria.where("date").gt(date1).andOperator(Criteria.where("date").lt(date2)));
        return mongoTemplate.count(q, AlertData.class);
    }

    /**
     * Count alerts between 2 dates group by engine version. date1 > date2.
     * @param date1
     * @param date2
     * @return
     */
    public List<AlertDataGroupMapping> countAlertBetweenDateGroupByEngine(long date1, long date2) {
        TypedAggregation<AlertData> aggAlert = Aggregation.newAggregation(AlertData.class,
                Aggregation.match(Criteria.where("date").gt(date1).andOperator(Criteria.where("date").lt(date2))),
                Aggregation.group("engineVersion").count().as("nb"));
        AggregationResults<AlertDataGroupMapping> results = mongoTemplate.aggregate(aggAlert, AlertDataGroupMapping.class);
        List<AlertDataGroupMapping> res = new ArrayList<>();
        res.addAll(results.getMappedResults());
        Collections.sort(res, new Comparator<AlertDataGroupMapping>() {
            @Override
            public int compare(AlertDataGroupMapping o1, AlertDataGroupMapping o2) {
                return -Integer.compare(o1._id, o2._id);
            }
        });
        return res;
    }

    /**
     * Count alerts group by engine version.
     * @return
     */
    public List<AlertDataGroupMapping> countAlertGroupByEngine() {
        TypedAggregation<AlertData> aggAlert = Aggregation.newAggregation(AlertData.class,
                Aggregation.group("engineVersion").count().as("nb"));
        AggregationResults<AlertDataGroupMapping> results = mongoTemplate.aggregate(aggAlert, AlertDataGroupMapping.class);
        List<AlertDataGroupMapping> res = new ArrayList<>();
        res.addAll(results.getMappedResults());
        Collections.sort(res, new Comparator<AlertDataGroupMapping>() {
            @Override
            public int compare(AlertDataGroupMapping o1, AlertDataGroupMapping o2) {
                return -Integer.compare(o1._id, o2._id);
            }
        });
        return res;
    }

    /**
     * Count all alerts
     * @return
     */
    public long countAllAlert() {
        return mongoTemplate.count(new Query(), AlertData.class);
    }

    public void removeAlerts(List<String> listID) {
        mongoTemplate.remove(Query.query(Criteria.where("_id").in(listID)), AlertData.class);
    }

    public void removeAlertsForEngine(List<Integer> listEngineVersion) {
        mongoTemplate.remove(Query.query(Criteria.where("engineVersion").in(listEngineVersion)), AlertData.class);
    }
}
