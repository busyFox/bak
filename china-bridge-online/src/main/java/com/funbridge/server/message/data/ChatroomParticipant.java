package com.funbridge.server.message.data;

/**
 * Created by bplays on 04/11/16.
 */
public class ChatroomParticipant {
    private long playerID;
    private long joinDate;
    private long lastRead;
    private long resetDate = 0;
    private String gameID;
    private boolean administrator;
    private boolean muted;

    public long getPlayerID() {
        return playerID;
    }

    public void setPlayerID(long playerID) {
        this.playerID = playerID;
    }

    public long getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(long joinDate) {
        this.joinDate = joinDate;
    }

    public long getLastRead() {
        return lastRead;
    }

    public void setLastRead(long lastRead) {
        this.lastRead = lastRead;
    }

    public long getResetDate() {
        return resetDate;
    }

    public void setResetDate(long resetDate) {
        this.resetDate = resetDate;
    }

    public String getGameID() {
        return gameID;
    }

    public void setGameID(String gameID) {
        this.gameID = gameID;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    public void setAdministrator(boolean administrator) {
        this.administrator = administrator;
    }

    public boolean hasMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }
}
