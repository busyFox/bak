package com.funbridge.server.ws.presence;

import com.funbridge.server.Utils.RedisUtils;
import com.funbridge.server.common.*;
import com.funbridge.server.message.GenericChatMgr;
import com.funbridge.server.message.MessageMgr;
import com.funbridge.server.message.MessageNotifMgr;
import com.funbridge.server.nursing.NursingMgr;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCacheMgr;
import com.funbridge.server.player.data.Device;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.player.data.Privileges;
import com.funbridge.server.presence.FBSession;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.store.StoreMgr;
import com.funbridge.server.ws.*;
import com.funbridge.server.ws.player.WSPlayerInfo;
import com.gotogames.common.crypt.AESCrypto;
import com.gotogames.common.lock.LockWeakString;
import com.gotogames.common.tools.JSONTools;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

@Service(value = "presenceService")
@Scope(value = "singleton")
public class PresenceServiceRestImpl extends FunbridgeMgr implements PresenceServiceRest {
    @Resource(name = "presenceMgr")
    private PresenceMgr presenceMgr;
    @Resource(name = "playerMgr")
    private PlayerMgr playerMgr;
    @Resource(name = "playerCacheMgr")
    private PlayerCacheMgr playerCacheMgr;
    @Resource(name = "messageMgr")
    private MessageMgr messageMgr;
    @Resource(name = "messageNotifMgr")
    private MessageNotifMgr notifMgr = null;
    @Resource(name = "nursingMgr")
    private NursingMgr nursingMgr = null;
    @Resource(name = "chatMgr")
    private GenericChatMgr chatMgr = null;
    private LockWeakString lockLogin = new LockWeakString();
    private JSONTools jsonTools = new JSONTools();
    private Random randomRpc = new Random(System.nanoTime());

    /**
     * Call by spring on initialisation of bean
     */
    @PostConstruct
    @Override
    public void init() {
    }

    @PreDestroy
    @Override
    public void destroy() {
    }

    @Override
    public void startUp() {

    }

    @Override
    public FBWSResponse hello(HelloParam param) {
        FBWSResponse response = new FBWSResponse();
        HelloResponse resp = new HelloResponse();
        resp.response = "Hello " + param.name + ". Time is : " + Constantes.timestamp2StringDateHour(System.currentTimeMillis());
        response.setData(resp);
        return response;
    }

    /**
     * send SMS Code
     * @param param
     * @return
     */
    @Override
    public FBWSResponse sendSMSCode(SendSMSCodeParam param) {
        FBWSResponse response = new FBWSResponse();
        SendSMSCodeResponse resp = new SendSMSCodeResponse() ;
        if (param == null || !param.isValid()){
            resp.send = false ;
            resp.msg = "参数为空！" ;
            response.setData(resp);
            return response ;
        }
        try {

            //check service maintenance
            if (presenceMgr.isServiceMaintenance()) {
                throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
            }

            //check phone
            if (!PlayerMgr.checkPhoneFormat(param.phone)) {
                log.error("Phone format not valid - param phone=" + param.phone);
                throw new FBWSException(FBExceptionType.PRESENCE_BAD_FORMAT_LOGIN);
            }

            //check service maintenance for device ios/android
            if (presenceMgr.isServiceMaintenanceForDevice(param.deviceType)) {
                throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
            }

            boolean isSend = presenceMgr.sendSMSCode(param.phone) ;
            if (!isSend){
                resp.send = false ;
                resp.msg = "发送失败！" ;
            }else{
                resp.send = true ;
                resp.msg = "发送成功！" ;
            }
            response.setData(resp);

        }catch (FBWSException e) {
            log.error("Exception : " + e.getMessage(), e);
            response.setException(new FBWSExceptionRest(e.getType()));
        }

        return response ;
    }

