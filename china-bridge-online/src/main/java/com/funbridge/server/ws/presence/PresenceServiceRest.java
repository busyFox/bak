package com.funbridge.server.ws.presence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.funbridge.server.ws.FBWSResponse;
import com.funbridge.server.ws.player.WSContextInfo;
import com.funbridge.server.ws.player.WSPlayerInfo;

import javax.ws.rs.*;

@Path("/presence")
public interface PresenceServiceRest {

    @POST
    @Path("/hello")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse hello(HelloParam param);
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

	/**------------------------------------------------------------------------------------------**/
	/**
	 * send SMS Code
	 */
	@POST
	@Path("/sendSMSCode")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse sendSMSCode(SendSMSCodeParam param);
	class SendSMSCodeParam{
		public String phone ;
		public String deviceType ;
		public String app ;
		@JsonIgnore
		public boolean isValid() {
			if (phone == null || phone.isEmpty()){
				return false ;
			}
			return deviceType != null && deviceType.length() != 0;
		}
	}
	class SendSMSCodeResponse{
		public boolean send;
		public String msg;
	}


    /**------------------------------------------------------------------------------------------**/
	/**
	 * Create an account for a new player (new method).
	 */
	@POST
	@Path("/createAccount3")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse createAccount3(CreateAccount3Param param);
	class CreateAccount3Param{
		public String data;
		@JsonIgnore
		public boolean isValid() {
            return data != null && data.length() != 0;
        }
		@JsonIgnore
		public String toString() {
			return "data= ....";
		}
	}
	class CreateAccount3Response{
		public int nbDealBonus;
		public int nbDealBonusValid;
	}

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Check if the session is valid
	 */
	@POST
	@Path("/isSessionValid")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse isSessionValid(@HeaderParam("sessionID")String sessionID);
	class IsSessionValidResponse{
		public boolean result;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Send the password by mail to the user
	 */
	@POST
	@Path("/sendPassword")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse sendPassword(SendPasswordParam param);
	class SendPasswordParam{
		public String login;
		@JsonIgnore
		public boolean isValid() {
            return login != null && login.length() != 0;
        }
		@JsonIgnore
		public String toString() {
			return "login="+login;
		}
	}
	class SendPasswordResponse{
		public boolean result;
	}

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Set the value of push token for this device
	 */
	@POST
	@Path("/setDevicePushToken")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse setDevicePushToken(SetDevicePushTokenParam param);
	class SetDevicePushTokenParam{
		public String deviceID;
		public String pushToken;
		public String FCMToken;
		public String deviceType;
		@JsonIgnore
		public boolean isValid() {
			if (deviceID == null || deviceID.length() == 0) {
				return false;
			}
            return (pushToken != null && pushToken.length() != 0) || (FCMToken != null && FCMToken.length() != 0);
        }
		@JsonIgnore
		public String toString() {
			return "deviceID="+deviceID+" - deviceType="+deviceType+" - pushToken="+pushToken;
		}

	}
	class SetDevicePushTokenResponse{
		public boolean result;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Change the player mail. 
	 */
	@POST
	@Path("/changePlayerMail")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse changePlayerMail(ChangePlayerMailParam param);
	class ChangePlayerMailParam{
		public String login;
		public String newmail;
		public String password;
		@JsonIgnore
		public boolean isValid() {
			if (login == null || login.length() == 0) {
				return false;
			}
			if (newmail == null || newmail.length() == 0) {
				return false;
			}
            return password != null && password.length() != 0;
        }
		@JsonIgnore
		public String toString() {
			return "login="+login+" - newmail="+newmail+" - password="+password;
		}
	}
	class ChangePlayerMailResponse{
		public boolean result;
	}
	
	/**------------------------------------------------------------------------------------------**/
	/**
	 * Login method in one step. No challenge, password is given in AES crypt mode 
	 */
	@POST
	@Path("/login")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse login(LoginParam param);
	class LoginParam {
		public String data;
		@JsonIgnore
		public boolean isValid() {
            return data != null && data.length() != 0;
        }
		@JsonIgnore
		public String toString() {
			return "data= ....";
		}
	}
	class LoginResponse {
		public String sessionID;
		public WSPlayerInfo playerInfo;
		public WSContextInfo contextInfo;
        public String privileges;
        public boolean createPlayerOnLogin = false;
	}

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Login trial method in one step.
	 */
	@POST
	@Path("/loginTrial")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse loginTrial(LoginTrialParam param);
	class LoginTrialParam {
		public String deviceID;
		public String deviceType;
		public String clientVersion;
		public String lang;
		public String displayLang;
		@JsonIgnore
		public boolean isValid() {
            return deviceID != null && deviceID.length() != 0 &&
                    deviceType != null && deviceType.length() != 0 &&
                    clientVersion != null && clientVersion.length() != 0 &&
                    lang != null && lang.length() != 0;
        }
		@JsonIgnore
		public String toString() {
			return "deviceID="+deviceID+" - deviceType="+deviceType+" - clientVersion="+clientVersion+" - lang="+lang;
		}
	}

    /**------------------------------------------------------------------------------------------**/
    /**
     * Clear accounts for this device
     */
    @POST
    @Path("/clearDeviceAccounts")
    @Consumes("application/json")
    @Produces("application/json")
    FBWSResponse clearDeviceAccounts(ClearDeviceAccountsParam param);
    class ClearDeviceAccountsParam {
        public String deviceID;
        @JsonIgnore
        public boolean isValid() {
            return deviceID != null && deviceID.length() != 0;
        }
        @JsonIgnore
        public String toString() {
            return "deviceID="+deviceID;
        }
    }
	class ClearDeviceAccountsResponse {
        public boolean result;
    }

	/**------------------------------------------------------------------------------------------**/
	/**
	 * Reset nursing for a player
	 */
	@POST
	@Path("/resetNursing")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse resetNursing(@HeaderParam("sessionID")String sessionID);
	class ResetNursingResponse {
		public boolean result;
	}

	/**------------------------------------------------------------------------------------------**/
	/**
	 * WeChat Authorized Login
	 */
	@POST
	@Path("/wxlogin")
	@Consumes("application/json")
	@Produces("application/json")
	FBWSResponse wxlogin(WXLoginParam param);
	class WXLoginParam {
		public String data;
		@JsonIgnore
		public boolean isValid() {
			return data != null && data.length() != 0;
		}
		@JsonIgnore
		public String toString() {
			return "data= ....";
		}
	}
	class WXLoginResponse {
		public String sessionID;
		public WSPlayerInfo playerInfo;
		public WSContextInfo contextInfo;
		public WXUserInfo wxUserInfo ;
		public String privileges;
		public boolean createPlayerOnLogin = false;
	}
}
