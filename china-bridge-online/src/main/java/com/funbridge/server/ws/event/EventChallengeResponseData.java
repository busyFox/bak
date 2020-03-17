package com.funbridge.server.ws.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "eventChallengeResponse")
public class EventChallengeResponseData {
    public long partnerID;
    public String partnerPseudo;
    public long challengeID;
    public boolean response;
    public String message;

    @JsonIgnore
    public String toString() {
        return "partnerID=" + partnerID + " - partnerPseudo=" + partnerPseudo + " - challengeID=" + challengeID + " - response=" + response + " - message=" + message;
    }
}
