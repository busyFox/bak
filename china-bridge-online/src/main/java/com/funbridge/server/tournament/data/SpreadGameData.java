package com.funbridge.server.tournament.data;

public class SpreadGameData {
	private char requester;
	private int nbResponseOK = 0;
	private boolean waitingForCheckSpread = true;
	private int nbResponseWaiting = 0;
	private int nbResponseReceived = 0;
    private long dateLastAddSpreadPlay = 0;

    public String toString() {
        return "requester="+ requester +" - nbResponseOK="+nbResponseOK+" - waitingForCheckSpread="+waitingForCheckSpread+" - nbResponseWaiting="+nbResponseWaiting+" - nbResponseReceived="+nbResponseReceived;
    }
	public SpreadGameData(char requester, boolean waitingForCheckSpread, int nbResponseWaiting) {
		this.requester = requester;
		this.waitingForCheckSpread = waitingForCheckSpread;
		this.nbResponseWaiting = nbResponseWaiting;
	}
	public boolean isWaitingForCheckSpread() {
		return waitingForCheckSpread;
	}
	public void setWaitingForCheckSpread(boolean waitingForCheckSpread) {
		this.waitingForCheckSpread = waitingForCheckSpread;
	}
	public char getRequester() {
		return requester;
	}
	public void incrementResponseReceived() {
		nbResponseReceived++;
	}
	public void incrementResponseOK() {
		nbResponseOK++;
	}
	public boolean isAllResponseOK() {
		return (nbResponseOK == nbResponseWaiting);
	}
	public boolean isAllResponseReceived() {
		return (nbResponseReceived == nbResponseWaiting);
	}

    public long getDateLastAddSpreadPlay() {
        return dateLastAddSpreadPlay;
    }

    public void setDateLastAddSpreadPlay(long dateLastAddSpreadPlay) {
        this.dateLastAddSpreadPlay = dateLastAddSpreadPlay;
    }
}
