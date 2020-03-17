package com.funbridge.server.ws.notification;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.message.MessageNotifMgr;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Created by pserent on 06/11/2015.
 */
@JsonAutoDetect(fieldVisibility= JsonAutoDetect.Visibility.ANY, getterVisibility= JsonAutoDetect.Visibility.NONE, setterVisibility= JsonAutoDetect.Visibility.NONE, isGetterVisibility= JsonAutoDetect.Visibility.NONE)
public class WSNotifData {
    public int category;
    public int displayMode = MessageNotifMgr.MESSAGE_DISPLAY_NORMAL;
    public String textTemplate;
    public Map<String, String> mapTextTemplateParameters; // Parameters name => value
    public String textFR;
    public String textEN;
    public String paramOpenURL;
    public boolean openUrlLeaveApp = false;
    public boolean openCategoryPage = false;
    public String expirationDate; // format dd/MM/yyyy - HH:mm:ss
    public String notifDate = null; // format dd/MM/yyyy - HH:mm:ss
    public int openCategory = 0;
    public String openSettings;
    public String openFriends;
    public int openProfile = 0;
    public String titleFR;
    public String titleEN;
    public String titleIcon;
    public String titleBackgroundColor;
    public String titleColor;
    public String richBodyFR;
    public String richBodyEN;
    public String actionButtonTextFR;
    public String actionButtonTextEN;
    public String actionTextFR;
    public String actionTextEN;

    public String toString() {
        return "category="+category
                +" - displayMode="+displayMode
                +" - openCategoryPage="+openCategoryPage
                +" - textTemplate="+ textTemplate
                +" - mapTextTemplateParameters size="+(mapTextTemplateParameters !=null? mapTextTemplateParameters.size():0)
                +" - textFR="+textFR
                +" - textEN="+textEN
                +" - paramOpenURL="+paramOpenURL
                +" - expirationDate="+expirationDate
                +" - openCategory="+openCategory
                +" - openSettings="+openSettings
                +" - openProfile="+openProfile
                +" - titleFR="+titleFR
                +" - titleEN="+titleEN
                +" - titleIcon="+titleIcon
                +" - titleBackgroundColor="+titleBackgroundColor
                +" - titleColor="+titleColor
                +" - actionButtonTextFR="+actionButtonTextFR
                +" - actionButtonTextEN="+actionButtonTextEN
                +" - actionTextFR="+actionTextFR
                +" - actionTextEN="+actionTextEN;
    }
    public boolean isValid() {
        if (!MessageNotifMgr.isMessageCategoryValid(category)) {
            return false;
        }
        if (textTemplate == null || textTemplate.length() == 0) {
            if (textFR == null || textFR.length() == 0) {
                return false;
            }
            if (textEN == null || textEN.length() == 0) {
                return false;
            }
            if (displayMode == MessageNotifMgr.MESSAGE_DISPLAY_DIALOG_BOX) {
                if (titleFR == null || titleFR.length() == 0) {
                    return false;
                }
                if (titleEN == null || titleEN.length() == 0) {
                    return false;
                }
            }
        }
        try {
            Constantes.stringDateHour2Timestamp(expirationDate);
        } catch (Exception e) {
            return false;
        }

        return getExpirationDateTS() >= System.currentTimeMillis();
    }

    public long getNotifDateTS() {
        if (notifDate != null && notifDate.length() > 0) {
            try {
                return Constantes.stringDateHour2Timestamp(notifDate);
            } catch (Exception e) {}
        }
        return 0;
    }

    public long getExpirationDateTS() {
        try {
            return Constantes.stringDateHour2Timestamp(expirationDate);
        } catch (Exception e) {}
        return 0;
    }

