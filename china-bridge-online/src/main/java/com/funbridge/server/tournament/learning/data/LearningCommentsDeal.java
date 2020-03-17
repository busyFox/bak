package com.funbridge.server.tournament.learning.data;

import com.funbridge.server.common.Constantes;
import com.gotogames.common.bridge.BridgeBid;
import com.gotogames.common.bridge.BridgeCard;
import com.gotogames.common.bridge.BridgeDeal;
import com.gotogames.common.bridge.GameBridgeRule;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * Created by ldelbarre on 31/05/2018.
 */
@Document(collection="learning_comments_deal")
public class LearningCommentsDeal {
    @Id
    private ObjectId ID;
    public Date dateCreation;
    public String chapterID;
    public char dealer;
    public char vulnerability;
    public String distribution;
    public String contract;
    public String declarer;
    public String bids;
    public String begins;
    public int nbTricks;
    public String importDealFileID = "unknown";
    public int importDealNumber = 0;
    public String importProcessError = null;
    public boolean useBids = false;

    public String getIDStr() {
        if (ID != null) {
            return ID.toString();
        }
        return null;
    }

    public String toString() {
        return "ID="+getIDStr()+" - chapterID="+chapterID+" - dealer="+getStrDealer()+" - vulnerability="+getStrVulnerability()+" - distribution="+distribution+" - contract="+contract+" - bids="+bids+" - begins="+begins+" - nbTricks="+nbTricks+" - importDealFileID="+importDealFileID+" - importDealNumber="+importDealNumber+" - useBids="+useBids;
    }

    public String getStrDealer() {
        return Character.toString(dealer);
    }

    public String getStrVulnerability() {
        return Character.toString(vulnerability);
    }

    public String getDealFileID() {
        String result = null;
        if (importDealFileID != null && importDealFileID.length() > 0) {
            String[] temp = importDealFileID.split(Constantes.SEPARATOR_VALUE);
            if (temp[0].length() > 0) {
                result = temp[0];
            }
        }
        if (result != null) {
            if (result.lastIndexOf('.') > 0) {
                result = result.substring(0, result.lastIndexOf('.'));
            }
            result += "-"+String.format("%02d", importDealNumber);
        }
        return result;
    }

    public void addImportProcessError(String str) {
        if (importProcessError == null || importProcessError.length() == 0) {
            importProcessError = str;
        } else {
            importProcessError += " - "+str;
        }

    }

    public LearningDeal transformDeal(int idxDeal) {
        LearningDeal deal = new LearningDeal();
        deal.index = idxDeal;
        deal.dealer = this.dealer;
        deal.vulnerability = this.vulnerability;
        deal.cards = this.distribution;
        deal.learningCommentsDeal = getIDStr();
        deal.bids = this.bids;
        deal.begins = this.begins;
        return deal;
    }

    public String getBidsWithoutOwnerAndSeparator() {
        if (bids != null && bids.length() > 0) {
            String[] temp = bids.split(Constantes.GAME_BIDCARD_SEPARATOR);
            String result = "";
            for (String e : temp) {
                if (e.length() != 3) {
                    return null;
                }
                result += e.substring(0,2);
            }
            return result;
        }
        return null;
    }

    /**
     * Check all data field
     * @return
     */
    public String checkData() {
        // check chapterID
        if (chapterID == null || chapterID.length() == 0) {
            return "AuthorID field not valid !";
        }
        // importDealFileID
        if (importDealFileID == null || importDealFileID.length() == 0) {
            return "ImportDealFileID field not valid";
        }
        // importDealNumber
        if (importDealNumber <= 0) {
            return "ImportDealNumber field not valid";
        }
        // check dealer, vul. & distribution
        if (!BridgeDeal.isDealValid(getStrDealer()+getStrVulnerability()+distribution)) {
            return "Distribution not valid";
        }
        // bids
        if (bids == null || bids.length() == 0) {
            return "Bid sequence empty";
        }
        List<BridgeBid> listBridgeBid = GameBridgeRule.convertPlayBidsStringToList(getBidsWithoutOwnerAndSeparator(), dealer);
        if (listBridgeBid == null || listBridgeBid.size() == 0) {
            return "Bid sequence not valid - failed to convert to BridgeBid list";
        }
        if (!GameBridgeRule.isBidsSequenceValid(listBridgeBid)) {
            return "Bid sequence not valid";
        }
        // begins
        if (begins != null && begins.length() == 3) {
            BridgeCard bridgeCard = BridgeCard.createCard(begins.substring(0,2), begins.charAt(2));
            if (bridgeCard == null) {
                return "Begin card not valid";
            }
            if (!GameBridgeRule.isCardValid(null, bridgeCard, GameBridgeRule.convertCardDealToList(distribution))) {
                return "Begin card not valid for distribution";
            }

        } else if (begins != null && begins.length() > 0 && begins.length() != 3) {
            return "Begin card format not valid";
        }
        // contract & declarer
        if (!GameBridgeRule.isBidsFinished(listBridgeBid)) {
            return "Bid sequence not finished";
        }
        BridgeBid contractBid = GameBridgeRule.getHigherBid(listBridgeBid);
        if (contractBid == null) {
            return "Failed to get higher bid from sequence";
        }
        String contractCheck = contractBid.getString();
        if (GameBridgeRule.isX2(listBridgeBid)) {
            contractCheck += "X2";
        } else if (GameBridgeRule.isX1(listBridgeBid)) {
            contractCheck += "X1";
        }
        if (!contractCheck.equals(contract)) {
            return "Contract values are not the same between bids sequence ("+contractCheck+") and contract field("+contract+")";
        }
        if (declarer == null || declarer.length() == 0 || GameBridgeRule.getWinnerBids(listBridgeBid) != declarer.charAt(0)) {
            return "Declarer not valid";
        }
        // all is OK
        return "OK";
    }

}
