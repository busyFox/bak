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
public class TreeBidInfo extends TreeMaster{
//	/**
//	 * Classe privée représentant un noeud de l'arbre
//	 * char : valeur
//	 * Node : child pour le fils
//	 * Node : brother pour le frère
//	 * @author pascal
//	 *
//	 */
//	private class NodeBidInfo {
//		private char val;
//		private NodeBidInfo child = null, brother = null;
//		private String bidInfo = null;
//	}
	
	private NodeBidInfo root = null;
	public static final String DATA_DELIMITER = " == ";
	public TreeBidInfo(String deal, String options) {
		super(deal,options);
	}
	
	/**
	 * Retourne l'information de l'enchère correspondant à la clé.
	 * @param key clé à rechercher dans l'arbre
	 * @return null si la clé n'existe pas dans l'arbre, sinon la valeur bidInfo associée à l'enchère
	 */
	public String getBidInfo(String key) {
		NodeBidInfo n = getNode(key);
		if (n != null) {
			return n.bidInfo;
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
		NodeBidInfo n = getNode(key);
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
	private NodeBidInfo getNode(String key) {
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
	private NodeBidInfo getNode(NodeBidInfo n, String key, int d) {
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
	 * @param info string à stocker dans le node associé au dernier élément de cette chaine de caractère
	 * @return true si la chaine n'existait pas dans l'arbre, false sinon
	 */
	public boolean insert(String s, String info) {
//		if (contains(s)) size++;
//		// on part de la racine et sur l'indice 0 de la chaine de caractères
//		root = insert(root, s, 0);
		if (!contains(s)) {
			int[] nbNewNode = new int[1];
			nbNewNode[0] = 0;
			// on part de la racine et sur l'indice 0 de la chaine de caractères
			root = insert(root, s, 0, nbNewNode);
			NodeBidInfo n = getNode(s);
			if (n != null) {
				n.bidInfo = info;
			}
			nbNode += nbNewNode[0];
			return true;
		}
		return false;
	}
	
	/**
	 * Insertion d'une chaine dans l'arbre. ! Méthode récursive !
	 * @param n noeud courant
	 * @param s chaine à insérer
	 * @param d position du caractère en cours
	 * @return le noeud modifié
	 */
	private NodeBidInfo insert(NodeBidInfo n, String s, int d, int[] nbNewNode) {
		// caractère courant
		char c = s.charAt(d);
		// si noeud courant null => c'est la place du nouveau noeud
		if (n == null) {
			n = new NodeBidInfo();
			n.val = c;
			nbNewNode[0]++;
		}
		
		// On n'est pas sur le bon noeud 
		if (c != n.val) {
			// le caractère courant est < à la valeur du noeud => insertion du nouveau noeud
			// le frère du nouveau noeud est le noeud courant
			if (c < n.val) {
				NodeBidInfo t = new NodeBidInfo();
				t.val = c;
				t.brother = n;
				nbNewNode[0]++;
				// on continue sur le noeud fils
				if (d < s.length()-1) {
					t.child = insert(t.child, s, d+1, nbNewNode);
				}
				// le nouveau noeud prendra la place du noeud courant
				return t;
			}
			// on continue sur le noeud frère
			else {
				n.brother = insert(n.brother, s, d, nbNewNode);
			}
		}
		// c'est le bon noeud => on continue sur le noeud fils
		else if (d < s.length()-1) {
			n.child = insert(n.child, s, d+1, nbNewNode);
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
	private void listAll(NodeBidInfo n, String s, List<String> list) {
		if (n == null) return;
		
		if (n.bidInfo != null && n.bidInfo.length() > 0) {
			list.add(s+n.val+DATA_DELIMITER+n.bidInfo);
		}
		if (n.brother != null) {
			listAll(n.brother,s, list);
		}
		if (n.child == null) {
			s += n.val;
		} else {
			s += n.val;
			listAll(n.child, s, list);
		}
	}
}
