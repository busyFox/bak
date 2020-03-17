package com.funbridge.server.player.data;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.GeoSpatialIndexed;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection="player_location")
public class PlayerLocation {
	@Id
	public ObjectId ID;
	@Indexed(unique=true)
	public long playerID; // index on the player
	@GeoSpatialIndexed
	public PlayerGeoLoc location = new PlayerGeoLoc();
	public long dateLastUpdate = 0;
	
	public class PlayerGeoLoc {
		public double longitude;
		public double latitude;
		
		public String toString() {
			return "longitude="+longitude+" - latitude="+latitude;
		}
	}
}
