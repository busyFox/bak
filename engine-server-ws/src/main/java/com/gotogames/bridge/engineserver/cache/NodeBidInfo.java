package com.gotogames.bridge.engineserver.cache;

/**
 * Classe privée représentant un noeud de l'arbre
 * char : valeur
 * Node : child pour le fils
 * Node : brother pour le frère
 * bidInfo : bid info data
 * @author pascal
 */
public class NodeBidInfo {
    public char val;
    public NodeBidInfo child = null, brother = null;
    public String bidInfo = null;
}