    public String getParamFieldName() {
        String value = null;
        if (openCategoryPage && category != MessageNotifMgr.MESSAGE_CATEGORY_GENERAL) {
            value = MessageNotifMgr.NOTIF_PARAM_ACTION;
        }
        if (openCategory != 0) {
            value = MessageNotifMgr.NOTIF_PARAM_ACTION + Constantes.SEPARATOR_VALUE +MessageNotifMgr.NOTIF_PARAM_CATEGORY_ID;
        }
        if (StringUtils.isNotBlank(openSettings)) {
            value = MessageNotifMgr.NOTIF_PARAM_ACTION + Constantes.SEPARATOR_VALUE +MessageNotifMgr.NOTIF_PARAM_PAGE;
        }
        if (StringUtils.isNotBlank(openFriends)) {
            value = MessageNotifMgr.NOTIF_PARAM_ACTION + Constantes.SEPARATOR_VALUE +MessageNotifMgr.NOTIF_PARAM_PAGE;
        }
        if (openProfile != 0) {
            value = MessageNotifMgr.NOTIF_PARAM_ACTION + Constantes.SEPARATOR_VALUE +MessageNotifMgr.NOTIF_PARAM_PLAYER_ID;
        }
        if (paramOpenURL != null && paramOpenURL.length() > 0) {
            if (value != null && value.length() > 0) {
                value+=";";
            } else {
                value="";
            }
            value += MessageNotifMgr.NOTIF_PARAM_ACTION+";"+MessageNotifMgr.NOTIF_PARAM_URL_FULL;
            if (openUrlLeaveApp) {
                value += ";LEAVE_APP";
            }
        }
        return value;
    }

    public String getParamFieldValue() {
        String value = null;
        if (openCategoryPage && category != MessageNotifMgr.MESSAGE_CATEGORY_GENERAL) {
            value = MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY_PAGE;
        }
        if (openCategory != 0) {
            value = MessageNotifMgr.NOTIF_ACTION_OPEN_CATEGORY + Constantes.SEPARATOR_VALUE +openCategory;
        }
        if (StringUtils.isNotBlank(openSettings)) {
            value = MessageNotifMgr.NOTIF_ACTION_OPEN_SETTINGS+ Constantes.SEPARATOR_VALUE +openSettings;
        }
        if (StringUtils.isNotBlank(openFriends)) {
            value = MessageNotifMgr.NOTIF_ACTION_OPEN_FRIENDS + Constantes.SEPARATOR_VALUE +openFriends;
        }
        if (openProfile != 0) {
            value = MessageNotifMgr.NOTIF_ACTION_OPEN_PROFILE + Constantes.SEPARATOR_VALUE +openProfile;
        }
        if (paramOpenURL != null && paramOpenURL.length() > 0) {
            if (value != null && value.length() > 0) {
                value+=";";
            } else {
                value="";
            }
            value += MessageNotifMgr.NOTIF_ACTION_OPEN_URL+";"+paramOpenURL;
            if (openUrlLeaveApp) {
                value += ";true";
            }
        }
        return value;
    }

    public void transformText() {
        if (textFR != null && textFR.contains("\r\n")) {
            textFR = textFR.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (textEN != null && textEN.contains("\r\n")) {
            textEN = textEN.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (richBodyFR != null && richBodyFR.contains("\r\n")) {
            richBodyFR = richBodyFR.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (richBodyEN != null && richBodyEN.contains("\r\n")) {
            richBodyEN = richBodyEN.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (titleFR != null && titleFR.contains("\r\n")) {
            titleFR = titleFR.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (titleEN != null && titleEN.contains("\r\n")) {
            titleEN = titleEN.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (actionTextFR != null && actionTextFR.contains("\r\n")) {
            actionTextFR = actionTextFR.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (actionTextEN != null && actionTextEN.contains("\r\n")) {
            actionTextEN = actionTextEN.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (actionButtonTextFR != null && actionButtonTextFR.contains("\r\n")) {
            actionButtonTextFR = actionButtonTextFR.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
        if (actionButtonTextEN != null && actionButtonTextEN.contains("\r\n")) {
            actionButtonTextEN = actionButtonTextEN.replace("\r\n", "{"+MessageNotifMgr.VARIABLE_LINE_BREAK+"}");
        }
    }
}
