package com.gotogames.bridge.engineserver.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * Arbre N-aire
 * Chaque noeud possède un lien sur un frère et un lien sur un fils.
 * 
 * Exemple de l'arbre suivant:
 * 
 *     	   a
 *  	/  |  \
 *     a   b   e
 * 	   |  / \  |
 *	   b c   d a
 * 
 * Représentation n-aire correspondante
 * 
 *		a
 * 		|
 * 		a - b  -   e
 * 		|   |      |
 * 		b   c - d  a
 * 
 * 
 * La méthode d'insertion tient compte de la valeur des caractères et fait en sorte que les frères des noeuds soient chainés du plus petit au plus grand
 * Arbre avec les mots abc et ac
 *	 a
 *	 |
 *	 b - c
 *	 |
 *	 c
 * 
 * Si on ajoute le mot aa on aura
 * 
 * 	a						a
 * 	|						|
 * 	a - b - c	au lieu de 	b - c - a
 *      |					|
 *      c					c
 *      
 * Cela permet d'optimiser la recherche
 * @author pascal
 *
 */
public class TreeBidCard extends TreeMaster{
//	/**
//	 * Classe privée représentant un noeud de l'arbre
//	 * char : valeur
//	 * Node : child pour le fils
//	 * Node : brother pour le frère
//	 * @author pascal
//	 *
//	 */
//	private class Node {
//		private char val;
//		private Node child = null, brother = null;
//		private boolean isRobot = false;
//        private boolean isBidAlert = false;
//        private boolean isSpreadEnable = false;
//	}
	
	private Node root = null;

	public TreeBidCard(String deal, String options) {
		super(deal,options);
	}

	
	/**
	 * Retourne la suite de caractère qui suit cette clé. Chaque caractère représente un coup joué par un robot.
	 * Chaque noeud comporte un seul fils et il doit etre de type robot.
	 * @param key clé à rechercher dans l'arbre
	 * @return null si la clé n'existe pas dans l'arbre ou que le coup n'est pas joué par un robot, 
	 * chaine vide si la clé existe mais comporte 0 ou plusieurs (>1) fils 
	 * chaine de caractère représentant la valeur des noeuds fils uniques
	 */
	public String getSuffixRobot(String key) {
		Node n = getNode(key);
		if (n != null) {
			StringBuffer sb = new StringBuffer();
			Node current = n;
			// recherche sur l'ensemble des noeud fils uniques
			while (current.child != null && current.child.brother == null && current.child.isRobot) {
				sb.append(current.child.val);
				current = current.child;
			}
			return sb.toString();
		}
		return null;
	}

    public String getValueRobot(String key) {
        Node n = getNode(key);
        if (n != null) {
            StringBuffer sb = new StringBuffer();
            Node current = n;
            // recherche sur l'ensemble des noeud fils uniques
            while (current.child != null && current.child.brother == null && current.child.isRobot) {
                sb.append(current.child.val);
                if (current.child.isBidAlert || current.child.isSpreadEnable) {
                    sb.append("1");
                } else {
                    sb.append("0");
                }
                current = current.child;
            }
            return sb.toString();
        }
        return null;
    }
	
	/**
	 * Retourne le caractère suivant qui suit cette clé. si la clé n'est pas trouvée ou que plusieurs réponses sont possibles, le caractère resultNotFound est retourné
	 * @param key
	 * @param resultNotFound
	 * @return le caractère suivant si la clé est trouvée sinon resultNotFound
	 */
	public char getNext(String key, char resultNotFound) {
		Node n = getNode(key);
		if (n != null) {
			if (n.child != null && n.child.brother == null) {
				return n.child.val;
			}
		}
		return resultNotFound;
	}
	
	/**
	 * retourne le noeud associé à cette chaine
	 * @param key
	 * @return
	 */
	private Node getNode(String key) {
		if (key == null || key.length() == 0) {
			return null;
		}
		return getNode(root, key, 0);
	}
	
	/**
	 * Méthode récursive pour retrouver le noeud correspondant à la chaine
	 * @param n noeud ocurant
	 * @param key chaine complète à rechercher
	 * @param d indice du caractère courent
	 * @return le noeud du bout
	 */
	private Node getNode(Node n, String key, int d) {
		if (n == null) {
			return null;
		}
		char c = key.charAt(d);
		// noeud courant ne correspond pas au caractère courant
		if (c != n.val) {
			// si le caractère courant est supérieur au noeud courant => recherche sur le frère
			// on se limite à la valeur supérieure car l'insertion ordonne les noeuds
			if (c > n.val){ 
				return getNode(n.brother, key, d);
			}
			// chaine non trouvée
			return null;
		}
		// noeud courant = caractère courant et non fin de chaine => recherche sur le noeud fils 
		else if (d < key.length() - 1){
			return getNode(n.child, key, d+1);
		}
		// fin de chaine et noeud courant = caractère courant => TROUVE
		else {
			return n;
		}
	}
	
