package com.funbridge.server.message;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.common.FBConfiguration;
import com.funbridge.server.common.FunbridgeMgr;
import com.funbridge.server.message.data.MessagePlayer2;
import com.funbridge.server.player.PlayerMgr;
import com.funbridge.server.player.cache.PlayerCache;
import com.funbridge.server.player.data.Player;
import com.funbridge.server.presence.PresenceMgr;
import com.funbridge.server.texts.TextUIData;
import com.funbridge.server.texts.TextUIMgr;
import com.funbridge.server.tournament.data.TournamentChallenge;
import com.funbridge.server.tournament.data.TournamentSettings;
import com.funbridge.server.ws.event.*;
import com.funbridge.server.ws.tournament.WSDuelHistory;
import com.funbridge.server.ws.tournament.WSTournamentDuelResult;
import com.gotogames.common.lock.LockMgr;
import com.gotogames.common.tools.JSONTools;
import com.gotogames.common.tools.StringTools;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.util.JSON;
import org.springframework.context.annotation.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@Component(value="messageMgr")
@Scope(value="singleton")
public class MessageMgr extends FunbridgeMgr{
	@Resource(name="playerMgr")
	private PlayerMgr playerMgr = null;
	@Resource(name="presenceMgr")
	private PresenceMgr presenceMgr = null;
    @Resource(name="mongoTemplate")
    private MongoTemplate mongoTemplate;
    @Resource(name="textUIMgr")
    private TextUIMgr textUIMgr;

    private Pattern pattern;
	private JSONTools jsontools = new JSONTools();
	private LockMgr lockMsgPla = new LockMgr();

	/**
	 * Call by spring on initialisation of bean
	 */
	@PostConstruct
	@Override
	public void init() {
		try {
			pattern = Pattern.compile("\\{(.+?)\\}");
		} catch (PatternSyntaxException e) {
			log.error("Pattern not valid !",e);
		}
	}
	
	@Override
	public void startUp() {
	}
	
	/**
	 * Call by spring on destroy of bean
	 */
	@PreDestroy
	@Override
	public void destroy() {
	}
	
	public String[] getSupportedLang() {
        return textUIMgr.getSupportedLang();
	}

	
	private void addSystemVariable(Player recipient, Map<String, String> mapVarVal) {
		if (mapVarVal != null && recipient != null) {
			mapVarVal.put("PLA_PSEUDO", recipient.getNickname());
			mapVarVal.put("PLA_CREDIT", ""+recipient.getCreditAmount());
			mapVarVal.putAll(Constantes.MESSAGE_SERIE_PARAM);
		}
	}
	
	/**
	 * Replace the variables in text by the values contain in the map. Variable are like '{VAR_NAME}' and map contain pair value (VAR_NAME, value)
	 * @param msgText
	 * @param varVal
	 * @return
	 */
	private String replaceTextVariables(String msgText, Map<String, String> varVal) {
		String strReturn = msgText;
		if (msgText.indexOf("{") >= 0) {
			if (varVal != null && varVal.size() > 0) {
				// replace all token by the value
				Matcher matcher = pattern.matcher(msgText);
				StringBuffer sb = new StringBuffer();
				while (matcher.find()) {
					String val = varVal.get(matcher.group(1));
					if (val != null) {
						matcher.appendReplacement(sb, "");
						sb.append(val);
					}
				}
				matcher.appendTail(sb);
				strReturn = sb.toString();
			}
		}
		return strReturn;
	}
	
