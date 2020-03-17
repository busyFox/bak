package com.funbridge.server.message.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Element to indicate the date of read notifGroup by a player
 * @author pserent
 *
 */
@Document(collection="message_notif_group_read")
public class MessageNotifGroupRead {
	@Id
	public ObjectId ID;
	@DBRef
	@Indexed
	public MessageNotifGroup notifGroup;
	@Indexed
	public long playerID;
	@Indexed
	public long dateNotifExpiration;

	public long dateMsg;
	
	public long dateRead = 0;
	
	public boolean isRead() {
		return dateRead > 0;
	}
}
