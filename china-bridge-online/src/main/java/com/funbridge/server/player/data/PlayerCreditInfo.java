package com.funbridge.server.player.data;

import com.funbridge.server.common.Constantes;

/**
 * Created by pserent on 30/10/2014.
 */
public class PlayerCreditInfo {
    public int nbDealBuy = 0;
    public int nbDealBonus = 0;
    public int nbDealPlayedBuy = 0;
    public int nbDealPlayedBonus = 0;
    public int nbDealPlayedAbo = 0;
    public long dateSubscriptionExpiration = 0;

    /**
     * Compute the credit : nbDealBonus - nbDealPlayedBonus + nbDealBuy - nbDealPlayedBuy
     * @return
     */
    public int computeCredit() {
        return nbDealBonus - nbDealPlayedBonus + nbDealBuy - nbDealPlayedBuy;
    }

    public String toString() {
        return "computeCredit="+computeCredit()+" - nbDealBuy="+nbDealBuy+" - nbDealBonus="+nbDealBonus+" - nbDealPlayedBuy="+nbDealPlayedBuy+" - nbDealPlayedBonus="+nbDealPlayedBonus+" - nbDealPlayedAbo="+nbDealPlayedAbo+" - dateSubscriptionExpiration="+ Constantes.timestamp2StringDateHour(dateSubscriptionExpiration);
    }
}
