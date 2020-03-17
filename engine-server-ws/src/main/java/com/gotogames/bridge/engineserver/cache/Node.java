package com.gotogames.bridge.engineserver.cache;

/**
 * Classe privée représentant un noeud de l'arbre
 * char : valeur
 * Node : child pour le fils
 * Node : brother pour le frère
 * @author pascal
 */
public class Node {
    public char val;
    public Node child = null, brother = null;
    public boolean isRobot = false;
    public boolean isBidAlert = false;
    public boolean isSpreadEnable = false;
}