	/**
	 * Indique si la chaine est présente dans l'arbre
	 * @param key
	 * @return true si la chaine est présenter sinon false
	 */
	public boolean contains(String key) {
		return getNode(key) != null;
	}
	
	/**
	 * Insertion de la chaine de caractère dans l'arbre.
	 * @param s
     * @param lastBidAlert
     * @param lastCardSpread
	 * @return true si la chaine n'existait pas dans l'arbre, false sinon
	 */
	public boolean insert(String s, boolean lastBidAlert, boolean lastCardSpread) {
//		if (contains(s)) size++;
//		// on part de la racine et sur l'indice 0 de la chaine de caractères
//		root = insert(root, s, 0);
		Node n = getNode(s);
		boolean bResult = false;
		// node not existing => insert it!
		if (n == null) {
			int[] nbNewNode = new int[1];
			nbNewNode[0] = 0;
			// on part de la racine et sur l'indice 0 de la chaine de caractères
			root = insert(root, s, 0, nbNewNode, lastBidAlert, lastCardSpread);
			bResult = true;
			nbNode += nbNewNode[0];
		}
		// already existing in the tree
		else {
			// update the status robot (to indicate that this result has been computed by robot)
			if (!n.isRobot) {
				n.isRobot = true;
				bResult = true;
			}
            if ((!n.isBidAlert && lastBidAlert) || (n.isBidAlert && !lastBidAlert)) {
                n.isBidAlert = lastBidAlert;
                bResult = true;
            }
            if ((!n.isSpreadEnable && lastCardSpread) || (n.isSpreadEnable && !lastCardSpread)) {
			    n.isSpreadEnable = lastCardSpread;
			    bResult = true;
            }
		}
		return bResult;
	}
	
	/**
	 * Insertion d'une chaine dans l'arbre. ! Méthode récursive !
	 * @param n noeud courant
	 * @param s chaine à insérer
	 * @param d position du caractère en cours
     * @param nbNewNode nb new node created
     * @param lastBidAlert the last bid is alert
     * @param lastCardSpread the last card has spread flag
	 * @return le noeud modifié
	 */
	private Node insert(Node n, String s, int d, int[] nbNewNode, boolean lastBidAlert, boolean lastCardSpread) {
		// caractère courant
		char c = s.charAt(d);
		// si noeud courant null => c'est la place du nouveau noeud
		if (n == null) {
			n = new Node();
			n.val = c;
			nbNewNode[0]++;
            if (d == (s.length()-1)) {
                n.isBidAlert = lastBidAlert;
                n.isSpreadEnable = lastCardSpread;
            }
		}
		
		// On n'est pas sur le bon noeud 
		if (c != n.val) {
			// le caractère courant est < à la valeur du noeud => insertion du nouveau noeud
			// le frère du nouveau noeud est le noeud courant
			if (c < n.val) {
				Node t = new Node();
				t.val = c;
				t.brother = n;
				nbNewNode[0]++;
				// on continue sur le noeud fils
				if (d < s.length()-1) {
					t.child = insert(t.child, s, d+1, nbNewNode, lastBidAlert, lastCardSpread);
				} else {
                    t.isBidAlert = lastBidAlert;
                    t.isSpreadEnable = lastCardSpread;
                }
				// le nouveau noeud prendra la place du noeud courant
				return t;
			}
			// on continue sur le noeud frère
			else {
				n.brother = insert(n.brother, s, d, nbNewNode, lastBidAlert, lastCardSpread);
			}
		}
		// c'est le bon noeud => on continue sur le noeud fils
		else if (d < s.length()-1) {
			n.child = insert(n.child, s, d+1, nbNewNode, lastBidAlert, lastCardSpread);
		} else {
			n.isRobot = true;
		}
		return n;
	}
	
	/**
	 * List all value contained in this tree
	 * @return
	 */
	public List<String> listAll() {
		List<String> list = new ArrayList<String>();
		listAll(root, "", list);
		return list;
	}
	
	/**
	 * Recursive method to list value in a tree
	 * @param n
	 * @param s
	 * @param list
	 */
	private void listAll(Node n, String s, List<String> list) {
		if (n == null) return;
		
		// on part sur les frères
		if (n.brother != null) {
			listAll(n.brother,s, list);
		}
		// si pas de fils alors ajout du caractère courant au string et ajout du string dans la liste
		if (n.child == null) {
			s += n.val+(n.isRobot?"1":"0");
			list.add(s);
		}
		// au moins un fils alors ajout du caractère courant au string et on continue sur le fils
		else {
			s += n.val+(n.isRobot?"1":"0");
			listAll(n.child, s, list);
		}
	}
}
