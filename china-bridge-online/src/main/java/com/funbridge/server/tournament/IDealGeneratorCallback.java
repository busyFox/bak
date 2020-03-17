package com.funbridge.server.tournament;

public interface IDealGeneratorCallback {
	/**
	 * Check if distribution already exists
	 * @param cards
	 * @return
	 */
	boolean distributionExists(String cards);
}
