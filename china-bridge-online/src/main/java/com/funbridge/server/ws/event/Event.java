package com.funbridge.server.ws.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.common.Constantes;
import com.gotogames.common.tools.StringTools;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;

@XmlRootElement(name = "event")
public class Event implements Cloneable {
    public long timestamp;
    public long senderID;
    public long receiverID;
    public ArrayList<EventField> fields = new ArrayList<EventField>();

    public Event() {
    }

    @JsonIgnore
    public String toString() {
        return "{senderID=" + senderID + " - receiverID=" + receiverID + " - timestamp=" + timestamp + " - fields=" + StringTools.listToString(fields) + "}";
    }

    @SuppressWarnings("unchecked")
    public Object clone() {
        Event evt = null;
        try {
            evt = (Event) super.clone();
            evt.fields = (ArrayList<EventField>) this.fields.clone();
            for (int i = 0; i < fields.size(); i++) {
                evt.fields.set(i, (EventField) evt.fields.get(i).clone());
            }
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return evt;
    }

    @JsonIgnore
    public void addField(EventField field) {
        fields.add(field);
    }

    @JsonIgnore
    public void addFieldCategory(String value) {
        fields.add(new EventField(Constantes.EVENT_FIELD_CATEGORY, value, null));
    }

    @JsonIgnore
    public void addFieldCategory(String value, String dataAdditionnal) {
        fields.add(new EventField(Constantes.EVENT_FIELD_CATEGORY, value, dataAdditionnal));
    }

    @JsonIgnore
    public void addFieldType(String value, String dataAdditionnal) {
        fields.add(new EventField(Constantes.EVENT_FIELD_TYPE, value, dataAdditionnal));
    }

    @JsonIgnore
    public String getCategory() {
        for (EventField f : fields) {
            if (f.name.equals(Constantes.EVENT_FIELD_CATEGORY)) {
                return f.value;
            }
        }
        return null;
    }
}
