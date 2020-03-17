package com.funbridge.server.ws.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.FBWSResponse;

import javax.ws.rs.*;
import java.util.ArrayList;
import java.util.List;

@Path("/event")
public interface EventServiceRest {
    /**------------------------------------------------------------------------------------------**/
    /**
     * Return a list of events since the timestamp lastTS
     */
    @POST
    @Path("/getEvents")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getEvents(@HeaderParam("sessionID") String sessionID, GetEventsParam param);

    /**
     * Return a list of message to display according to the language selection
     */
    @POST
    @Path("/getMessages")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getMessages(@HeaderParam("sessionID") String sessionID, GetMessagesParam param);

    /**
     * Return list of message count for type mask
     *
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/getMessagesCount")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getMessagesCount(@HeaderParam("sessionID") String sessionID, GetMessagesCountParam param);

    /**------------------------------------------------------------------------------------------**/

    /**
     * @param sessionID
     * @param param
     * @return
     */
    @POST
    @Path("/setMessageRead")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse setMessageRead(@HeaderParam("sessionID") String sessionID, SetMessageReadParam param);

    class GetEventsParam {
        public long lastTS;
        public long tableID = -1;

        @JsonIgnore
        public boolean isValid() {
            return lastTS >= 0;
        }

        @JsonIgnore
        public String toString() {
            return "lastTS=" + lastTS + " - tableID=" + tableID;
        }
    }

    class GetEventsResponse {
        public List<Event> listEvent;
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetMessagesParam {
        public long sender;
        public int typeMask;
        public int offset;
        public int nbMax;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "sender=" + sender + " - typeMask=" + typeMask + " - offset=" + offset + " - nbMax=" + nbMax;
        }
    }

    class GetMessagesReponse {
        public List<WSMessage> listMessages;
        public int totalSize;
        public int offset;
    }

    class GetMessagesCountParam {
        public int typeMask;
        public long lastTS;

        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "typeMask=" + typeMask + " - lastTS=" + lastTS;
        }
    }

    /**
     * ------------------------------------------------------------------------------------------
     **/

    class GetMessagesCountResponse {
        public List<MessageCountResult> listMessageCount;
        public long currentTS;

        public String toString() {
            String str = "{currentTS=" + currentTS + " - listMessageCount=";
            if (listMessageCount != null) {
                for (MessageCountResult t : listMessageCount) {
                    str += t.toString();
                }
            }
            str += "}";
            return str;
        }
    }

    class SetMessageReadParam {
        public List<Long> listMsgID;
        public List<String> listID;

        @JsonIgnore
        public boolean isValid() {
            if (listMsgID == null) {
                listMsgID = new ArrayList<Long>();
            }
            return true;
        }

        @JsonIgnore
        public String toString() {
            String str = "";
            if (listMsgID != null) {
                str += "listMsgID size=" + listMsgID.size();
            } else {
                str += "listMsgID null";
            }
            if (listID != null) {
                str += " - listID size=" + listID.size();
            } else {
                str += " - listID null";
            }
            return str;
        }
    }

    class SetMessageReadResponse {
        public boolean result;
    }

}
