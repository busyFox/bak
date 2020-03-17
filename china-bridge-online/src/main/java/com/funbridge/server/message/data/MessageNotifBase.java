package com.funbridge.server.message.data;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.ws.event.WSMessage;
import com.funbridge.server.ws.event.WSMessageExtraField;
import com.funbridge.server.ws.event.WSMessageNotif;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Document(collection="message_notif")
public class MessageNotifBase {
	@Id
	public ObjectId ID;
	@Indexed
	public String msgType = "";
	public int category = 0;
	public int displayMask = 0;
	public String msgBodyFR = "";
	public String msgBodyEN = "";
	@Indexed
	public long dateMsg;
	@Indexed
	public long dateExpiration = 0;
	public String msgBodyNL = "";
	public String fieldName = "";
	public String fieldValue = "";

	public String templateName = "";
	public Map<String, String> templateParameters = new HashMap<>();
    private Date creationDateISO = null;

	public String richBodyFR;
	public String richBodyEN;
	public String titleFR;
	public String titleEN;
	public String titleIcon;
	public String titleBackgroundColor;
	public String titleColor;
	public String actionButtonTextFR;
	public String actionButtonTextEN;
	public String actionTextFR;
	public String actionTextEN;
	
	public MessageNotifBase() {}
	public MessageNotifBase(String type) {
		this.msgType = type;
        this.creationDateISO = new Date();
        this.dateMsg = System.currentTimeMillis();
	}
	
	/**
	 * Build from template
	 * @param template
	 */
	public MessageNotifBase(String type, String template) {
		this.msgType = type;
        this.creationDateISO = new Date();
        this.dateMsg = System.currentTimeMillis();
		if (template != null) {
			this.templateName = template;
		}
	}
	
	public String toString() {
		return "ID="+getIDStr()
				+" - category="+category
				+" - msgType="+msgType
				+" - templateName="+templateName
				+" - msgBodyFr="+msgBodyFR
				+" - msgBodyEN="+msgBodyEN
				+" - dateMsg="+Constantes.timestamp2StringDateHour(dateMsg)
				+" - dateExpiration="+Constantes.timestamp2StringDateHour(dateExpiration)
				+" - fieldName="+fieldName
				+" - fieldValue="+fieldValue
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

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }
	
	/**
	 * Return body message for this lang
	 * @param lang
	 * @return
	 */
	public String getMsgBodyForLang(String lang) {
		String value = "";
		if (lang != null) {
			if (lang.equals(Constantes.PLAYER_LANG_FR)) {
				value = msgBodyFR;
			}
			if (lang.equals(Constantes.PLAYER_LANG_NL)) {
				value = msgBodyNL;
			}
		}
		if (value == null || value.length() == 0) {
			value = msgBodyEN;
		}
		return value;
	}

	/**
	 * Return rich body message for this lang
	 * @param lang
	 * @return
	 */
	public String getRichBodyForLang(String lang) {
		String value = richBodyEN;
		if (lang != null) {
			if (lang.equals(Constantes.PLAYER_LANG_FR)) {
				value = richBodyFR;
			}
		}
		return value;
	}

	/**
	 * Return title text for this lang
	 * @param lang
	 * @return
	 */
	public String getTitleForLang(String lang) {
		String value = titleEN;
		if (lang != null) {
			if (lang.equals(Constantes.PLAYER_LANG_FR)) {
				value = titleFR;
			}
		}
		return value;
	}

	/**
	 * Return action button text for this lang
	 * @param lang
	 * @return
	 */
	public String getActionButtonTextForLang(String lang) {
		String value = actionButtonTextEN;
		if (lang != null) {
			if (lang.equals(Constantes.PLAYER_LANG_FR)) {
				value = actionButtonTextFR;
			}
		}
		return value;
	}

	/**
	 * Return action text for this lang
	 * @param lang
	 * @return
	 */
	public String getActionTextForLang(String lang) {
		String value = actionTextEN;
		if (lang != null) {
			if (lang.equals(Constantes.PLAYER_LANG_FR)) {
				value = actionTextFR;
			}
		}
		return value;
	}
	