	/**
	 * Return list of WSMessage for player and sender
	 * @param player
	 * @param sender
	 * @param offset
	 * @param nbMax
	 * @return
	 */
	public List<WSMessage> getListWSMessageForPlayerAndSender(Player player, long sender, int offset, int nbMax) {
		List<WSMessage> listResult = new ArrayList<WSMessage>();
		if (player != null) {
			Player pSender = playerMgr.getPlayer(sender);
            if (pSender != null) {
                String senderPseudo = pSender.getNickname();
                boolean senderAvatar = false;

                senderAvatar = pSender.isAvatarPresent();

                List<MessagePlayer2> listMsgPla2 = listMessageBetweenPlayers(player.getID(), sender, offset, nbMax);
                if (listMsgPla2 != null  && listMsgPla2.size() > 0) {
                    for (MessagePlayer2 mp : listMsgPla2) {
                        WSMessage wmsg = null;
                        if (mp.senderID == player.getID()) {
                            wmsg = buildWSMessage(mp, player.getNickname(), player.isAvatarPresent());
                            wmsg.read = true;
                        } else {
                            wmsg = buildWSMessage(mp, senderPseudo, senderAvatar);
                        }
                        listResult.add(wmsg);
                    }
                }
            } else {
                log.error("No player for sender="+sender);
            }
		} else {
			log.error("Player is null !");
		}
		return listResult;
	}

    /**
     * List messages between these 2 players
     * @param askPlayerID
     * @param otherPlayerID
     * @param dateReset
     * @param offset
     * @param nbMax
     * @return
     */
    public List<MessagePlayer2> listMessageBetweenPlayers(long askPlayerID, long otherPlayerID, int offset, int nbMax) {
        Query q = new Query();
        Criteria criteriaRecipient = new Criteria().andOperator(
                Criteria.where("recipientID").is(askPlayerID),
                Criteria.where("senderID").is(otherPlayerID),
                Criteria.where("dateExpiration").gt(System.currentTimeMillis()),
                Criteria.where("resetRecipient").ne(true));
        Criteria criteriaSender = new Criteria().andOperator(
                Criteria.where("senderID").is(askPlayerID),
                Criteria.where("recipientID").is(otherPlayerID),
                Criteria.where("dateExpiration").gt(System.currentTimeMillis()),
                Criteria.where("resetSender").ne(true));
        q.addCriteria(new Criteria().orOperator(criteriaRecipient, criteriaSender));
        List<MessagePlayer2> listMsg = mongoTemplate.find(q, MessagePlayer2.class);
        if (listMsg != null && listMsg.size() > 0 && offset < listMsg.size()) {
            // Do the sort and offset manually, mongodb server version not supported OR operator and sort together !
            // sort on dateMsg DESC
            Collections.sort(listMsg, new Comparator<MessagePlayer2>() {
                @Override
                public int compare(MessagePlayer2 o1, MessagePlayer2 o2) {
                    if (o1.dateMsg > o2.dateMsg) {
                        return -1;
                    } else if (o1.dateMsg < o2.dateMsg) {
                        return 1;
                    }
                    return 0;
                }
            });
            // offset
            int idxEnd = offset + nbMax;
            if (idxEnd < listMsg.size()) {
                return listMsg.subList(offset, idxEnd);
            } else if (offset > 0) {
                return listMsg.subList(offset, listMsg.size());
            }
            return listMsg;
        }
        return new ArrayList<>();
    }

    /**
     * count nb message between these 2 players
     * @param askPlayerID
     * @param otherPlayerID
     * @return
     */
    public int countMessageBetweenPlayers(long askPlayerID, long otherPlayerID) {
        Query q = new Query();
        Criteria criteriaRecipient = new Criteria().andOperator(
                Criteria.where("recipientID").is(askPlayerID),
                Criteria.where("senderID").is(otherPlayerID),
                Criteria.where("dateExpiration").gt(System.currentTimeMillis()),
                Criteria.where("resetRecipient").ne(true));
        Criteria criteriaSender = new Criteria().andOperator(
                Criteria.where("senderID").is(askPlayerID),
                Criteria.where("recipientID").is(otherPlayerID),
                Criteria.where("dateExpiration").gt(System.currentTimeMillis()),
                Criteria.where("resetSender").ne(true));
        q.addCriteria(new Criteria().orOperator(criteriaRecipient, criteriaSender));
        return (int)mongoTemplate.count(q, MessagePlayer2.class);
    }

