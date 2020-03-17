package com.funbridge.server.message.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Element to indicate the date of read notifAll by a player
 * @author pserent
 *
 */
@Document(collection="message_notif_all_read")
public class MessageNotifAllRead {
	@Id
	public ObjectId ID;
	@DBRef
	@Indexed
	public MessageNotifAll notifAll;
	@Indexed
	public long playerID;
	@Indexed
	public long dateNotifExpiration;
	
	public long dateRead = 0;

    public Date creationDateISO = null;
	
	public boolean isRead() {
		return dateRead > 0;
	}
}
