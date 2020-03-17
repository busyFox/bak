package com.funbridge.server.message.data;

import com.funbridge.server.common.Constantes;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Created by bplays on 04/11/16.
 */
public abstract class GenericChatroom {
    protected String name;
    protected String nameTemplate;
    protected String imageID;
    protected String type;
    protected long creationDate;
    protected long updateDate;
    protected List<ChatroomParticipant> participants = new ArrayList<>();
    protected Date creationDateISO;

    public String toString() {
        return "ID="+getIDStr()+" - name="+name+" - dateCreation="+ Constantes.timestamp2StringDateHour(creationDate)+" - updateDate="+ Constantes.timestamp2StringDateHour(updateDate)+" - participants="+participants.size();
    }

    public abstract String getIDStr();

    /**
     * Get the number of participants in the chatroom
     * @return
     */
    public int getNbParticipants(){
        return participants.size();
    }

    /**
     * Get a participant from the chatroom
     * @param playerID
     * @return
     */
    public ChatroomParticipant getParticipant(long playerID) {
        for (ChatroomParticipant e : participants) {
            if (e.getPlayerID() == playerID) {
                return e;
            }
        }
        return null;
    }

    /**
     * Add a participant to the chatroom
     * @param playerID
     * @return
     */
    public boolean addParticipant(long playerID){
        if(getParticipant(playerID) == null){
            ChatroomParticipant participant = new ChatroomParticipant();
            participant.setPlayerID(playerID);
            participant.setJoinDate(System.currentTimeMillis());
            participants.add(participant);
            updateDate = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * Remove a participant from the chatroom
     * @param playerID
     * @return
     */
    public boolean removeParticipant(long playerID){
        Iterator<ChatroomParticipant> it = participants.iterator();
        while(it.hasNext()){
            ChatroomParticipant participant = it.next();
            if(participant.getPlayerID() == playerID){
                long date = System.currentTimeMillis();
                if(type.equalsIgnoreCase(Constantes.CHATROOM_TYPE_SINGLE)){
                    participant.setResetDate(date);
                } else {
                    it.remove();
                }
                updateDate = date;
                return true;
            }
        }
        return false;
    }

    public List<ChatroomParticipant> getAdministrators(){
        List<ChatroomParticipant> administrators = new ArrayList<>();
        for(ChatroomParticipant participant : participants){
            if(participant.isAdministrator()){
                administrators.add(participant);
            }
        }
        return administrators;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameTemplate() {
        return nameTemplate;
    }

    public void setNameTemplate(String nameTemplate) {
        this.nameTemplate = nameTemplate;
    }

    public long getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(long creationDate) {
        this.creationDate = creationDate;
    }

    public long getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(long updateDate) {
        this.updateDate = updateDate;
    }

    public List<ChatroomParticipant> getParticipants() {
        return participants;
    }

    public String getImageID() {
        return imageID;
    }

    public void setImageID(String imageID) {
        this.imageID = imageID;
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
}
