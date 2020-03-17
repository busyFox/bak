package com.gotogames.bridge.engineserver.cache;

import java.util.List;

public abstract class TreeMaster {
	protected int nbNode = 0;
	private long tsLastConsult = 0, tsLastAdd = 0;
	private String deal, options;
	
	public TreeMaster(String deal, String options) {
		this.deal = deal;
		this.options = options;
	}
	
	public abstract List<String> listAll();
	
	public String getTreeDealKey() {
		return deal+options;
	}
	
	public String getDeal() {
		return deal;
	}
	
	public String getOptions() {
		return options;
	}
	
	public long getTsLastConsult() {
		return tsLastConsult;
	}

	public void setTsLastConsult(long tsLastConsult) {
		this.tsLastConsult = tsLastConsult;
	}

	public long getTsLastAdd() {
		return tsLastAdd;
	}

	public void setTsLastAdd(long tsLastAdd) {
		this.tsLastAdd = tsLastAdd;
	}

	/**
	 * Retourne le nombre de noeud contenu dans l'arbre
	 * @return
	 */
	public int getNbNode() {
		return nbNode;
	}
}
