package com.funbridge.server.alert;

import com.funbridge.server.common.Constantes;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 12/03/2014.
 * ??
 */
@Document(collection="alert")
public class FunbridgeAlert {
    @Id
    public ObjectId ID;
    @Indexed
    public long tsDate;
    @Indexed
    public String category;
    @Indexed
    public String level;
    public String summary;
    public String details;
    public Object extraData;

    @Override
    public String toString() {
        return "ID="+ID+" - date="+ Constantes.timestamp2StringDateHour(tsDate)+" - category="+category+" - level="+level+" - summary="+summary+" - details="+details+" - extraData="+extraData;
    }

    /**
     * Build html view for this alert
     * ??????html??
     * @return
     */
    public String buildMessageHtml() {
        String htmlMessage = "";
        htmlMessage += "ID="+ID+"<br/>";
        htmlMessage += "Date="+Constantes.timestamp2StringDateHour(tsDate)+"<br/>";
        htmlMessage += "<b>Category="+category+"</b><br/>";
        htmlMessage += "<b>Level="+level+"</b><br/>";
        htmlMessage += "<b>Summary="+summary+"</b><br/>";
        htmlMessage += "details=<br/>"+details+"<br/>";
        htmlMessage += "extraData="+(extraData!=null?"data present":"no data");
        return htmlMessage;
    }
}
