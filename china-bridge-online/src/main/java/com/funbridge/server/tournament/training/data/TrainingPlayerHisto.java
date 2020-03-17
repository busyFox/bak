package com.funbridge.server.tournament.training.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Created by pserent on 09/04/2015.
 */
@Document(collection="training_player_histo")
public class TrainingPlayerHisto {
    @Id
    private ObjectId ID;
    private long playerId;

    private double resultIMP = 0;
    private long resetIMPDate = 0;
    private int nbResultIMP = -1;

    private double resultPaires = 0;
    private int nbResultPaires = 0;
    private long resetPairesDate = 0;

    public long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(long playerId) {
        this.playerId = playerId;
    }

    public double getResultIMP() {
        return resultIMP;
    }

    public void setResultIMP(double resultIMP) {
        this.resultIMP = resultIMP;
    }

    public long getResetIMPDate() {
        return resetIMPDate;
    }

    public void setResetIMPDate(long resetIMPDate) {
        this.resetIMPDate = resetIMPDate;
    }

    public double getResultPaires() {
        return resultPaires;
    }

    public void setResultPaires(double resultPaires) {
        this.resultPaires = resultPaires;
    }

    public int getNbResultPaires() {
        return nbResultPaires;
    }

    public void setNbResultPaires(int nbResultPaires) {
        this.nbResultPaires = nbResultPaires;
    }

    public long getResetPairesDate() {
        return resetPairesDate;
    }

    public void setResetPairesDate(long resetPairesDate) {
        this.resetPairesDate = resetPairesDate;
    }

    public void addResultPaire(double val) {
        this.resultPaires += val;
        this.nbResultPaires++;
    }

    public void addResultIMP(double val) {
        this.resultIMP += val;
        if (this.nbResultIMP >= 0) {
            this.nbResultIMP++;
        }
    }

    public int getNbResultIMP() {
        return nbResultIMP;
    }

    public void setNbResultIMP(int nbResultIMP) {
        this.nbResultIMP = nbResultIMP;
    }
}
