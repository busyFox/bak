package com.funbridge.server.ws.store;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.FBWSResponse;

import javax.ws.rs.*;
import java.util.List;

@Path("/store")
public interface StoreServiceRest {

    @POST
    @Path("/getProducts")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse getProducts(@HeaderParam("sessionID")String sessionID, GetProductsParam param);
    class GetProductsParam {
        public String storeType;

        @JsonIgnore
        public boolean isValid() {
            return storeType != null && storeType.length() != 0;
        }

        @JsonIgnore
        public String toString() {
            return "storeType=" + storeType;
        }
    }

    class GetProductsResponse {
        public List<WSProductGroup> groupList;
    }

    @POST
    @Path("/newTransaction")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse newTransaction(@HeaderParam("sessionID")String sessionID, NewTransactionParam param);
    class NewTransactionParam {
        public String productID;

        @JsonIgnore
        public boolean isValid() {
            return productID != null && productID.length() != 0;
        }

        @JsonIgnore
        public String toString() {
            return "productID=" + productID;
        }
    }

    class NewTransactionResponse {
        public WSNewTransactionReceipt checkTransactionReceipt;
    }


    @POST
    @Path("/checkTransactionReceipt")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse checkTransactionReceipt(@HeaderParam("sessionID") String sessionID, CheckTransactionReceiptParam param);
    class CheckTransactionReceiptParam {
        public String receiptData;
        public String transactionID ;
        public int receiptType ;
        public long targetPlayerID ;

        @JsonIgnore
        public boolean isValid() {
            if (receiptData == null || receiptData.isEmpty()){
                return false ;
            }
            return transactionID != null && !transactionID.isEmpty();
        }

        @JsonIgnore
        public String toString() {
            return "receiptData=" + receiptData + ",transactionID=" + transactionID +",receiptType=" + receiptType + ",targetPlayerID=" + targetPlayerID;
        }
    }

    class CheckTransactionReceiptResponse {
        public WSNewTransactionReceipt checkTransactionReceipt;
    }

}
