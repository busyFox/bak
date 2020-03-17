package com.gotogames.bridge.engineserver.ws.compute;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gotogames.bridge.engineserver.ws.WSResponse;

import javax.ws.rs.*;
import java.util.List;

@Path("/compute")
public interface ComputeService {
    /**------------------------------------------------------------------------------------------**/
    @POST
    @Path("/getQuery")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse getQuery(@HeaderParam("sessionID")String sessionID);
    class GetQueryResponse {
        public long computeID = -1;
        public String deal = "";
        public String game = "";
        public String conventions = "";
        public String options = "";
        public int queryType = 0;
        public int pollingValue = 0;
        public int nbTricksForClaim = 0;
        public String claimPlayer = "";
    }

    /**------------------------------------------------------------------------------------------**/
    @POST
    @Path("/getQueries")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse getQueries(@HeaderParam("sessionID")String sessionID, GetQueriesParam param);
    class GetQueriesParam {
        public int nbMax;
        public WSEngineStat engineStat;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "nbMax="+nbMax+" - engineStat="+engineStat;
        }
    }
    class GetQueriesResponse {
        public List<GetQueryResponse> queries;
    }

    /**------------------------------------------------------------------------------------------**/
    @POST
    @Path("/setResult")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse setResult(@HeaderParam("sessionID")String sessionID, SetResultParam param);
    class SetResultParam {
        public long computeID;
        public String result;
        @JsonIgnore
        public boolean isValid() {
            return true;
        }

        @JsonIgnore
        public String toString() {
            return "computeID="+computeID+" - result="+result;
        }
    }

    /**------------------------------------------------------------------------------------------**/
    @POST
    @Path("/setResults")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse setResults(@HeaderParam("sessionID")String sessionID, SetResultsParam param);
    class SetResultsParam {
        public List<SetResultParam> results;
        public WSEngineStat engineStat;
        @JsonIgnore
        public boolean isValid() {
            return results != null;
        }

        @JsonIgnore
        public String toString() {
            return "nb results="+(results!=null?results.size():"null")+" - engineStat="+engineStat;
        }
    }
}