    /**
     * count nb message not read by player=recipientID and sent by player=senderID
     * @param recipientID
     * @param senderID
     * @param afterDate
     * @return
     */
    public int countMessageNotReadForPlayerAndSender(long recipientID, long senderID, long afterDate) {
        Query q = new Query();
        q.addCriteria(new Criteria().andOperator(
                Criteria.where("recipientID").is(recipientID).andOperator(Criteria.where("senderID").is(senderID)),
                Criteria.where("dateExpiration").gt(System.currentTimeMillis()),
                Criteria.where("dateMsg").gt(afterDate),
                Criteria.where("dateRead").is(0),
                Criteria.where("resetRecipient").ne(true)));
        return (int)mongoTemplate.count(q, MessagePlayer2.class);
    }

    /**
     * Count messages not read for player=recipient
     * @param recipientID
     * @return
     */
    public int countMessageNotReadForRecipient(long recipientID) {
        Query q = new Query();
        q.addCriteria(new Criteria().andOperator(
                Criteria.where("recipientID").is(recipientID),
                Criteria.where("dateExpiration").gt(System.currentTimeMillis()),
                Criteria.where("dateRead").is(0),
                Criteria.where("resetRecipient").ne(true)
                ));
        return (int)mongoTemplate.count(q, MessagePlayer2.class);
    }


