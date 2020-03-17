package com.funbridge.server.tournament.data;

import javax.xml.bind.annotation.XmlRootElement;

import com.gotogames.common.bridge.BridgeDealParam;

@XmlRootElement(name="dealSettings")
public class DealSettings {
	public DealPlayerSettings north;
	public DealPlayerSettings east;
	public DealPlayerSettings south;
	public DealPlayerSettings west;
	
	public String toString() {
		return "north={"+north+"} - east={"+east+"} - south={"+south+"} - west={"+west+"}";
	}
	
	public boolean isValid() {
        return north != null && north.isValid() &&
                east != null && east.isValid() &&
                south != null && south.isValid() &&
                west != null && west.isValid();
    }
	
	public BridgeDealParam toBridgeDealParam() {
		if (isValid()) {
			BridgeDealParam bdp = new BridgeDealParam();
			// North
			int idxNorth = BridgeDealParam.INDEX_N;
			bdp.nbCardCMax[idxNorth] = north.maxs.nbC;
			bdp.nbCardCMin[idxNorth] = north.mins.nbC;
			bdp.nbCardDMax[idxNorth] = north.maxs.nbD;
			bdp.nbCardDMin[idxNorth] = north.mins.nbD;
			bdp.nbCardHMax[idxNorth] = north.maxs.nbH;
			bdp.nbCardHMin[idxNorth] = north.mins.nbH;
			bdp.nbCardSMax[idxNorth] = north.maxs.nbS;
			bdp.nbCardSMin[idxNorth] = north.mins.nbS;
			bdp.ptsHonMax[idxNorth] = north.maxs.nbPoints;
			bdp.ptsHonMin[idxNorth] = north.mins.nbPoints;
			// East
			int idxEast = BridgeDealParam.INDEX_E;
			bdp.nbCardCMax[idxEast] = east.maxs.nbC;
			bdp.nbCardCMin[idxEast] = east.mins.nbC;
			bdp.nbCardDMax[idxEast] = east.maxs.nbD;
			bdp.nbCardDMin[idxEast] = east.mins.nbD;
			bdp.nbCardHMax[idxEast] = east.maxs.nbH;
			bdp.nbCardHMin[idxEast] = east.mins.nbH;
			bdp.nbCardSMax[idxEast] = east.maxs.nbS;
			bdp.nbCardSMin[idxEast] = east.mins.nbS;
			bdp.ptsHonMax[idxEast] = east.maxs.nbPoints;
			bdp.ptsHonMin[idxEast] = east.mins.nbPoints;
			// South
			int idxSouth = BridgeDealParam.INDEX_S;
			bdp.nbCardCMax[idxSouth] = south.maxs.nbC;
			bdp.nbCardCMin[idxSouth] = south.mins.nbC;
			bdp.nbCardDMax[idxSouth] = south.maxs.nbD;
			bdp.nbCardDMin[idxSouth] = south.mins.nbD;
			bdp.nbCardHMax[idxSouth] = south.maxs.nbH;
			bdp.nbCardHMin[idxSouth] = south.mins.nbH;
			bdp.nbCardSMax[idxSouth] = south.maxs.nbS;
			bdp.nbCardSMin[idxSouth] = south.mins.nbS;
			bdp.ptsHonMax[idxSouth] = south.maxs.nbPoints;
			bdp.ptsHonMin[idxSouth] = south.mins.nbPoints;
			// West
			int idxWest = BridgeDealParam.INDEX_W;
			bdp.nbCardCMax[idxWest] = west.maxs.nbC;
			bdp.nbCardCMin[idxWest] = west.mins.nbC;
			bdp.nbCardDMax[idxWest] = west.maxs.nbD;
			bdp.nbCardDMin[idxWest] = west.mins.nbD;
			bdp.nbCardHMax[idxWest] = west.maxs.nbH;
			bdp.nbCardHMin[idxWest] = west.mins.nbH;
			bdp.nbCardSMax[idxWest] = west.maxs.nbS;
			bdp.nbCardSMin[idxWest] = west.mins.nbS;
			bdp.ptsHonMax[idxWest] = west.maxs.nbPoints;
			bdp.ptsHonMin[idxWest] = west.mins.nbPoints;
			return bdp;
		}
		return null;
	}
}
