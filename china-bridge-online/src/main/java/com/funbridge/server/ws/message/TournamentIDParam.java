package com.funbridge.server.ws.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by bplays on 07/11/16.
 */
public class TournamentIDParam {
    public String tournamentID;

    @JsonIgnore
    public boolean isValid(){
        return tournamentID != null && !tournamentID.isEmpty();
    }

    @JsonIgnore
    public String toString(){
        return "tournamentID="+tournamentID;
    }
}
