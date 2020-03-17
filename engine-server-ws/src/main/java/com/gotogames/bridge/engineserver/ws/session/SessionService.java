package com.gotogames.bridge.engineserver.ws.session;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gotogames.bridge.engineserver.ws.WSResponse;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/session")
public interface SessionService {

    @POST
    @Path("/hello")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse hello(HelloParam param);
    class HelloParam {
        public String name;
        @JsonIgnore
        public String toString() {
            return "name="+name;
        }
    }
    class HelloResponse {
        public String response;
    }

	/**
	 * Create a session for login
	 * A challenge is return and it must be decrypt and return using checkChallenge method
	 */
    @POST
    @Path("/openSession")
    @Consumes("application/json")
    @Produces("application/json")
	WSResponse openSession(OpenSessionParam param);
    class OpenSessionParam {
        public String login;
        @JsonIgnore
        public boolean isValid() {
            if (login != null && login.length() > 0){
                return true;
            }
            return false;
        }

        @JsonIgnore
        public String toString() {
            return "login="+login;
        }
    }
    class OpenSessionResponse {
        public String challenge;
    }

	/**
	 * Create a session for login and specify the list engine version used
	 * A challenge is return and it must be decrypt and return using checkChallenge method
	 */
    @POST
    @Path("/openSessionEngineVersion")
    @Consumes("application/json")
    @Produces("application/json")
	WSResponse openSessionEngineVersion(OpenSessionEngineVersionParam param);
    class OpenSessionEngineVersionParam {
        public String login;
        public String listEngineVersion;
        @JsonIgnore
        public boolean isValid() {
            if (login != null && login.length() > 0 && listEngineVersion != null && listEngineVersion.length() > 0){
                return true;
            }
            return false;
        }

        @JsonIgnore
        public String toString() {
            return "login="+login+" - listEngineVersion="+listEngineVersion;
        }
    }
    class OpenSessionEngineVersionResponse {
        public String challenge;
    }


	/**
	 * Check the challenge string from openSession method
	 * If the challenge decrypt is valid, a session ID is returned
	 */
    @POST
    @Path("/checkChallenge")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse checkChallenge(CheckChallengeParam param);
    class CheckChallengeParam {
        public String login;
        public String challenge;
        public String urlFBSetResult;
        @JsonIgnore
        public boolean isValid() {
            if (login != null && login.length() > 0 && challenge != null && challenge.length() > 0){
                return true;
            }
            return false;
        }

        @JsonIgnore
        public String toString() {
            return "login="+login+" - challenge="+challenge;
        }
    }
    class CheckChallengeResponse {
        public String sessionID;
    }
	/**
	 * Terminate the session
	 */
    @POST
    @Path("/closeSession")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse closeSession(CloseSessionParam param);
    class CloseSessionParam {
        public String sessionID;
        @JsonIgnore
        public boolean isValid() {
            if (sessionID != null && sessionID.length() > 0){
                return true;
            }
            return false;
        }

        @JsonIgnore
        public String toString() {
            return "sessionID="+sessionID;
        }
    }
    class CloseSessionResponse {
        public boolean result;
    }

	/**
	 * Check if the session is valid
	 */
    @POST
    @Path("/isSessionValid")
    @Consumes("application/json")
    @Produces("application/json")
    WSResponse isSessionValid(IsSessionValidParam param);
    class IsSessionValidParam {
        public String sessionID;
        @JsonIgnore
        public boolean isValid() {
            if (sessionID != null && sessionID.length() > 0){
                return true;
            }
            return false;
        }

        @JsonIgnore
        public String toString() {
            return "sessionID="+sessionID;
        }
    }
    class IsSessionValidResponse {
        public boolean result;
    }
}
