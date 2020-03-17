package com.funbridge.server.ws.event;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "eventChallengeRequest")
public class EventChallengeRequestData {
    public long creatorID;
    public String creatorPseudo;
    public long challengeID;
    public String message;
    public long currentTS;
    public long challengeExpiration;
    public String mode;
    public String theme;
    public boolean reset = false;

    @JsonIgnore
    public String toString() {
        return "creatorID=" + creatorID + " - creatorPseudo=" + creatorPseudo + " - challengeID=" + challengeID + " - message=" + message;
    }
}