    /**
     * Check connection data valid.
     *
     * @param conData
     * @throws FBWSException
     */
    private void checkConnectionData(ConnectionData conData) throws FBWSException {
        if (conData == null || !conData.isValid()) {
            log.error("Parameter not valid : loginData=" + conData);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
        if (conData.lang == null || conData.lang.length() == 0) {
            conData.lang = "zh";
        }
        if (conData.country == null || conData.country.length() == 0) {
            conData.country = "CN";
        }

        // check device type
        if (!conData.deviceType.equals(Constantes.DEVICE_TYPE_ANDROID) &&
                !conData.deviceType.equals(Constantes.DEVICE_TYPE_IOS)) {
            log.error("Device not supported ! deviceType=" + conData.deviceType);
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
    }

	@Override
	public FBWSResponse createAccount3(CreateAccount3Param param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            try {
                if (presenceMgr.isServiceMaintenance()) {
                    log.error("THE SERVER IS IN MAINTENANCE");
                    throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
                }
                // decrypt param data
                String strParam = AESCrypto.decrypt(param.data, Constantes.CRYPT_KEY);
                // Map json param to LoginData
                CreateAccountParam createData = jsonTools.mapData(strParam, CreateAccountParam.class);
                if (createData == null || !createData.isValid()) {
                    log.error("THE PARAMETER IS NOT VALID!");
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (presenceMgr.isServiceMaintenanceForDevice(createData.deviceType)) {
                    log.error("THE SERVER IS IN MAINTENANCE");
                    throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
                }
                if (createData.lang == null || createData.lang.length() == 0) {
                    createData.lang = Constantes.PLAYER_LANG_EN;
                }
                // set mail to lower case
                createData.mail = createData.mail.toLowerCase();
                if (log.isDebugEnabled()) {
                    log.debug("param createData=" + createData);
                }
                CreateAccount3Response resp = new CreateAccount3Response();
                // check pseudo, mail & password
                if (!PlayerMgr.checkMailFormat(createData.mail)) {
                    log.error("Mail format not valid - param createData=" + createData);
                    throw new FBWSException(FBExceptionType.PRESENCE_BAD_FORMAT_LOGIN);
                }
                if (!PlayerMgr.checkPseudoFormat(createData.pseudo)) {
                    log.error("Pseudo format not valid - param createData=" + createData);
                    throw new FBWSException(FBExceptionType.PRESENCE_BAD_FORMAT_PSEUDO);
                }
                if (!PlayerMgr.checkPasswordFormat(createData.password)) {
                    log.error("Password format not valid - param createData=" + createData);
                    throw new FBWSException(FBExceptionType.PRESENCE_BAD_FORMAT_PASSWORD);
                }

                // check pseudo & mail not used in Funbridge
                if (playerMgr.getPlayerByPseudo(createData.pseudo) != null) {
                    throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_PSEUDO);
                }
                if (playerMgr.getPlayerByMail(createData.mail) != null) {
                    log.error("Mail already used - createData=" + createData);
                    throw new FBWSException(FBExceptionType.PRESENCE_ALREADY_USED_LOGIN);
                }

                // check nb player on this device ... if too many throw error ...
                int limitNbPlayerForDevice = FBConfiguration.getInstance().getIntValue("presence.createAccountLimitPlayerForDevice", 0);
                Device dev = playerMgr.getDeviceForStrID(createData.deviceID);
                if (limitNbPlayerForDevice > 0 && dev != null) {
                    int nbPlayerForDevice = playerMgr.countPlayerForDevice(dev.getID());
                    if (nbPlayerForDevice >= limitNbPlayerForDevice) {
                        log.error("Too many player for device=" + dev + " - nbPlayerForDevice=" + nbPlayerForDevice + " - limitNbPlayerForDevice=" + limitNbPlayerForDevice + " - param data=" + createData);
                        throw new FBWSException(FBExceptionType.PRESENCE_TOO_MANY_PLAYER_FOR_DEVICE);
                    }
                }

                resp.nbDealBonus = 0;
                resp.nbDealBonusValid = 0;
                response.setData(resp);
                final Player p = playerMgr.createPlayer2(createData.mail, createData.password, createData.pseudo, createData.lang, createData.displayLang);
                if(p == null) {
                    response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
                }
            } catch (FBWSException e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid : param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

	@Override
	public FBWSResponse isSessionValid(String sessionID) {
		FBWSResponse response = new FBWSResponse();
		if (sessionID != null) {
			try {
				if (log.isDebugEnabled()) {
					log.debug("sessionID="+sessionID);
				}
				IsSessionValidResponse resp = new IsSessionValidResponse();
				resp.result = presenceMgr.isSessionValid(sessionID);
				response.setData(resp);
			} catch (Exception e) {
				log.error("Exception : "+e.getMessage(), e);
				response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
			}
		} else {
			log.warn("Parameter not valid");
			response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
		}
		return response;
	}

    @Override
    public FBWSResponse sendPassword(SendPasswordParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            try {
                if (presenceMgr.isServiceMaintenance()) {
                    throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
                }
                if (log.isDebugEnabled()) {
                    log.debug("param=" + param);
                }
                SendPasswordResponse resp = new SendPasswordResponse();
                resp.result = false;
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid : param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse setDevicePushToken(SetDevicePushTokenParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            if (log.isDebugEnabled()) {
                log.debug("param=" + param);
            }
            try {
                if (param.deviceType == null || param.deviceType.length() == 0) {
                    param.deviceType = Constantes.DEVICE_TYPE_IOS;
                }
                SetDevicePushTokenResponse resp = new SetDevicePushTokenResponse();
                resp.result = true;
                response.setData(resp);
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid : param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse changePlayerMail(ChangePlayerMailParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            try {
                throw new FBWSException(FBExceptionType.COMMON_DEPRECATED);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid : param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse login(LoginParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            try {
                // check maintenance
                if (presenceMgr.isServiceMaintenance()) {
                    throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
                }
                // decrypt param data
                String strParam = AESCrypto.decrypt(param.data, Constantes.CRYPT_KEY);
                // Map json param to LoginData
                LoginData loginData = jsonTools.mapData(strParam, LoginData.class);
                if (loginData == null || !loginData.isValid()) {
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (presenceMgr.isServiceMaintenanceForDevice(loginData.deviceType)) {
                    throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
                }
                synchronized (lockLogin.getLock(loginData.login)) {
                    // Set displayLang if null
                    if (loginData.displayLang == null || loginData.displayLang.isEmpty())
                        loginData.displayLang = playerMgr.generateDisplayLang(loginData.lang).toLowerCase();
                    ConnectionData conData = new ConnectionData(loginData);
                    checkConnectionData(conData);

                    Player player = null;

                    // TODO : Check pwd
                    player = playerMgr.getPlayerByLogin(loginData.login);
                    if(player == null){
                        throw new FBWSException(FBExceptionType.PRESENCE_UNKNOWN_PLAYER);
                    } else if(!player.getPassword().equals(PlayerUtilities.cryptPassword(loginData.password))){
                        throw new FBWSException(FBExceptionType.PRESENCE_INVALID_PASSWORD);
                    }

                    // get a session for this valid player
                    FBSession session = buildSession(player, conData);
                    // RPC settings
                    boolean forceRpcEnable = FBConfiguration.getInstance().getIntValue("presence.forceRpcEnable", 1) == 1;
                    int rpcUsePercent = FBConfiguration.getInstance().getIntValue("presence.rpcUsePercent", 100);
                    // build response
                    LoginResponse resp = new LoginResponse();
                    Privileges privileges = playerMgr.getPrivilegesForPlayer(player.getID());
                    if (privileges != null) {
                        if (!privileges.rpc && forceRpcEnable) {
                            if (rpcUsePercent > 0) {
                                int tempRandomRpc = randomRpc.nextInt(100);
                                if (tempRandomRpc < rpcUsePercent) {
                                    privileges.rpc = true;
                                }
                            }
                        }
                        session.setRpcEnabled(privileges.rpc);
                        String strPrivileges = jsonTools.transform2String(new WSPrivileges(privileges), false);
                        resp.privileges = AESCrypto.crypt(strPrivileges, Constantes.CRYPT_KEY);
                    }

                    resp.sessionID = session.getID();
                    WSPlayerInfo playerInfo = playerMgr.playerToWSPlayerInfo(session);
                    resp.playerInfo = playerInfo;
                    resp.contextInfo = presenceMgr.getContextInfo(session);
                    if (log.isDebugEnabled()) {
                        log.debug("player=" + session.getPlayer().getID() + " - " + resp.contextInfo);
                    }
                    response.setData(resp);
                }
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid : param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    /**
     * Realize all operations on login and return session
     *
     * @param player
     * @param connectionData
     * @throws FBWSException
     */
    public FBSession buildSession(Player player, ConnectionData connectionData) throws FBWSException {
        try {
            // get last device used, important to get it before call setDeviceTorPlayer2 !!
            Device lastDevice = playerMgr.getLastDeviceUsedForPlayer(player.getID());

            long dateLastConnection = player.getLastConnectionDate();
            // update device
            Device device = null;
            boolean bFirstConnection = false;
            if (playerMgr.isDeviceExcludeForUpdate(connectionData.deviceID)) {
                device = playerMgr.getDeviceForStrID(connectionData.deviceID);
            } else {
                device = playerMgr.setDeviceForPlayer2(connectionData, player);
            }
            if (device == null) {
                log.error("No device found with deviceID=" + connectionData.deviceID);
                throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
            }
            bFirstConnection = (player.getCreationDate() == dateLastConnection);

            // update player data
            player = playerMgr.updatePlayerOnConnection(player.getID(), connectionData.lang, connectionData.displayLang, connectionData.country, System.currentTimeMillis());

            // create session
            FBSession session = presenceMgr.createSession(player, device, connectionData.clientVersion, dateLastConnection, lastDevice != null ? lastDevice.getType() : null);

            if (session == null) {
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }

            //设置会员日期
            if (player.getSubscriptionExpirationDate() > 0){
                session.setTsSubscriptionExpiration(player.getSubscriptionExpirationDate());
            }

            session.protocol = connectionData.protocol;
            // set nb message not read
            session.setNbNewMessage(chatMgr.getNbUnreadMessagesForPlayer(player.getID(), true));
            // update player cache
            session.setPlayerCache(playerCacheMgr.getAndLoadPlayerCache(player.getID()));


            // special operation on connection ?
            ContextManager.getOperationMgr().processOperationOnConnection(session);

            // nursing
            if (!bFirstConnection) {
                String nursing = nursingMgr.processNursing(player);
                if (nursing != null) {
                    session.setNursing(nursing);
                }
            }

            StoreMgr storeMgr = ContextManager.getStoreMgr();

            // need to update credit amount ?
            try {
                playerMgr.updateCreditOnConnection(session, bFirstConnection);
            } catch (Exception e) {
                log.error("Error to update player credit for player=" + player, e);
                throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
            }

            // set store promo
            session.storePromo = storeMgr.getNbProductPromoForPlayer(session);

            // add notif for friend
            if (presenceMgr.isEventFriendConnectedEnable()) {
                List<Player> listFriend = playerMgr.listFriendForPlayer(session.getPlayer().getID());
                if (listFriend != null && listFriend.size() > 0) {
                    for (Player plaFriend : listFriend) {
                        FBSession sessionFriend = presenceMgr.getSessionForPlayerID(plaFriend.getID());
                        if (sessionFriend != null) {
                            // send event to friend (recipient is friend !)
                            sessionFriend.pushEvent(notifMgr.buildEventFriendConnected(plaFriend, session.getPlayer(), true));
                        }
                    }
                }
                session.setNbFriends(listFriend.size());
            } else {
                session.setNbFriends(playerMgr.countLinkForPlayerAndType(session.getPlayer().getID(), Constantes.PLAYER_LINK_TYPE_FRIEND));
            }

            // Following / Followers
            session.setNbFollowing(playerMgr.countFollowingForPlayer(session.getPlayer().getID()));
            session.setNbFollowers(playerMgr.countFollowerForPlayer(session.getPlayer().getID()));

            session.setPlayerInTeam(ContextManager.getTeamMgr().getTeamForPlayer(player.getID()) != null);
            log.info(session.getID());
            return session;
        } catch (FBWSException e1) {
            throw e1;
        } catch (Exception e2) {
            log.error("Exception : " + e2.getMessage(), e2);
            throw new FBWSException(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public FBWSResponse loginTrial(LoginTrialParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            try {
                throw new FBWSException(FBExceptionType.COMMON_DEPRECATED);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid : param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse clearDeviceAccounts(ClearDeviceAccountsParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            try {
                ClearDeviceAccountsResponse resp = new ClearDeviceAccountsResponse();
                Device device = playerMgr.getDeviceForStrID(param.deviceID);

                response.setData(resp);
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid : param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    @Override
    public FBWSResponse resetNursing(String sessionID) {
        FBWSResponse response = new FBWSResponse();
        if (sessionID != null) {
            try {
                FBSession session = ServiceTools.getAndCheckSession(sessionID);
                ResetNursingResponse resp = new ResetNursingResponse();
                resp.result = nursingMgr.resetNursing(session.getPlayer().getID());
                response.setData(resp);
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e.getType()));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid");
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }

    /**
     * WeChat Login Funcation
     */

    @Override
    public FBWSResponse wxlogin(WXLoginParam param) {
        FBWSResponse response = new FBWSResponse();
        if (param != null && param.isValid()) {
            try {
                // check maintenance
                if (presenceMgr.isServiceMaintenance()) {
                    throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
                }
                // decrypt param data
                String strParam = AESCrypto.decrypt(param.data, Constantes.CRYPT_KEY);
                // Map json param to LoginData
                WXLoginData wxLoginData = jsonTools.mapData(strParam, WXLoginData.class);
                if (wxLoginData == null || !wxLoginData.isValid()) {
                    throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
                }
                if (presenceMgr.isServiceMaintenanceForDevice(wxLoginData.deviceType)) {
                    throw new FBWSException(FBExceptionType.COMMON_SERVER_MAINTENANCE);
                }

//                WXUserInfo wxUserInfo = null ;
                String code = wxLoginData.code ;
                //get openid
                Jedis jedis = RedisUtils.getJedis() ;

                if(jedis.get(code+"_AccessToken") == null && jedis.get(code+"_OppenId") == null){
                    WXAccessToken wxAccessToken = presenceMgr.getAccessToken(wxLoginData.code);
                    if (wxAccessToken == null || wxAccessToken.getErrcode() !=0 ){
                        RedisUtils.returnResource(jedis);
                        log.error("get token is fail errorCode:{},errorMsg:{}",wxAccessToken.getErrcode(),wxAccessToken.getErrmsg());
                        throw new FBWSException(FBExceptionType.PRESENCE_WXLOGIN_GETACCESSTOKEN_FAIL);
                    }
                    jedis.set(wxLoginData.code+"_AccessToken",wxAccessToken.getAccess_token());
                    jedis.expire(wxLoginData.code+"_AccessToken",10);
                    jedis.set(wxLoginData.code+"_OppenId",wxAccessToken.getOpenid());
                    jedis.expire(wxLoginData.code+"_OppenId",10);
                }


                WXUserInfo wxUserInfo = presenceMgr.getUserInfo(jedis.get(wxLoginData.code+"_AccessToken"),jedis.get(wxLoginData.code+"_OppenId"));
                if (wxUserInfo == null || wxUserInfo.getErrcode() != 0 ){
                    RedisUtils.returnResource(jedis);
                    log.error("get userinfo is fail errorCode:{},errorMsg:{}",wxUserInfo.getErrcode(),wxUserInfo.getErrmsg());
                    throw new FBWSException(FBExceptionType.PRESENCE_WXLOGIN_GETACCESSTOKEN_FAIL);
                }
                RedisUtils.returnResource(jedis);

                String oppenId = wxUserInfo.getOpenid() ;

                Player player = playerMgr.getPlayerByCert(oppenId);
                if (player == null){
                    String nickName = playerMgr.getNewNickName() ;
                    String pwd = "QWE123" ;
                    player = playerMgr.createPlayerForWX(wxUserInfo.getOpenid(),Constantes.OPPENID_TYPE, pwd,nickName,wxLoginData.lang,wxLoginData.displayLang);
                    int count = 0 ;
                    while (player == null ){
                        count++;
                        nickName = playerMgr.getNewNickName() ;
                        player = playerMgr.createPlayerForWX(wxUserInfo.getOpenid(),Constantes.OPPENID_TYPE, pwd, nickName ,wxLoginData.lang,wxLoginData.displayLang);
                        if (count>100){
                            break;
                        }
                    }

                    if (player == null ){
                        log.info("insert userinfo is fail userOppenId:{},userNickName{} count:{}",wxUserInfo.getOpenid(),wxUserInfo.getNickname(),count);
                        throw new FBWSException(FBExceptionType.PRESENCE_WXLOGIN_INSERTUSER_FAIL);
                    }

                }

                synchronized (lockLogin.getLock(oppenId)) {
                    // Set displayLang if null
                    if (wxLoginData.displayLang == null || wxLoginData.displayLang.isEmpty())
                        wxLoginData.displayLang = playerMgr.generateDisplayLang(wxLoginData.lang).toLowerCase();
                    ConnectionData conData = new ConnectionData(wxLoginData);
                    checkConnectionData(conData);

                    // get a session for this valid player:
                    FBSession session = buildSession(player, conData);
                    // RPC settings:
                    boolean forceRpcEnable = FBConfiguration.getInstance().getIntValue("presence.forceRpcEnable", 1) == 1;
                    int rpcUsePercent = FBConfiguration.getInstance().getIntValue("presence.rpcUsePercent", 100);
                    // build response
                    WXLoginResponse resp = new WXLoginResponse();
                    Privileges privileges = playerMgr.getPrivilegesForPlayer(player.getID());
                    if (privileges != null) {
                        if (!privileges.rpc && forceRpcEnable) {
                            if (rpcUsePercent > 0) {
                                int tempRandomRpc = randomRpc.nextInt(100);
                                if (tempRandomRpc < rpcUsePercent) {
                                    privileges.rpc = true;
                                }
                            }
                        }
                        session.setRpcEnabled(privileges.rpc);
                        String strPrivileges = jsonTools.transform2String(new WSPrivileges(privileges), false);
                        resp.privileges = AESCrypto.crypt(strPrivileges, Constantes.CRYPT_KEY);
                    }

                    resp.sessionID = session.getID();
                    WSPlayerInfo playerInfo = playerMgr.playerToWSPlayerInfo(session);
                    resp.playerInfo = playerInfo;
                    resp.contextInfo = presenceMgr.getContextInfo(session);
                    resp.wxUserInfo = wxUserInfo ;
                    if (log.isDebugEnabled()) {
                        log.debug("player=" + session.getPlayer().getID() + " - " + resp.contextInfo);
                    }
                    log.info("retrun msg->sesssionId:{}, playerInfo:{}, contextInfo:{}, wxUserInfo:{}, privileges:{}",resp.sessionID,resp.playerInfo.toString(),resp.contextInfo.toString(), resp.wxUserInfo.toString(), resp.privileges);
                    response.setData(resp);
                }
            } catch (FBWSException e) {
                response.setException(new FBWSExceptionRest(e));
            } catch (Exception e) {
                log.error("Exception : " + e.getMessage(), e);
                response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_INTERNAL_SERVER_ERROR));
            }
        } else {
            log.warn("Parameter not valid : param=" + param);
            response.setException(new FBWSExceptionRest(FBExceptionType.COMMON_PARAMETER_NOT_VALID));
        }
        return response;
    }
}