	/**
     * Set messageRead by player=plaID for all message with ID in this list
     * @param plaID
     * @param listMsgID
     * @return
     */
    public boolean setMessagePlayerRead(long plaID, List<String> listMsgID) {
        try {
            if (listMsgID != null && listMsgID.size() > 0) {
                Criteria c = Criteria.where("recipientID").is(plaID).andOperator(Criteria.where("ID").in(listMsgID));
                Update update = new Update();
                update.set("dateRead", System.currentTimeMillis());
                long ts = System.currentTimeMillis();
                mongoTemplate.updateMulti(new Query(c), update, MessagePlayer2.class);
                if ((System.currentTimeMillis() - ts) > FBConfiguration.getInstance().getIntValue("general.mongoLogLimit", 1000)) {
                    log.error("Mongo update too long ! ts="+(System.currentTimeMillis() - ts));
                }
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to set message read for plaID="+plaID+" - listMsgID="+StringTools.listToString(listMsgID), e);
        }
        return false;
    }

	/**
	 * Return the number of message for player and sender
	 * @param plaID
	 * @param senderID
	 * @return
	 */
	public int getNbMessageForPlayerAndSender(long plaID, long senderID) {
        return countMessageBetweenPlayers(plaID, senderID);
	}
	
	/**
	 * Return the number of message not read by player with ID=plaID and sent by player with ID=senderID
	 * @param plaID
	 * @param senderID
	 * @param afterDate
	 * @return
	 */
	public int getNbMessageNotReadForPlayerAndSender(long plaID, long senderID, long afterDate) {
		return countMessageNotReadForPlayerAndSender(plaID, senderID, afterDate);
	}
	
	/**
	 * Return the current time stamp. A sleep time of 2 ms is do to be sure to have different TS
	 * @return
	 */
	private static synchronized long getTimeStamp() {
		try {
			Thread.sleep(2);
		} catch (InterruptedException e) {
		}
		return System.currentTimeMillis();
	}
	
	/**
	 * Return template for message with template name
	 * @param name
	 * @return
	 */
	public TextUIData getTemplate(String name) {
        return textUIMgr.getTextMsg(name);
	}
	
	/**
	 * Return all template message
	 * @return
	 */
	public List<TextUIData> listAllTemplate() {
        List<String> listTemplateName = textUIMgr.getListTemplateNameMsg();
        List<TextUIData> listTemplate = new ArrayList<>();
        for (String e : listTemplateName) {
            TextUIData temp = textUIMgr.getTextMsg(e);
            listTemplate.add(temp);
        }
        return listTemplate;
	}
	
	/**
	 * Build event challenge to send request to partner. Sender=creator, recipient=partner
	 * @param tc
	 * @return
	 */
	public Event buildEventChallengeRequest(TournamentChallenge tc, boolean reset) {
		if (tc != null && tc.getCreator() != null && tc.getPartner() != null) {
			// build a message from template
			TextUIData msgTemplate = null;
			String textBody = "";
			if (reset) {
				msgTemplate = getTemplate(Constantes.MESSAGE_TEMPLATE_EVENT_CHALLENGE_REQUEST_RESET);
			} else {
				msgTemplate = getTemplate(Constantes.MESSAGE_TEMPLATE_EVENT_CHALLENGE_REQUEST);
			}
			if (msgTemplate != null) {
				textBody = msgTemplate.getText(tc.getPartner().getDisplayLang());
			} else {
				log.error("No template found for name="+Constantes.MESSAGE_TEMPLATE_EVENT_CHALLENGE_REQUEST_RESET+" or "+Constantes.MESSAGE_TEMPLATE_EVENT_CHALLENGE_REQUEST);
			}
			if (textBody != null && textBody.length() > 0) {
				Map<String, String> mapExtra = new HashMap<String, String>();
				mapExtra.put("PARTNER_PSEUDO", tc.getCreator().getNickname());
                textBody = replaceTextVariables(textBody, mapExtra);
				// create event
				Event evt = new Event();
				evt.timestamp = System.currentTimeMillis();
				evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
				evt.receiverID = tc.getPartner().getID();
				// create data request
				EventChallengeRequestData evd = new EventChallengeRequestData();
				evd.creatorID = tc.getCreator().getID();
				evd.creatorPseudo = tc.getCreator().getNickname();
				evd.message = textBody;
				evd.challengeID = tc.getID();
				evd.currentTS = System.currentTimeMillis();
				evd.challengeExpiration = tc.getDateExpiration();
				evd.reset = reset;
				TournamentSettings ts = ContextManager.getTournamentMgr().getTournamentSettings(tc.getSettings());
				if (ts != null) {
					evd.mode = ts.mode;
					evd.theme = ts.theme;
				}
				evt.addFieldCategory(Constantes.EVENT_CATEGORY_CHALLENGE);
				// set data JSON in event
				try {
					evt.addFieldType(Constantes.EVENT_TYPE_CHALLENGE_REQUEST, jsontools.transform2String(evd, false));
					return evt;
				} catch (JsonGenerationException e) {
					log.error("JsonGenerationException to transform data="+evd,e);
				} catch (JsonMappingException e) {
					log.error("JsonMappingException to transform data="+evd,e);
				} catch (IOException e) {
					log.error("IOException to transform data="+evd,e);
				}
			} else {
				log.error("Message is null");
			}
		} else {
			log.error("Param challenge not valid tc=" + tc);
		}
		return null;
	}
	
	public Event buildEventChallengeResponse(Player sender, Player recipient, TournamentChallenge tc, boolean response) {
		// build a message from template
		TextUIData msgTemplate = null;
		String textBody = "";
		if (response) {
			msgTemplate = getTemplate(Constantes.MESSAGE_TEMPLATE_EVENT_CHALLENGE_RESPONSE_OK);
		} else {
			msgTemplate = getTemplate(Constantes.MESSAGE_TEMPLATE_EVENT_CHALLENGE_RESPONSE_KO);
		}
		textBody = msgTemplate.getText(recipient.getDisplayLang());
		if (textBody != null && textBody.length() > 0) {
			Map<String, String> mapExtra = new HashMap<String, String>();
			mapExtra.put("PARTNER_PSEUDO", sender.getNickname());
			textBody = replaceTextVariables(textBody, mapExtra);
			// create event
			Event evt = new Event();
			evt.timestamp = System.currentTimeMillis();
			evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
			evt.receiverID = recipient.getID();
			// create data request
			EventChallengeResponseData evd = new EventChallengeResponseData();
			evd.partnerID = sender.getID();
			evd.partnerPseudo = sender.getNickname();
			evd.challengeID = tc.getID();
			evd.response = response;
			evd.message = textBody;
			evt.addFieldCategory(Constantes.EVENT_CATEGORY_CHALLENGE);
			// set data JSON in event
			try {
				evt.addFieldType(Constantes.EVENT_TYPE_CHALLENGE_RESPONSE, jsontools.transform2String(evd, false));
				return evt;
			} catch (JsonGenerationException e) {
				log.error("JsonGenerationException to transform data="+evd,e);
			} catch (JsonMappingException e) {
				log.error("JsonMappingException to transform data="+evd,e);
			} catch (IOException e) {
				log.error("IOException to transform data="+evd,e);
			}
		} else {
			log.error("Failed to build message");
		}
		return null;
	}
	
	public void removeAllMessageBetweenPlayers(long playerID1, long playerID2) {
        try {
            Query q = new Query();
            if (FBConfiguration.getInstance().getIntValue("message.requestMongoUseIn", 1) == 1) {
                List listValue = new ArrayList();
                listValue.add(playerID1);
                listValue.add(playerID2);
                q.addCriteria(new Criteria().andOperator(
                        Criteria.where("recipientID").in(listValue),
                        Criteria.where("senderID").in(listValue)));
            } else {
                q.addCriteria(new Criteria().orOperator(
                        Criteria.where("recipientID").is(playerID1).andOperator(Criteria.where("senderID").is(playerID2)),
                        Criteria.where("recipientID").is(playerID2).andOperator(Criteria.where("senderID").is(playerID1))));
            }
            mongoTemplate.remove(q, MessagePlayer2.class);
        } catch (Exception e) {
            log.error("Failed to remove message between playerID1="+playerID1+" - playerID2="+playerID2, e);
        }
	}
	
	/**
	 * Remove all message for player in list
	 * @param listPlaID
	 * @return
	 */
	public boolean deleteForPlayerList(List<Long> listPlaID) {
        try {
            mongoTemplate.remove(Query.query(Criteria.where("recipientID").in(listPlaID).orOperator(Criteria.where("senderID").in(listPlaID))), MessagePlayer2.class);
            return true;
        } catch (Exception e) {
            log.error("Failed to remove message for player in list="+StringTools.listToString(listPlaID), e);
        }
        return false;
	}
	
	/**
	 * Build an event for duel request. To push to recipient. DuelHistory is build for recipient ask.
	 * @param sender
	 * @param recipient
	 * @param duelHistory
	 * @return
	 */
	public Event buildEventDuelRequest(Player sender, Player recipient, WSDuelHistory duelHistory) {
		if (sender != null && recipient != null && duelHistory != null) {
			// build a message from template
            TextUIData msgTemplate = getTemplate(Constantes.MESSAGE_TEMPLATE_EVENT_DUEL_REQUEST);
			String textBody = "";
			if (msgTemplate != null) {
				textBody = msgTemplate.getText(recipient.getDisplayLang());
			}
			if (textBody != null && textBody.length() > 0) {
				Map<String, String> mapExtra = new HashMap<String, String>();
				mapExtra.put("PARTNER_PSEUDO", sender.getNickname());
				textBody = replaceTextVariables(textBody, mapExtra);
				// create event
				Event evt = new Event();
				evt.timestamp = System.currentTimeMillis();
				evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
				evt.receiverID = recipient.getID();
				// create data request
				EventDuelData evd = new EventDuelData();
				evd.message = textBody;
				evd.type = Constantes.EVENT_TYPE_DUEL_REQUEST;
				evd.duelHistory = duelHistory;
				// set data JSON in event
				try {
					evt.addFieldCategory(Constantes.EVENT_CATEGORY_DUEL, jsontools.transform2String(evd, false));
				} catch (JsonGenerationException e) {
					log.error("JsonGenerationException to transform data="+evd,e);
				} catch (JsonMappingException e) {
					log.error("JsonMappingException to transform data="+evd,e);
				} catch (IOException e) {
					log.error("IOException to transform data="+evd,e);
				}
				return evt;
			} else {
				log.error("Failed to build message for duel request");
			}
		} else {
			log.error("Parameter is null - sender="+sender+" - recipient="+recipient+" - duelhistory="+duelHistory);
		}
		return null;
	}
	
	/**
	 * Build an event for duel answer. To push to recipient. DuelHistory is build for recipient ask.
	 * @param sender
	 * @param recipient
	 * @param duelHistory
	 * @return
	 */
	public Event buildEventDuelRequestAnswer(Player sender, Player recipient, boolean accept, WSDuelHistory duelHistory) {
		if (sender != null && recipient != null && duelHistory != null) {
			// build a message from template
            TextUIData msgTemplate = null;
			String textBody = "";
			if (accept) {
				msgTemplate = getTemplate(Constantes.MESSAGE_TEMPLATE_EVENT_DUEL_REQUEST_ACCEPT);
			} else {
				msgTemplate = getTemplate(Constantes.MESSAGE_TEMPLATE_EVENT_DUEL_REQUEST_REFUSE);
			}
			if (msgTemplate != null) {
				textBody = msgTemplate.getText(recipient.getDisplayLang());
			}
			if (textBody != null && textBody.length() > 0) {
				Map<String, String> mapExtra = new HashMap<String, String>();
				mapExtra.put("PARTNER_PSEUDO", sender.getNickname());
				textBody = replaceTextVariables(textBody, mapExtra);
				// create event
				Event evt = new Event();
				evt.timestamp = System.currentTimeMillis();
				evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
				evt.receiverID = recipient.getID();
				// create data request
				EventDuelData evd = new EventDuelData();
				evd.message = textBody;
				if (accept) {
					evd.type = Constantes.EVENT_TYPE_DUEL_ACCEPT;
				} else {
					evd.type = Constantes.EVENT_TYPE_DUEL_REFUSE;
				}
				evd.duelHistory = duelHistory;
				// set data JSON in event
				try {
					evt.addFieldCategory(Constantes.EVENT_CATEGORY_DUEL, jsontools.transform2String(evd, false));
				} catch (JsonGenerationException e) {
					log.error("JsonGenerationException to transform data="+evd,e);
				} catch (JsonMappingException e) {
					log.error("JsonMappingException to transform data="+evd,e);
				} catch (IOException e) {
					log.error("IOException to transform data="+evd,e);
				}
				return evt;
			} else {
				log.error("Failed to build message for duel request answer");
			}
		} else {
			log.error("Parameter is null - sender="+sender+" - recipient="+recipient+" - duelhistory="+duelHistory);
		}
		return null;
	}

    /**
	 * Build an event for duel update. To push to recipient. DuelHistory is build for recipient ask. No message in event, just the duel history.
	 * @param recipient
	 * @param duelHistory
     * @param duelResult
	 * @return
	 */
	public Event buildEventDuelUpdate(Player recipient, WSDuelHistory duelHistory, WSTournamentDuelResult duelResult) {
		if (recipient != null && duelHistory != null) {
			// create event
			Event evt = new Event();
			evt.timestamp = System.currentTimeMillis();
			evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
			evt.receiverID = recipient.getID();
			// create data request
			EventDuelData evd = new EventDuelData();
			evd.type = Constantes.EVENT_TYPE_DUEL_UPDATE;
			evd.duelHistory = duelHistory;
            evd.duelResult = duelResult;
			// set data JSON in event
			try {
				evt.addFieldCategory(Constantes.EVENT_CATEGORY_DUEL, jsontools.transform2String(evd, false));
			} catch (JsonGenerationException e) {
				log.error("JsonGenerationException to transform data="+evd,e);
			} catch (JsonMappingException e) {
				log.error("JsonMappingException to transform data="+evd,e);
			} catch (IOException e) {
				log.error("IOException to transform data="+evd,e);
			}
			return evt;
		} else {
			log.error("Parameter is null - recipient="+recipient+" - duelhistory="+duelHistory);
		}
		return null;
	}

    /**
     * Build an event for duel match making success. To push to recipient. DuelHistory is build for recipient ask. No message in event, just the duel history.
     * @param recipient
     * @param duelHistory
     * @return
     */
    public Event buildEventDuelMatchMaking(Player recipient, WSDuelHistory duelHistory) {
        if (recipient != null && duelHistory != null) {
            // create event
            Event evt = new Event();
            evt.timestamp = System.currentTimeMillis();
            evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
            evt.receiverID = recipient.getID();
            // create data request
            EventDuelData evd = new EventDuelData();
            evd.duelHistory = duelHistory;
            evd.type = Constantes.EVENT_TYPE_DUEL_MATCH_MAKING;
            // set data JSON in event
            try {
                evt.addFieldCategory(Constantes.EVENT_CATEGORY_DUEL, jsontools.transform2String(evd, false));
            } catch (JsonGenerationException e) {
                log.error("JsonGenerationException to transform data="+evd,e);
            } catch (JsonMappingException e) {
                log.error("JsonMappingException to transform data="+evd,e);
            } catch (IOException e) {
                log.error("IOException to transform data="+evd,e);
            }
            return evt;
        } else {
            log.error("Parameter is null - recipient="+recipient+" - duelhistory="+duelHistory);
        }
        return null;
    }
	
    public WSMessage buildWSMessage(MessagePlayer2 msg, String senderPseudo, boolean senderAvatar) {
        WSMessage wmsg = new WSMessage();
        wmsg.ID = MessagePlayer2.MSG_TYPE+"-"+msg.getIDStr();
        wmsg.category = MessageNotifMgr.MESSAGE_CATEGORY_FRIEND;
        wmsg.body = msg.text;
        wmsg.dateExpiration = msg.dateExpiration;
        wmsg.dateReceive = msg.dateMsg;
        wmsg.senderID = msg.senderID;
        wmsg.displayMask = MessageNotifMgr.MESSAGE_DISPLAY_TOP_BAR;
        wmsg.read = msg.dateRead > 0;
        wmsg.senderPseudo = senderPseudo;
        wmsg.senderAvatar = senderAvatar;
        return wmsg;
    }

    public Event buildEventGeneralDisconnect(Player recipient, String disconnectValue) {
        Event evt = new Event();
        evt.timestamp = System.currentTimeMillis();
        evt.senderID = Constantes.EVENT_PLAYER_ID_SERVER;
        evt.receiverID = recipient.getID();
        evt.addFieldCategory(Constantes.EVENT_CATEGORY_GENERAL);
        evt.addFieldType(Constantes.EVENT_TYPE_GENERAL_DISCONNECT, disconnectValue);
        return evt;
    }

    public List<WSChatHistory> getChatHistory(long playerID) {
        List<WSChatHistory> result = new ArrayList<>();
        try {
            /*
            db.message_player.aggregate([
            {$match:{$or:[{recipientID:2},{senderID:2}]}},
            {$match:{"dateExpiration":{$gt:1432634400000}}},
            {$group:{_id:0, "chat":{$addToSet:{"player":{$cond:[{$eq:["$senderID",2]},"$recipientID","$senderID"]},"text":"$text", "dateMsg":"$dateMsg","dateRead":"$dateRead", "sender":"$senderID", "reset":{$cond:[{$eq:["$senderID",2]},"$resetSender","$resetRecipient"]}}}}},
            {$unwind:"$chat"},
            {$match:{"chat.reset":{$ne:true}}},
            {$sort:{"chat.dateMsg":-1}},
            {$group:{_id:"$chat.player", "playerID":{$first:"$chat.player"}, "dateMsg":{$first:"$chat.dateMsg"},"dateRead":{$first:"$chat.dateRead"},"text":{$first:"$chat.text"}, "sender":{$first:"$chat.sender"}}},
            {$sort:{"dateMsg":-1}},
            {$limit:50}
            ])
             */
            List<BasicDBObject> listAggregate = new ArrayList<>();
            // select message for / from this player
            BasicDBObject matchMessage = new BasicDBObject("$match", JSON.parse("{$or:[{recipientID:" + playerID + "},{senderID:" + playerID + "}]}"));
            listAggregate.add(matchMessage);
            // select only message not expired
            BasicDBObject matchExpiration = new BasicDBObject("$match", JSON.parse("{dateExpiration:{$gt:" + System.currentTimeMillis() + "}}"));
            listAggregate.add(matchExpiration);
            // group on field player (senderID or recipientID)
            BasicDBObject groupPlayer = new BasicDBObject("$group", JSON.parse("" +
                    "{" +
                    "_id:0, " +
                    "'chat':{$addToSet:{'player':{$cond:[{$eq:['$senderID'," + playerID + "]},'$recipientID','$senderID']}," +
                                        "'text':'$text', " +
                                        "'dateMsg':'$dateMsg', "+
                                        "'dateRead':'$dateRead', " +
                                        "'sender':'$senderID', " +
                                        "'reset':{$cond:[{$eq:['$senderID',"+playerID+"]},'$resetSender','$resetRecipient']}}}" +
                    "}"));
            listAggregate.add(groupPlayer);
            // explode chat array
            BasicDBObject unwind = new BasicDBObject("$unwind", "$chat");
            listAggregate.add(unwind);
            // match to exclude message with reset
            BasicDBObject matchReset = new BasicDBObject("$match", new BasicDBObject("chat.reset", new BasicDBObject("$ne", true)));
            listAggregate.add(matchReset);
            // sort on dateMsg DESC
            BasicDBObject sortMessage = new BasicDBObject("$sort", new BasicDBObject("chat.dateMsg", -1));
            listAggregate.add(sortMessage);
            // group on chat player and on first message (most recent)
            BasicDBObject groupMessage = new BasicDBObject("$group", JSON.parse("" +
                    "{" +
                    "_id:'$chat.player', " +
                    "'playerID':{$first:'$chat.player'}, " +
                    "'dateMsg':{$first:'$chat.dateMsg'}, " +
                    "'dateRead':{$first:'$chat.dateRead'}, " +
                    "'text':{$first:'$chat.text'}, " +
                    "'sender':{$first:'$chat.sender'}" +
                    "}"));
            listAggregate.add(groupMessage);
            // sort chat on dateMsg DESC
            BasicDBObject sortChat = new BasicDBObject("$sort", new BasicDBObject("dateMsg", -1));
            listAggregate.add(sortChat);
            // limit nb result
            BasicDBObject limit = new BasicDBObject("$limit", FBConfiguration.getInstance().getIntValue("message.chatHistoryLimit", 50));
            listAggregate.add(limit);

            // run aggregation
            AggregateIterable<BasicDBObject> aggOut = mongoTemplate.getCollection("message_player").aggregate(listAggregate, BasicDBObject.class);

            for (DBObject e : aggOut) {
                WSChatHistory c = new WSChatHistory();
                c.playerID = (Long) e.get("playerID");
                c.lastMessageDate = (Long) e.get("dateMsg");
                c.lastMessageText = (String) e.get("text");
                if ((Long)e.get("sender") != playerID) {
                    c.lastMessageRead = (Long) e.get("dateRead") > 0;
                } else {
                    c.lastMessageRead = true;
                }
                PlayerCache pc = ContextManager.getPlayerCacheMgr().getPlayerCache(c.playerID);
                if (pc != null) {
                    c.pseudo = pc.getPseudo();
                    c.avatar = pc.avatarPublic;
                    c.connected = presenceMgr.isSessionForPlayerID(c.playerID);
                    c.countryCode = pc.countryCode;
                    result.add(c);
                }
            }
        } catch (Exception e) {
            log.error("Failed to aggregate message for player="+playerID, e);
        }
        return result;
    }
}

