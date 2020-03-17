package com.gotogames.bridge.engineserver.ws.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gotogames.bridge.engineserver.common.Constantes;
import com.gotogames.bridge.engineserver.ws.WSResponse;
import com.gotogames.common.bridge.BridgeConstantes;

import javax.ws.rs.*;

@Path("/request")
public interface RequestService {

    /**------------------------------------------------------------------------------------------**/
    @POST
    @Path("/getNextBid")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse getNextBid(GetNextBidParam param);
    class GetNextBidParam {
        public String user;
        public String deal;
        public String game;
        public int resultType = 1; // 1 : paire - 2 : IMP
        @JsonIgnore
        public boolean isValid() {
            if (user == null || user.length() == 0 || deal == null || deal.length() == 0) {
                return false;
            }
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "user="+user+" - deal="+deal+" - game="+game+" - resultType="+resultType;
        }
    }
    class GetNextBidResponse {
        public String result;
        public String toString() {
            return "result="+result;
        }
    }

    /**------------------------------------------------------------------------------------------**/
    @POST
    @Path("/getNextCard")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse getNextCard(GetNextCardParam param);
    class GetNextCardParam {
        public String user;
        public String deal;
        public String game;
        public int resultType = 1; // 1 : paire - 2 : IMP
        @JsonIgnore
        public boolean isValid() {
            if (user == null || user.length() == 0 || deal == null || deal.length() == 0 || game == null || game.length() == 0) {
                return false;
            }
            return true;
        }
        @JsonIgnore
        public String toString() {
            return "user="+user+" - deal="+deal+" - game="+game+" - resultType="+resultType;
        }
    }
    class GetNextCardResponse {
        public String result;
        public String toString() {
            return "result="+result;
        }
    }

    /**------------------------------------------------------------------------------------------**/
    @POST
    @Path("/getResult")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse getResult(@HeaderParam("sessionID")String sessionID, GetResultParam param);
    class GetResultParam {
        public String deal;
        public String game;
        public String conventions;
        public String options;
        public int requestType;
        public boolean useCache = true;
        public boolean logStat = false;
        public String asyncID = null;
        public int nbTricksForClaim = 0;
        public String claimPlayer = Character.toString(BridgeConstantes.POSITION_NOT_VALID);
        @JsonIgnore
        public boolean isValid() {
            if (deal == null || deal.length() == 0) {
                return false;
            }
            if (requestType != Constantes.REQUEST_TYPE_PAR && (conventions == null || conventions.length() == 0)) {
                return false;
            }
            if (options == null || options.length() == 0) {
                return false;
            }
            if (requestType != Constantes.REQUEST_TYPE_BID &&
                    requestType != Constantes.REQUEST_TYPE_BID_INFO &&
                    requestType != Constantes.REQUEST_TYPE_CARD &&
                    requestType != Constantes.REQUEST_TYPE_PAR &&
                    requestType != Constantes.REQUEST_TYPE_CLAIM) {
                return false;
            }
            return true;
        }
        @JsonIgnore
        public boolean isAsync() {
            return asyncID != null && asyncID.length() > 0;
        }

        @JsonIgnore
        public String toString() {
            return "deal="+deal+" - game="+game+" - conventions="+conventions+" - options="+options+" - requestType="+requestType+" - useCache="+useCache+" - logStat="+logStat+" - asyncID="+asyncID+" - nbTricksForClaim="+nbTricksForClaim+" - claimPlayer="+claimPlayer;
        }

        /**
         * Return the key of the request : all field separated by a ';'
         * If type is bid info, the deal is fantom deal (SASSSSSSSSSSSSSWWWWWWWWWWWWWNNNNNNNNNNNNNEEEEEEEEEEEEE)
         * @return
         */
        @JsonIgnore
        public String getKey() {
            String dealTemp = deal;
            // for type BID INFO set specific deal to minimize the number of tree
            if (requestType == Constantes.REQUEST_TYPE_BID_INFO) {
                switch (deal.charAt(0)) {
                    case 'N':
                    case 'S':
                        dealTemp = "S";
                        break;
                    default:
                        dealTemp = "E";
                        break;
                }
                dealTemp += "A"+Constantes.REQUEST_DEAL_BIDINFO;
            }
            return dealTemp+Constantes.REQUEST_FIELD_SEPARATOR+
                    options+Constantes.REQUEST_FIELD_SEPARATOR+
                    conventions+Constantes.REQUEST_FIELD_SEPARATOR+
                    game+Constantes.REQUEST_FIELD_SEPARATOR+
                    requestType+Constantes.REQUEST_FIELD_SEPARATOR+
                    nbTricksForClaim+Constantes.REQUEST_FIELD_SEPARATOR+
                    claimPlayer;
        }
    }
    class GetResultResponse {
        public String result;
        public String toString() {
            return "result="+result;
        }
    }

    @POST
    @Path("/getResultWithCrypt")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse getResultWithCrypt(@HeaderParam("sessionID")String sessionID, GetResultParam param);

    @POST
    @Path("/clearCache")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse clearCache(@HeaderParam("sessionID")String sessionID);
    class ClearCacheResponse {
        public boolean result;
        public String toString() {
            return "result="+result;
        }
    }
}
