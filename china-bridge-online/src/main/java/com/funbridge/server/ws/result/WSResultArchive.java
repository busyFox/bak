package com.funbridge.server.ws.result;

import com.funbridge.server.ws.tournament.WSTournament;

import java.util.List;

public class WSResultArchive {
    private List<WSTournament> listTournament;
    private int offset = 0;
    private int totalSize = 0;

    public List<WSTournament> getListTournament() {
        return listTournament;
    }

    public void setListTournament(List<WSTournament> listTournament) {
        this.listTournament = listTournament;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }


}
