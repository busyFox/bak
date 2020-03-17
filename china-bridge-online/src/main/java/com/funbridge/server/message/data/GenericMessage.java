package com.funbridge.server.message.data;

import com.funbridge.server.common.Constantes;
import com.gotogames.common.tools.StringTools;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bplays on 04/11/16.
 */
public abstract class GenericMessage {
    protected String chatroomID;
    protected long authorID;
    protected String body;
    protected String mediaID;
    protected String mediaSize;
    protected String quotedMessageID;
    protected long date;
    protected String type;
    protected Date creationDateISO;
    protected String templateName = "";
    protected Map<String, String> templateParameters = new HashMap<>();

    public String toString() {
        return "ID="+getIDStr()+" - chatroomID="+chatroomID+" - authorID="+authorID+" - date="+Constantes.timestamp2StringDateHour(date)+" - type="+type+" - body="+ StringTools.truncate(body, 10, "...")+" - templateName="+templateName+" - templateParameters size="+templateParameters.size();
    }

    public abstract String getIDStr();

    public String getChatroomID() {
        return chatroomID;
    }

    public void setChatroomID(String chatroomID) {
        this.chatroomID = chatroomID;
    }

    public long getAuthorID() {
        return authorID;
    }

    public void setAuthorID(long authorID) {
        this.authorID = authorID;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getMediaID() {
        return mediaID;
    }

    public void setMediaID(String mediaID) {
        this.mediaID = mediaID;
    }

    public String getMediaSize() {
        return mediaSize;
    }

    public void setMediaSize(String mediaSize) {
        this.mediaSize = mediaSize;
    }

    public String getQuotedMessageID() {
        return quotedMessageID;
    }

    public void setQuotedMessageID(String quotedMessageID) {
        this.quotedMessageID = quotedMessageID;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getCreationDateISO() {
        return creationDateISO;
    }

    public void setCreationDateISO(Date creationDateISO) {
        this.creationDateISO = creationDateISO;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public Map<String, String> getTemplateParameters() {
        return templateParameters;
    }

    public void setTemplateParameters(Map<String, String> templateParameters) {
        this.templateParameters = templateParameters;
    }

}