	/**
	 * Transform to WSMessage - some field can not be set ... to be done in child classes
	 * @param p
	 * @return
	 */
	public WSMessage toWSMessage(Player p) {
		WSMessageNotif wmsg = new WSMessageNotif();
		wmsg.ID = buildWSMessageID(msgType,ID.toString());
		wmsg.category = category;
		wmsg.displayMask = displayMask;
		wmsg.dateExpiration = dateExpiration;
		wmsg.dateReceive = dateMsg;
		wmsg.senderID = Constantes.PLAYER_FUNBRIDGE_ID;
		wmsg.senderAvatar = false;
		wmsg.senderPseudo = Constantes.PLAYER_FUNBRIDGE_PSEUDO;
		wmsg.title = getTitleForLang(p.getDisplayLang());
		wmsg.titleIcon = titleIcon;
		wmsg.titleBackgroundColor = titleBackgroundColor;
		wmsg.titleColor = titleColor;
		wmsg.actionButtonText = getActionButtonTextForLang(p.getDisplayLang());

		// BODY TEXT
		wmsg.body = getMsgBodyForLang(p.getDisplayLang());
		wmsg.richBody = getRichBodyForLang(p.getDisplayLang());
		// replace some variables : PSEUDO, SERIE
		Map<String, String> mapVarVal = new HashMap<String, String>();
        mapVarVal.put(MessageNotifMgr.VARIABLE_PLAYER_PSEUDO, p.getNickname());
        if (MessageNotifMgr.textContainVariableSerie(wmsg.body)) {
			mapVarVal.putAll(Constantes.MESSAGE_SERIE_PARAM);
		}
        if (MessageNotifMgr.textContainVariableTeamDivision(wmsg.body)) {
            mapVarVal.putAll(Constantes.MESSAGE_TEAM_DIVISION_PARAM);
        }
		wmsg.body = MessageNotifMgr.replaceTextVariables(wmsg.body, mapVarVal);
		// FIELDS
		if (fieldName != null && fieldName.length() > 0 &&
			fieldValue != null && fieldValue.length() > 0) {
			wmsg.extraFields = new ArrayList<WSMessageExtraField>();
			String[] tempFieldName = fieldName.split(Constantes.SEPARATOR_VALUE);
			String[] tempFieldValue = fieldValue.split(Constantes.SEPARATOR_VALUE);
			if (tempFieldName.length == tempFieldValue.length) {
				for (int i = 0; i < tempFieldName.length; i++) {
					WSMessageExtraField temp = new WSMessageExtraField();
					temp.name = tempFieldName[i];
					temp.value = MessageNotifMgr.replaceTextVariables(tempFieldValue[i], mapVarVal);
					wmsg.extraFields.add(temp);
				}
			}
		}
		return wmsg;
	}
	
	/**
	 * Add extra field to message (couple name & value)
	 * @param name
	 * @param value
	 */
	public void addExtraField(String name, String value) {
		if (fieldName.length() > 0) {
			fieldName += Constantes.SEPARATOR_VALUE;
		}
		fieldName+=name;
		if (fieldValue.length() > 0) {
			fieldValue += Constantes.SEPARATOR_VALUE;
		}
		fieldValue+=value;
	}

    public void addMapExtraField(Map<String, String> mapData) {
        if (mapData != null) {
            for (Map.Entry<String, String> e : mapData.entrySet()) {
                addExtraField(e.getKey(), e.getValue());
            }
        }
    }
	
	/**
	 * Return the WSMessageID : build with the msg type
	 * @param msgType
	 * @param msgID
	 * @return
	 */
	public static String buildWSMessageID(String msgType, String msgID) {
		return msgType+"-"+msgID;
	}
	
	/**
     * check if it is expired for a date (timestamp)
     * @param tsDate
     * @return
     */
    public boolean isExpiredForDate(long tsDate) {
        return tsDate > dateExpiration;
    }

    public void setDateMsg(long value) {
        this.dateMsg = value;
        this.creationDateISO = new Date(value);
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void addTemplateParameters(Map<String, String> templateParameters) {
    	if (templateParameters != null) {
			this.templateParameters.putAll(templateParameters);
		}
	}
}
