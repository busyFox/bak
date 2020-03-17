package com.gotogames.common.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class GameClaim {

	/**
	 * Compute the number of tricks than player1 can make assuming player2 has
	 * j2 as cards
	 * 
	 * @param j1
	 *            : list of cards of j1
	 * @param j2
	 *            : list of cards of j2
	 * @return the number of tricks makeable by j1
	 * */
	public static int getNbWinningCardColor(List<BridgeCard> j1,List<BridgeCard> j2) {
		int res = 0;
		/* If j1 has 0 card => return 0 */
		if (j1.size() == 0) {
			return 0;
		}
		/*
		 * Else we have to find the number of cards of j1 greater than all the
		 * cards of j2
		 */
		else {
			/* If j2 has 0 card => return nb of cards of j1 */
			if (j2.size() == 0) {
				return j1.size();
			} else {
				/* We get the best card of j2 */
				BridgeCard best_card_j2 = j2.get(0);
				int iter2 = j2.size();
				for (int i = 0; i < iter2; i++) {
					if (j2.get(i).getValue().getVal() > best_card_j2.getValue().getVal()) {
						best_card_j2 = j2.get(i);
					}
				}

				/*
				 * For each card of j1, we check that it is better than the best
				 * card of j2
				 */
				int iter1 = j1.size();
				for (int i = 0; i < iter1; i++) {
					if (j1.get(i).getValue().getVal() > best_card_j2.getValue().getVal()) {
						res++;
					}
				}

				/* If all the cards of j1 are established */
				if (res >= j2.size()) {
					return j1.size();
				} 
				else 
				{
					return res;
				}
			}

		}
	}

	/**
	 * Compute the number of cards of player1 which are smaller than all the
	 * cards of j2
	 * 
	 * @param j1
	 *            : list of cards of j1
	 * @param j2
	 *            : list of cards of j2
	 * @return the number of submissive cards of j1
	 * */
	public static int getNbSubmissiveCardColor(List<BridgeCard> j1,List<BridgeCard> j2) {
		int res = 0;
		/* If j1 has 0 card => return 0 */
		if (j1.size() == 0) {
			return 0;
		}
		/*
		 * Else we have to find the number of cards of j1 smaller than all the
		 * cards of j2
		 */
		else {
			/* If j2 has 0 card => return 0 */
			if (j2.size() == 0) {
				return 0;
			} else {
				/* We get the worst card of j2 */
				BridgeCard worst_card_j2 = j2.get(0);
				int iter2 = j2.size();
				for (int i = 0; i < iter2; i++) {
					if (j2.get(i).getValue().getVal() < worst_card_j2.getValue().getVal()) {
						worst_card_j2 = j2.get(i);
					}
				}

				/*
				 * For each card of j1, we check that it is better than the best
				 * card of j2
				 */
				int iter1 = j1.size();
				for (int i = 0; i < iter1; i++) {
					if (j1.get(i).getValue().getVal() < worst_card_j2.getValue().getVal()) {
						res++;
					}
				}

				return res;
			}

		}
	}

	/**
	 * Compute the number of immediate losers of player 1
	 * 
	 * @param j1
	 *            : list of cards of j1 (declarer)
	 * @param n1
	 *            : trump's number of j1
	 * @param j2
	 *            : list of cards of j2 (dummy)
	 * @param n2
	 *            : trump's number of j2
	 * @param j3
	 *            : list of cards of j3 (opponent)
	 */
	public static int getNbImmediateLoserColor(List<BridgeCard> j1IN, int n1,List<BridgeCard> j2IN, int n2, List<BridgeCard> j3IN) {
		int res = 0;
		boolean continuer = false;
		List<BridgeCard> j1 = new ArrayList<BridgeCard>(j1IN);
		List<BridgeCard> j2 = new ArrayList<BridgeCard>(j2IN);
		List<BridgeCard> j3 = new ArrayList<BridgeCard>(j3IN);

		int nb1 = j1.size();
		int nb2 = j2.size();

		/*
		 * Rule : We check that j3 has still a winning card. If it's the case,
		 * we remove the best card between j1 and j2 and the worst in the other
		 * hand.
		 */
		do {
			/* Check j1 and/or j2 has card */
			if (j1.size() == 0 && j2.size() == 0) {
				res += j3.size();
				break;
			}

			/* Check j3 has card */
			if (j3.size() == 0) {
				break;
			}

			/* Get the index of the best/worst card of j1 */
			BridgeCard best_card_j1 = null;
			BridgeCard worst_card_j1 = null;
			int iter1 = j1.size();
			if (iter1 > 0) {
				best_card_j1 = j1.get(0);
				worst_card_j1 = j1.get(0);
				for (int i = 1; i < iter1; i++) {
					if (j1.get(i).getValue().getVal() < worst_card_j1.getValue().getVal()) {
						worst_card_j1 = j1.get(i);
					} else {
						if (j1.get(i).getValue().getVal() > best_card_j1.getValue().getVal()) {
							best_card_j1 = j1.get(i);
						}
					}
				}
			}

			/* Get the index of the best/worst card of j2 */
			BridgeCard best_card_j2 = null;
			BridgeCard worst_card_j2 = null;
			int iter2 = j2.size();
			if (iter2 > 0) {
				best_card_j2 = j2.get(0);
				worst_card_j2 = j2.get(0);
				for (int i = 1; i < iter2; i++) {
					if (j2.get(i).getValue().getVal() < worst_card_j2.getValue().getVal()) {
						worst_card_j2 = j2.get(i);
					} else {
						if (j2.get(i).getValue().getVal() > best_card_j2.getValue().getVal()) {
							best_card_j2 = j2.get(i);
						}
					}
				}
			}

			/* Get the index of the best card of j3 */
			BridgeCard best_card_j3 = null;
			int iter3 = j3.size();
			if (iter3 > 0) {
				best_card_j3 = j3.get(0);
				for (int i = 1; i < iter3; i++) {
					if (j3.get(i).getValue().getVal() > best_card_j3.getValue().getVal()) {
						best_card_j3 = j3.get(i);
					}
				}
			}

			/* If the best card of j3 is not a winning card, we leave */
			if (j1.size() == 0 && (best_card_j3.getValue().getVal() < best_card_j2.getValue().getVal())) {
				break;
			}
			if (j2.size() == 0 && (best_card_j3.getValue().getVal() < best_card_j1.getValue().getVal())) {
				break;
			}
			if (j2.size() > 0
				&& j1.size() > 0
				&& ((best_card_j3.getValue().getVal() < best_card_j1.getValue().getVal())
					|| (best_card_j3.getValue().getVal() < best_card_j2.getValue().getVal()))) {
				break;
			}
			/* Otherwise we have a new loser, and have to update the data */

			res++;

			/*
			 * Si j1 et j2 ont le meme nombre de cartes, on enlève la carte la
			 * plus forte, sinon en cas de cartes équivalentes, on fait tomber
			 * la meilleure carte du jeu qui a le moins de cartes
			 */
			if (j1.size() == j2.size()) {
				/* Si j1 a la meilleure carte */
				if (best_card_j1.getValue().getVal() > best_card_j2.getValue().getVal()) {
					/* On enleve la meilleur carte de j1 */
					j1.remove(best_card_j1);

					/* On enleve la pire carte de j2 */
					j2.remove(worst_card_j2);

					/* On enleve la meilleure carte de j3 */
					j3.remove(best_card_j3);
				}
				/* Si j2 a la meilleure carte */
				else {
					/* On enleve la pire carte de j1 */
					j1.remove(worst_card_j1);

					/* On enleve la meilleure carte de j2 */
					j2.remove(best_card_j2);

					/* On enleve la meilleure carte de j3 */
					j3.remove(best_card_j3);
				}
			} else {
				/*
				 * On regarde si la meilleure carte de j1 et la meilleur carte
				 * de j2 sont équivalentes
				 */
				boolean equivalente = true;
				if (j1.size() > 0 && j2.size() > 0) {
					for (int j = 0; j < iter3; j++) {
						if (((best_card_j1.getValue().getVal() < j3.get(j).getValue().getVal())
								&& (best_card_j2.getValue().getVal() > j3.get(j).getValue().getVal()))
							|| ((best_card_j1.getValue().getVal() > j3.get(j).getValue().getVal()) 
									&& (best_card_j2.getValue().getVal() < j3.get(j).getValue().getVal()))) {
							equivalente = false;
							break;
						}
					}
				} else {
					equivalente = false;
				}

				/* Si les deux cartes sont equivalentes */
				if (equivalente) {
					/* On enleve la meilleur carte de j1 */
					if (j1.size() < j2.size()) {
						/* On enleve la meilleur carte de j1 */
						j1.remove(best_card_j1);

						/* On enleve la pire carte de j2 */
						j2.remove(worst_card_j2);

						/* On enleve la meilleure carte de j3 */
						j3.remove(best_card_j3);
					}
					/* On enleve la meilleur carte de j2 */
					else {
						/* On enleve la pire carte de j1 */
						j1.remove(worst_card_j1);

						/* On enleve la meilleure carte de j2 */
						j2.remove(best_card_j2);

						/* On enleve la meilleure carte de j3 */
						j3.remove(best_card_j3);
					}
				} else {
					if (j1.size() == 0 || j2.size() == 0) {
						if (j1.size() == 0) {
							/* On enleve la meilleur carte de j1 */
							j1.remove(best_card_j1);

							/* On enleve la meilleure carte de j3 */
							j3.remove(best_card_j3);
						} else {
							/* On enleve la meilleur carte de j2 */
							j1.remove(best_card_j2);

							/* On enleve la meilleure carte de j3 */
							j3.remove(best_card_j3);
						}

					} else {
						/* Si j1 a la meilleure carte */
						if (best_card_j1.getValue().getVal() > best_card_j2.getValue().getVal()) {
							/* On enleve la meilleur carte de j1 */
							j1.remove(best_card_j1);

							/* On enleve la pire carte de j2 */
							j2.remove(worst_card_j2);

							/* On enleve la meilleure carte de j3 */
							j3.remove(best_card_j3);
						}
						/* Si j2 a la meilleure carte */
						else {
							/* On enleve la pire carte de j1 */
							j1.remove(worst_card_j1);

							/* On enleve la meilleure carte de j2 */
							j2.remove(best_card_j2);

							/* On enleve la meilleure carte de j3 */
							j3.remove(best_card_j3);
						}

					}

				}
			}

			/* On continue */
			continuer = true;

		} while (continuer);

		/* If neither j1 nor j2 have trump, we lose 'res' tricks */
		if (n1 == 0 && n2 == 0) {
			return res;
		}
		/*
		 * If j2 has no trump but j1 has got, we lose the min betwwen
		 * 'j1.size()' and 'res' tricks
		 */
		if (n2 == 0) {
			return Math.min(nb1, res);
		}
		/*
		 * If j1 has no trump but j2 has got, we lose the min betwwen
		 * 'j2.size()' and 'res' tricks
		 */
		if (n2 == 0) {
			return Math.min(nb2, res);
		}
		/* Otherwise we lose the min betwwen 'j1.size()','j2.size()' and 'res' */
		return Math.min(Math.min(nb1, nb2), res);
	}

	public static int getNbUselessCard(List<BridgeCard> j1IN,List<BridgeCard> j2IN, List<BridgeCard> j3IN) {
		
		List<BridgeCard> j1 = new ArrayList<BridgeCard>(j1IN);
		List<BridgeCard> j2 = new ArrayList<BridgeCard>(j2IN);
		List<BridgeCard> j3 = new ArrayList<BridgeCard>(j3IN);


		/* Get the index of the bestcard of j1 */
		BridgeCard best_card_j1 = null;
		int iter1 = j1.size();
		if (iter1 > 0) {
			best_card_j1 = j1.get(0);
			for (int i = 1; i < iter1; i++) {
				if (j1.get(i).getValue().getVal() > best_card_j1.getValue()
						.getVal()) {
					best_card_j1 = j1.get(i);
				}
			}
		}

		/* Get the index of the worst card of j2 */
		BridgeCard worst_card_j2 = null;
		int iter2 = j2.size();
		if (iter2 > 0) {
			worst_card_j2 = j2.get(0);
			for (int i = 1; i < iter2; i++) {
				if (j2.get(i).getValue().getVal() < worst_card_j2.getValue().getVal()) {
					worst_card_j2 = j2.get(i);
				}
			}
		}

		int iter3 = j3.size();
		/*
		 * On regarde si la meilleure carte de j1 et la pire carte de j2
		 * sont �quivalentes
		 */
		boolean equivalente = true;
		if (j1.size() > 0 && j2.size() > 0) {
			if (best_card_j1.getValue().getVal() > worst_card_j2.getValue().getVal()) {
				for (int j = 0; j < iter3; j++) {
					if (((worst_card_j2.getValue().getVal() < j3.get(j).getValue().getVal()) && (best_card_j1.getValue().getVal() > j3.get(j).getValue().getVal()))) {
						equivalente = false;
						break;
					}
				}
			}
		} 
		else 
		{
			equivalente = false;
		}
		
		
		Blocage b = getBlocage(j1IN,j2IN,j3IN);
		
		int n_j1_j3 = getNbWinningCardColor(j1IN,j3IN);
		int n_j2_j3 = getNbWinningCardColor(j2IN,j3IN);
		
		int ns_j1_j2 = getNbSubmissiveCardColor(j1IN,j2IN);
		int ns_j1_j3 = getNbSubmissiveCardColor(j1IN,j3IN);
		
		int n = getNbTricksColor(j1IN,j2IN,j3IN,b);
		
		if( n > 0 )
		{
			if(n_j2_j3 == 0)
			{
				return Math.max(j1IN.size() - n, 0);
			}
			
			if(ns_j1_j2 == j1IN.size() && j1IN.size() <= j2IN.size())
			{
				return j1IN.size() - 1;
			}
			
			if(n_j1_j3 + n_j2_j3 <= n)
			{
				return j1IN.size() - n_j1_j3 - 1;
			}
			
			if( b == Blocage._Niveau_0 || b == Blocage._Niveau_1_A || b == Blocage._Niveau_1_B)
			{
				return j1IN.size() - (n-n_j2_j3) - 1;
			}
			return 0;
		}
		
		if( ns_j1_j3 == j1IN.size())
		{
			return j1IN.size(); 
		}
		
		if( (ns_j1_j3 == j3IN.size()))
		{
			return j1IN.size(); 
		}
		
		if(equivalente && j1IN.size() <= j2IN.size())
		{
			return j1IN.size();
		}
		
		if(j1IN.size() > j3IN.size())
		{
			return j1IN.size() - j3IN.size();
		}
		
		return 0;
	}

	public static Blocage getBlocage(List<BridgeCard> j1IN,List<BridgeCard> j2IN, List<BridgeCard> j3IN) {

		int n_j1_j2 = getNbWinningCardColor(j1IN, j2IN);
		int ns_j1_j2 = getNbSubmissiveCardColor(j1IN, j2IN);
		int n_j2_j1 = getNbWinningCardColor(j2IN, j1IN);
		int ns_j2_j1 = getNbSubmissiveCardColor(j2IN, j1IN);
		int n_j1_j3 = getNbWinningCardColor(j1IN, j3IN);
		int n_j2_j3 = getNbWinningCardColor(j2IN, j3IN);

		/* At least one player has a void */
		if (j1IN.size() == 0 || j2IN.size() == 0) {
			/* If the other player has winning cards => Blocage = _Niveau_5 */
			if (n_j1_j3 + n_j2_j3 > 0) {
				if (n_j1_j3 > 0) {
					return Blocage._Niveau_5_A;
				} else {
					return Blocage._Niveau_5_B;
				}
			}
			/* Otherwise Blocage = _Niceau_6 */
			else {
				return Blocage._Niveau_6;
			}

		}

		/* All the cards of j1 are greater than the cards of j2 */
		if (n_j1_j2 == j1IN.size() && ns_j1_j2 == 0) {
			/* If j1 has no winning card => Blocage = _Niveau_6 */
			if (n_j1_j3 == 0) {
				return Blocage._Niveau_6;
			}
			/* BEGIN MODIF JR */
			if ((n_j1_j3 != n_j1_j2) && (j1IN.size() > j2IN.size())) {
				return Blocage._Niveau_1_A;
			}
			/* END MODIF JR */
			/*
			 * If j1 has more cards than j2 (AKQ - Jxx ) or j2 has no winning
			 * cards (A - xxx)=> Blocage = _Niveau_2
			 */
			if (j1IN.size() >= j2IN.size()
					|| (n_j2_j3 == 0 && n_j1_j3 < j3IN.size())) {
				return Blocage._Niveau_2_A;
			}

			return Blocage._Niveau_4_A;

		}

		/* All the cards of j2 are greater than the cards of j1 */
		if (n_j2_j1 == j2IN.size() && ns_j2_j1 == 0) {
			/* If j2 has no winning card => Blocage = _Niveau_6 */
			if (n_j2_j3 == 0) {
				return Blocage._Niveau_6;
			}
			/* BEGIN MODIF JR */
			if ((n_j2_j3 != n_j2_j1) && (j2IN.size() > j1IN.size())) {
				return Blocage._Niveau_1_B;
			}
			/* FIN MODIF JR */
			/*
			 * If j2 has more cards than j1 (AKQ - Jxx ) or j1 has no winning
			 * cards (A - xxx)=> Blocage = _Niveau_2
			 */
			if (j2IN.size() >= j1IN.size()
					|| (n_j1_j3 == 0 && n_j2_j3 < j3IN.size())) {
				return Blocage._Niveau_2_B;
			}

			return Blocage._Niveau_4_B;
		}

		/*
		 * If j1 has at least one card greater than all the j2's cards and one
		 * card smaller than at least one j2's card
		 */
		if (n_j1_j2 > 0) {
			/* If j1 has no winning card => Blocage = _Niveau_6 */
			if (n_j1_j3 == 0) {
				return Blocage._Niveau_6;
			}

			/* If j2 has no winning card */
			if (n_j2_j3 == 0) {
				/* No Setting up */
				if (n_j1_j3 < j3IN.size() || j2IN.size() <= j3IN.size()) {
					return Blocage._Niveau_2_A;
				}

				/* Setting up blocked */
				if (ns_j1_j2 + n_j1_j2 == j1IN.size() && n_j1_j3 == j3IN.size()) {
					return Blocage._Niveau_1_B;
				}

				/* Setting up not blocked */
				return Blocage._Niveau_0;
			}

			/* If the both hand have the same number of cards */
			if (j1IN.size() == j2IN.size()) {
				/* No Setting up */
				if (n_j1_j3 + n_j2_j3 < j3IN.size()
						|| j2IN.size() <= j3IN.size()) {
					return Blocage._Niveau_0;
				}

				/* Setting up blocked B */
				if (ns_j1_j2 + n_j1_j2 == j1IN.size()
						&& n_j1_j3 + n_j2_j3 == j3IN.size()) {
					return Blocage._Niveau_1_B;
				}

				/* Setting up blocked A */
				if (ns_j2_j1 + n_j2_j3 == j2IN.size()
						&& n_j1_j3 + n_j2_j3 == j3IN.size()) {
					return Blocage._Niveau_1_A;
				}

				/* Setting up not blocked */
				return Blocage._Niveau_0;
			}

			/**/
			if (Math.min(j1IN.size(), j2IN.size()) >= n_j1_j3 + n_j2_j3) {
				return Blocage._Niveau_0;
			}

			/**/
			if (Math.min(j1IN.size(), j2IN.size()) < n_j1_j3 + n_j2_j3
					&& Math.max(j1IN.size(), j2IN.size()) >= n_j1_j3 + n_j2_j3
					&& (j1IN.size() == n_j1_j3 || j2IN.size() == n_j2_j3)) {
				if (j1IN.size() < j2IN.size()) {
					return Blocage._Niveau_3_A;
				}

				return Blocage._Niveau_3_B;

			}

			if (j1IN.size() < j2IN.size()) {
				return Blocage._Niveau_1_B;
			}
			return Blocage._Niveau_1_A;
		}

		/*
		 * If j2 has at least one card greater than all the j1's cards and one
		 * card smaller than at least one j1's card
		 */
		if (n_j2_j1 > 0) {
			/* If j2 has no winning card => Blocage = _Niveau_6 */
			if (n_j2_j3 == 0) {
				return Blocage._Niveau_6;
			}

			/* If j1 has no winning card */
			if (n_j1_j3 == 0) {
				/* No Setting up */
				if (n_j2_j3 < j3IN.size() || j1IN.size() <= j3IN.size()) {
					return Blocage._Niveau_2_B;
				}

				/* Setting up blocked */
				if (ns_j2_j1 + n_j2_j1 == j2IN.size() && n_j2_j3 == j3IN.size()) {
					return Blocage._Niveau_1_A;
				}

				/* Setting up not blocked */
				return Blocage._Niveau_0;
			}

			/* If the both hand have the same number of cards */
			if (j1IN.size() == j2IN.size()) {
				/* No Setting up */
				if (n_j1_j3 + n_j2_j3 < j3IN.size()
						|| j1IN.size() <= j3IN.size()) {
					return Blocage._Niveau_0;
				}

				/* Setting up blocked B */
				if (ns_j1_j2 + n_j1_j3 == j1IN.size()
						&& n_j1_j3 + n_j2_j3 == j3IN.size()) {
					return Blocage._Niveau_1_B;
				}

				/* Setting up blocked A */
				if (ns_j2_j1 + n_j2_j1 == j2IN.size()
						&& n_j1_j3 + n_j2_j3 == j3IN.size()) {
					return Blocage._Niveau_1_A;
				}

				/* Setting up not blocked */
				return Blocage._Niveau_0;
			}

			/**/
			if (Math.min(j1IN.size(), j2IN.size()) >= n_j1_j3 + n_j2_j3) {
				return Blocage._Niveau_0;
			}

			/**/
			if (Math.min(j1IN.size(), j2IN.size()) < n_j1_j3 + n_j2_j3
					&& Math.max(j1IN.size(), j2IN.size()) >= n_j1_j3 + n_j2_j3
					&& (j1IN.size() == n_j1_j3 || j2IN.size() == n_j2_j3)) {
				if (j1IN.size() < j2IN.size()) {
					return Blocage._Niveau_3_A;
				}

				return Blocage._Niveau_3_B;

			}

			if (j1IN.size() < j2IN.size()) {
				return Blocage._Niveau_1_B;
			}
			return Blocage._Niveau_1_A;
		}

		return Blocage._Niveau_Inconnu;
	}

	public static int getNbTricksColor(List<BridgeCard> j1IN,List<BridgeCard> j2IN, List<BridgeCard> j3IN, Blocage b) {
		int n_j1_j3 = getNbWinningCardColor(j1IN, j3IN);
		int n_j2_j3 = getNbWinningCardColor(j2IN, j3IN);

		if (b == Blocage._Niveau_0) {
			if (n_j1_j3 + n_j2_j3 >= j3IN.size()) {
				return Math.max(j1IN.size(), j2IN.size());
			} else {
				return Math.min(n_j1_j3 + n_j2_j3,Math.max(j1IN.size(), j2IN.size()));
			}
		}

		if (b == Blocage._Niveau_1_A) {
			if (n_j1_j3 + n_j2_j3 >= j3IN.size()) {
				return Math.max(j1IN.size(), j2IN.size());
			} else {
				return Math.min(n_j1_j3 + n_j2_j3,Math.max(j1IN.size(), j2IN.size()));
			}
		}

		if (b == Blocage._Niveau_1_B) {
			if (n_j1_j3 + n_j2_j3 >= j3IN.size()) {
				return Math.max(j1IN.size(), j2IN.size());
			} else {
				return Math.min(n_j1_j3 + n_j2_j3,Math.max(j1IN.size(), j2IN.size()));
			}
		}

		if (b == Blocage._Niveau_2_A) {
			if (n_j1_j3 >= j3IN.size()) 
			{
				return j1IN.size();
			} 
			else 
			{
				return n_j1_j3;
			}
		}

		if (b == Blocage._Niveau_2_B) {
			if (n_j2_j3 >= j3IN.size()) 
			{
				return j2IN.size();
			} 
			else {
				return n_j2_j3;
			}
		}

		if (b == Blocage._Niveau_3_A) {
			return n_j1_j3;
		}

		if (b == Blocage._Niveau_3_B) {
			return n_j2_j3;
		}

		if (b == Blocage._Niveau_4_A) {
			return n_j1_j3;
		}

		if (b == Blocage._Niveau_4_B) {
			return n_j2_j3;
		}

		if (b == Blocage._Niveau_5_A) {
			if (n_j1_j3 >= j3IN.size()) 
			{
				return j1IN.size();
			} 
			else 
			{
				return n_j1_j3;
			}
		}

		if (b == Blocage._Niveau_5_B) {
			if (n_j2_j3 >= j3IN.size()) 
			{
				return j2IN.size();
			} 
			else 
			{
				return n_j2_j3;
			}
		}

		if (b == Blocage._Niveau_6) {
			return 0;
		}

		return -100;
	}

	public static int getNbTricksUnlockableColor(List<BridgeCard> j1IN,List<BridgeCard> j2IN, List<BridgeCard> j3IN, Blocage b) {
		int n_j1_j3 = getNbWinningCardColor(j1IN, j3IN);
		int n_j2_j3 = getNbWinningCardColor(j2IN, j3IN);

		if (b == Blocage._Niveau_0) {
			return 0;
		}

		if (b == Blocage._Niveau_1_A) {
			return 0;
		}

		if (b == Blocage._Niveau_1_B) {
			return 0;
		}

		if (b == Blocage._Niveau_2_A) {
			return 0;
		}

		if (b == Blocage._Niveau_2_B) {
			return 0;
		}

		if (b == Blocage._Niveau_3_A) {
			if (n_j1_j3 + n_j2_j3 >= j3IN.size()) 
			{
				return j2IN.size() - j1IN.size();
			} 
			else 
			{
				return n_j2_j3;
			}
		}

		if (b == Blocage._Niveau_3_B) {
			if (n_j1_j3 + n_j2_j3 >= j3IN.size()) 
			{
				return j1IN.size() - j2IN.size();
			} 
			else 
			{
				return n_j1_j3;
			}
		}

		if (b == Blocage._Niveau_4_A) {
			if (n_j1_j3 + n_j2_j3 >= j3IN.size()) 
			{
				return j2IN.size() - j1IN.size();
			} 
			else 
			{
				return Math.min(n_j2_j3, j2IN.size() - j1IN.size());
			}
		}

		if (b == Blocage._Niveau_4_B) {
			if (n_j1_j3 + n_j2_j3 >= j3IN.size()) 
			{
				return j1IN.size() - j2IN.size();
			} 
			else 
			{
				return Math.min(n_j1_j3, j1IN.size() - j2IN.size());
			}
		}

		if (b == Blocage._Niveau_5_A) {
			return 0;
		}

		if (b == Blocage._Niveau_5_B) {
			return 0;
		}

		if (b == Blocage._Niveau_6) {
			return 0;
		}

		return -100;
	}

	public static int getNbGoBack(List<BridgeCard> j1IN,List<BridgeCard> j2IN, List<BridgeCard> j3IN)
	{
		boolean communicate[] = new boolean[j2IN.size()];
		for(int i = 0 ; i < j2IN.size() ; i++)
		{
			communicate[i] = false;
		}
		
		int n_j1_j3 = getNbWinningCardColor(j1IN, j3IN);
		int n_j2_j3 = getNbWinningCardColor(j2IN, j3IN);
		
		int nb = 0;
		for(int i =0 ; i < j1IN.size() ; i++)
		{
			boolean isWinning = true;
			/*If we can't set up the suit, check that the card is a winning one*/
			if(n_j1_j3+n_j2_j3 < j3IN.size())
			{
				for(int j = 0 ; j < j3IN.size() ; j ++)
				{
					if(j1IN.get(i).getValue().getVal() < j3IN.get(j).getValue().getVal())
					{
						isWinning = false;
						break;
					}
				}				
			}
			
			int nbInf = 0;
			if(isWinning)
			{
				for(int j = 0 ; j < j2IN.size() ; j++)
				{
					if(j1IN.get(i).getValue().getVal() > j2IN.get(j).getValue().getVal())
					{
						nbInf++;
					}
					
				}
			}
			
			while(nbInf > 0)
			{
				if(communicate[nbInf-1]==true)
				{
					nbInf --;
				}
				else
				{
					communicate[nbInf-1] = true;
					nb ++;
					break;
				}
			}
		}
		return nb;
	}
	
	/**
	 * Compute the number of ruffs makeable in this suit : 
	 * the result is positive if j1 ruffs
	 * the result is negative if j2 ruffs*/
	public static int getNbRuff(List<BridgeCard> j1IN,List<BridgeCard> j2IN, List<BridgeCard> j3IN)
	{
		int nb = 0;
		
		int n_j1_j3 = getNbWinningCardColor(j1IN, j3IN);
		int n_j2_j3 = getNbWinningCardColor(j2IN, j3IN);
		
		if( (n_j1_j3+n_j2_j3 >= j1IN.size()) && (n_j1_j3+n_j2_j3 < j2IN.size()) )
		{
			nb = Math.max(j2IN.size() - n_j1_j3 - n_j2_j3, 0);
		}
		
		if( (n_j1_j3+n_j2_j3 >= j2IN.size()) && (n_j1_j3+n_j2_j3 < j1IN.size()) )
		{
			nb = - Math.max(j1IN.size() - n_j1_j3 - n_j2_j3, 0);
		}
		
		return nb;
	}
	
	public static void removeTrump(List<BridgeCard> j1IN,List<BridgeCard> j2IN, List<BridgeCard> j3IN) throws GameClaimException
	{
		int pos1 = 0;
		int pos2 = 0;
		
		/*Remove the last trump of j3*/
		if(j3IN.size() == 1)
		{
			j3IN.remove(0);
		}
		else
		{
			throw new GameClaimException("BUG REMOVE_TRUMP : size j3IN="+j3IN.size());
		}
		
		
		/*Remove the worst card of j1*/
		int iter1 = j1IN.size();
		if(iter1 > 0)
		{
			for(int i = 1 ; i < iter1 ; i++)
			{
				if(j1IN.get(i).getValue().getVal() < j1IN.get(pos1).getValue().getVal())
				{
					pos1 = i;
				}
			}
			j1IN.remove(pos1);			
		}
		
		/*Remove the worst card of j2*/
		int iter2 = j2IN.size();
		if(iter2 > 0)
		{
			for(int i = 1 ; i < iter2 ; i++)
			{
				if(j2IN.get(i).getValue().getVal() < j2IN.get(pos2).getValue().getVal())
				{
					pos2 = i;
				}
			}
			j2IN.remove(pos2);
		}
	


	}

	public static void removeTrumps(List<BridgeCard> j1IN,List<BridgeCard> j2IN, List<BridgeCard> j3IN)
	{
		Blocage b = getBlocage(j1IN,j2IN,j3IN);
		int n = getNbTricksColor(j1IN, j2IN, j3IN, b);
		int nj3 = j3IN.size();
		
		while((n>0) && (nj3 > 0))
		{
			int pos1 = 0;
			int pos2 = 0;
			int pos3 = 0;
			
			/*j1 has the best card*/
			if(getNbWinningCardColor(j1IN, j2IN) > 0)
			{
				/*Get the position of the best card of j1*/
				for(int i = 1 ; i < j1IN.size() ; i++)
				{
					if(j1IN.get(i).getValue().getVal() > j1IN.get(pos1).getValue().getVal())
					{
						pos1 = i;
					}
				}
				
				/*Get the position of the worst card of j2*/
				for(int i = 1 ; i < j2IN.size() ; i++)
				{
					if(j2IN.get(i).getValue().getVal() < j2IN.get(pos2).getValue().getVal())
					{
						pos2 = i;
					}
				}
			}
			/*j2 has the bast card*/
			else
			{
				/*Get the position of the worst card of j1*/
				for(int i = 1 ; i < j1IN.size() ; i++)
				{
					if(j1IN.get(i).getValue().getVal() < j1IN.get(pos1).getValue().getVal())
					{
						pos1 = i;
					}
				}
				
				/*Get the position of the best card of j2*/
				for(int i = 1 ; i < j2IN.size() ; i++)
				{
					if(j2IN.get(i).getValue().getVal() > j2IN.get(pos2).getValue().getVal())
					{
						pos2 = i;
					}
				}
			}
			
			/*Get the position of the worst card of j3*/
			for(int i = 1 ; i < j3IN.size() ; i++)
			{
				if(j3IN.get(i).getValue().getVal() < j3IN.get(pos3).getValue().getVal())
				{
					pos3 = i;
				}
			}
			
			
			
			if(j1IN.size() > 0)
			{
				j1IN.remove(pos1);
			}
			if(j2IN.size() > 0)
			{
				j2IN.remove(pos2);
			}	
			j3IN.remove(pos3);
			
			n = getNbTricksColor(j1IN, j2IN, j3IN, b);
			nj3 = j3IN.size();
		}
	}

	public static void updateTrump(List<BridgeCard> j1IN,List<BridgeCard> j2IN) throws GameClaimException
	{
		int pos1 = 0;
		int pos2 = 0;
		
		if(j1IN.size() == 0 || j2IN.size() == 0 )
		{
			throw new GameClaimException("ERROR update TRUMP : j1IN.size()="+j1IN.size()+" - j2IN.size()="+j2IN.size());
		}
		
		/*Get the position of the best card of j1*/
		for(int i = 1 ; i < j1IN.size() ; i++)
		{
			if(j1IN.get(i).getValue().getVal() > j1IN.get(pos1).getValue().getVal())
			{
				pos1 = i;
			}
		}
		
		/*Get the position of the worst card of j2*/
		for(int i = 1 ; i < j2IN.size() ; i++)
		{
			if(j2IN.get(i).getValue().getVal() < j2IN.get(pos2).getValue().getVal())
			{
				pos2 = i;
			}
		}
		
		if(j1IN.get(pos1).getValue().getVal() > j2IN.get(pos2).getValue().getVal())
		{
			j1IN.remove(pos1);
			j2IN.remove(pos2);
		}
//		else
//		{
//			throw new GameClaimException("ERREUR UPDATE_TRUMP - j1IN.get(pos1).getValue().getVal() ("+j1IN.get(pos1).getValue().getVal()+") > j2IN.get(pos2).getValue().getVal() ("+j2IN.get(pos2).getValue().getVal()+")");
//		}
		
		
	}

	public static int getTotalNbTricks(BridgeGame game) throws GameClaimException
	{
		if(game != null)
		{
			/*Check that we are in a card's phase*/
			if(!game.isPhaseCard) {
				return -1;
			}
			/*For the moment claiming is allowed only at the beginning of a new trick*/
			if(!game.isBeginTrick()) {
				return -1;
			}
			
			final char declarer = game.getDeclarer();
			final char declarerPartner = GameBridgeRule.getNextPosition(GameBridgeRule.getNextPosition(declarer));
			final char declarerNext = GameBridgeRule.getNextPosition(declarer);
			
			char joueurCourant;
			if(game.cardPlayed.size() <= 3) {
				joueurCourant = declarerNext;
			} else {
				joueurCourant = GameBridgeRule.getLastWinnerTrick(game.cardPlayed,game.contract).getOwner();	
			}
			
			int maxTricks = (52 - game.cardPlayed.size() + (game.cardPlayed.size() % 4))/4;
			/*Case no trump*/
			if(game.contract.getColor() == BidColor.NoTrump) {
				List<BridgeCard> j1_p = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Spade);
				List<BridgeCard> j1_c = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Heart);
				List<BridgeCard> j1_k = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Diamond);
				List<BridgeCard> j1_t = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Club);
				
				List<BridgeCard> j2_p = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Spade);
				List<BridgeCard> j2_c = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Heart);
				List<BridgeCard> j2_k = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Diamond);
				List<BridgeCard> j2_t = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Club);
							
				
				List<BridgeCard> j3_p = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Spade);
				j3_p.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Spade));
				List<BridgeCard> j3_c = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Heart); 
				j3_c.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Heart));
				List<BridgeCard> j3_k = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Diamond);
				j3_k.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Diamond));
				List<BridgeCard> j3_t = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Club); 
				j3_t.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Club));
				
				int to_return = 0;
				
				/*The blocage's situation in each suit*/
				Blocage b_p = getBlocage(j1_p, j2_p, j3_p);
				Blocage b_c = getBlocage(j1_c, j2_c, j3_c);
				Blocage b_k = getBlocage(j1_k, j2_k, j3_k);
				Blocage b_t = getBlocage(j1_t, j2_t, j3_t);
				
				/*The number of direct tricks in each suit*/
				int n_p = getNbTricksColor(j1_p,j2_p,j3_p,b_p);
				int n_c = getNbTricksColor(j1_c,j2_c,j3_c,b_c);
				int n_k = getNbTricksColor(j1_k,j2_k,j3_k,b_k);
				int n_t = getNbTricksColor(j1_t,j2_t,j3_t,b_t);
				
				/*The number of unlockable tricks in each suit*/
				int n_u_p = getNbTricksUnlockableColor(j1_p,j2_p,j3_p,b_p);
				int n_u_c = getNbTricksUnlockableColor(j1_c,j2_c,j3_c,b_c);
				int n_u_k = getNbTricksUnlockableColor(j1_k,j2_k,j3_k,b_k);
				int n_u_t = getNbTricksUnlockableColor(j1_t,j2_t,j3_t,b_t);
				
				/****Study on communications****/
				/*if there is at least one level 0/1 --> we can take all the tricks*/
				if(b_p == Blocage._Niveau_0 || b_p == Blocage._Niveau_1_A || b_p == Blocage._Niveau_1_B 
				|| b_c == Blocage._Niveau_0 || b_c == Blocage._Niveau_1_A || b_c == Blocage._Niveau_1_B 
				|| b_k == Blocage._Niveau_0 || b_k == Blocage._Niveau_1_A || b_k == Blocage._Niveau_1_B 
				|| b_t == Blocage._Niveau_0 || b_t == Blocage._Niveau_1_A || b_t == Blocage._Niveau_1_B )
				{
					to_return = Math.min(n_p + n_c + n_k + n_t + n_u_p + n_u_c + n_u_k + n_u_t, maxTricks);
				}
				/*if there is a level 2 face to a level 2 --> we can take all the tricks*/
				else if( (b_p == Blocage._Niveau_2_A || b_c == Blocage._Niveau_2_A || b_k == Blocage._Niveau_2_A || b_t == Blocage._Niveau_2_A) 
				&&  (b_p == Blocage._Niveau_2_B || b_c == Blocage._Niveau_2_B || b_k == Blocage._Niveau_2_B || b_t == Blocage._Niveau_2_B))
				{
					to_return = Math.min(n_p + n_c + n_k + n_t + n_u_p + n_u_c + n_u_k + n_u_t, maxTricks);
				}
				/*if there is a level 3 face to a level 2 --> we can take all the tricks*/
				else if( ((b_p == Blocage._Niveau_2_A || b_c == Blocage._Niveau_2_A || b_k == Blocage._Niveau_2_A || b_t == Blocage._Niveau_2_A) 
						&& (b_p == Blocage._Niveau_3_B || b_c == Blocage._Niveau_3_B || b_k == Blocage._Niveau_3_B || b_t == Blocage._Niveau_3_B))
					||((b_p == Blocage._Niveau_2_B || b_c == Blocage._Niveau_2_B || b_k == Blocage._Niveau_2_B || b_t == Blocage._Niveau_2_B) 
						&& (b_p == Blocage._Niveau_3_A || b_c == Blocage._Niveau_3_A || b_k == Blocage._Niveau_3_A || b_t == Blocage._Niveau_3_A)))
				{
					to_return = Math.min(n_p + n_c + n_k + n_t + n_u_p + n_u_c + n_u_k + n_u_t, maxTricks);
				}
				/*if there is a level 4 face to a level 2 --> we can take all the tricks*/
				else if( ((b_p == Blocage._Niveau_2_A || b_c == Blocage._Niveau_2_A || b_k == Blocage._Niveau_2_A || b_t == Blocage._Niveau_2_A) 
						&&(b_p == Blocage._Niveau_4_B || b_c == Blocage._Niveau_4_B || b_k == Blocage._Niveau_4_B || b_t == Blocage._Niveau_4_B))
					||   ((b_p == Blocage._Niveau_2_B || b_c == Blocage._Niveau_2_B || b_k == Blocage._Niveau_2_B || b_t == Blocage._Niveau_2_B) 
						&&(b_p == Blocage._Niveau_4_A || b_c == Blocage._Niveau_4_A || b_k == Blocage._Niveau_4_A || b_t == Blocage._Niveau_4_A)))
				{
					to_return = Math.min(n_p + n_c + n_k + n_t + n_u_p + n_u_c + n_u_k + n_u_t, maxTricks);
				}
				/*if there is a level 3/4 face to a level 3/4*/
				else if(((b_p == Blocage._Niveau_3_A || b_c == Blocage._Niveau_3_A || b_k == Blocage._Niveau_3_A || b_t == Blocage._Niveau_3_A) 
						|| b_p == Blocage._Niveau_4_A || b_c == Blocage._Niveau_4_A || b_k == Blocage._Niveau_4_A || b_t == Blocage._Niveau_4_A)
					&&((b_p == Blocage._Niveau_3_B || b_c == Blocage._Niveau_3_B || b_k == Blocage._Niveau_3_B || b_t == Blocage._Niveau_3_B) 
						|| b_p == Blocage._Niveau_4_B || b_c == Blocage._Niveau_4_B || b_k == Blocage._Niveau_4_B || b_t == Blocage._Niveau_4_B))
				{
					/*Unlockable tricks for A*/
					int n_u_A = 0;
					/*Unlockable tricks for B*/
					int n_u_B = 0;
					/*Communications to A*/
					int com_A = 0;
					/*Communications to B*/
					int com_B = 0;
					
					/*Spade*/
					if(b_p == Blocage._Niveau_3_A || b_p == Blocage._Niveau_4_A) {
						n_u_B += n_u_p;
						com_A ++;
					} else {
						if(b_p == Blocage._Niveau_3_B || b_p == Blocage._Niveau_4_B) {
							n_u_A += n_u_p;
							com_B ++;							
						}
					}
					/*Heart*/
					if(b_c == Blocage._Niveau_3_A || b_c == Blocage._Niveau_4_A) {
						n_u_B += n_u_c;
						com_A ++;
					} else {
						if(b_c == Blocage._Niveau_3_B || b_c == Blocage._Niveau_4_B) {
							n_u_A += n_u_c;
							com_B ++;							
						}
					}
					/*Diamond*/
					if(b_k == Blocage._Niveau_3_A || b_k == Blocage._Niveau_4_A) {
						n_u_B += n_u_k;
						com_A ++;
					} else {
						if(b_k == Blocage._Niveau_3_B || b_k == Blocage._Niveau_4_B) {
							n_u_A += n_u_k;
							com_B ++;							
						}
					}
					/*Club*/
					if(b_t == Blocage._Niveau_3_A || b_t == Blocage._Niveau_4_A) {
						n_u_B += n_u_t;
						com_A ++;
					} else {
						if(b_t == Blocage._Niveau_3_B || b_t == Blocage._Niveau_4_B) {
							n_u_A += n_u_t;
							com_B ++;							
						}
					}
					
					/*I�) n_u_A > n_u_B --> more tricks unlockable in declarant*/					
					if(n_u_A > n_u_B) {
						int n_w_p_B = getNbWinningCardColor(j2_p, j3_p);
						int n_w_c_B = getNbWinningCardColor(j2_c, j3_c);
						int n_w_k_B = getNbWinningCardColor(j2_k, j3_k);
						int n_w_t_B = getNbWinningCardColor(j2_t, j3_t);
						
						int posLast = 0;
						
						/*Spade*/
						if(b_p == Blocage._Niveau_3_A && (n_p >= 2 || com_A >= 2) && n_w_p_B >= 2) {
							n_u_A = n_u_A + n_w_p_B - 1;
							com_A --;
							posLast = 1;
						}
						/*Heart*/
						if(b_c == Blocage._Niveau_3_A  && n_w_c_B >= 2) {
							if(n_c >= 2 || com_A >= 2) {
								n_u_A = n_u_A + n_w_c_B -1;
								com_A --;
								if(posLast == 0 || (n_w_p_B > n_w_c_B) ){
									posLast = 2;
								}
							} else if(posLast == 1 && n_w_c_B > n_w_p_B) {
								n_u_A = n_u_A + n_w_c_B - n_w_p_B;
								posLast = 2;
							}
						}
						/*Diamond*/
						if(b_k == Blocage._Niveau_3_A && n_w_k_B >= 2) {
							if(n_k >= 2 || com_A >= 2) {
								n_u_A = n_u_A + n_w_k_B -1;
								com_A --;
								if(posLast == 0 || (n_w_p_B > n_w_k_B) || (n_w_c_B > n_w_k_B) ) {
									posLast = 3;
								}
							} else if(posLast == 1 && n_w_k_B > n_w_p_B) {
								n_u_A = n_u_A + n_w_k_B - n_w_p_B;
								posLast = 3;
							} else if(posLast == 2 && n_w_k_B > n_w_c_B) {
								n_u_A = n_u_A + n_w_k_B - n_w_c_B;
								posLast = 3;
							}
						}
						/*Trefle*/
						if(b_t == Blocage._Niveau_3_A && n_w_t_B >= 2) {
							if(n_t >= 2 || com_A >= 2) {
								n_u_A = n_u_A + n_w_t_B -1;
								com_A --;
							} else if(posLast == 1 && n_w_t_B > n_w_p_B) {
								n_u_A = n_u_A + n_w_t_B - n_w_p_B;
							} else if(posLast == 2 && n_w_t_B > n_w_c_B) {
								n_u_A = n_u_A + n_w_t_B - n_w_c_B;
							} else if(posLast == 3 && n_w_t_B > n_w_k_B) {
								n_u_A = n_u_A + n_w_t_B - n_w_k_B;
							}
						}
						to_return = Math.min(n_p+n_c+n_k+n_t+n_u_A, maxTricks);
					}
					/*II�) n_u_A < n_u_B*/
					else if(n_u_A < n_u_B) {
						int n_w_p_A = getNbWinningCardColor(j1_p, j3_p);
						int n_w_c_A = getNbWinningCardColor(j1_c, j3_c);
						int n_w_k_A = getNbWinningCardColor(j1_k, j3_k);
						int n_w_t_A = getNbWinningCardColor(j1_t, j3_t);

						int posLast = 0;
						
						/*Spade*/
						if(b_p == Blocage._Niveau_3_B && (n_p >= 2 || com_B >= 2) && n_w_p_A >= 2) {
							n_u_B = n_u_B + n_w_p_A - 1;
							com_B --;
							posLast = 1;
						}
						/*Heart*/
						if(b_c == Blocage._Niveau_3_B  && n_w_c_A >= 2) {
							if(n_c >= 2 || com_B >= 2) {
								n_u_B = n_u_B + n_w_c_A -1;
								com_B --;
								if(posLast == 0 || (n_w_p_A > n_w_c_A) ) {
									posLast = 2;
								}
							} else if(posLast == 1 && n_w_c_A > n_w_p_A) {
								n_u_B = n_u_B + n_w_c_A - n_w_p_A;
								posLast = 2;
							}
						}
						/*Diamond*/
						if(b_k == Blocage._Niveau_3_B && n_w_k_A >= 2) {
							if(n_k >= 2 || com_B >= 2) {
								n_u_B = n_u_B + n_w_k_A -1;
								com_B --;
								if(posLast == 0 || (n_w_p_A > n_w_k_A) || (n_w_c_A > n_w_k_A) ){
									posLast = 3;
								}
							}
							else if(posLast == 1 && n_w_k_A > n_w_p_A){
								n_u_B = n_u_B + n_w_k_A - n_w_p_A;
								posLast = 3;
							}
							else if(posLast == 2 && n_w_k_A > n_w_c_A){
								n_u_B = n_u_B + n_w_k_A - n_w_c_A;
								posLast = 3;
							}
						}
						/*Trefle*/
						if(b_t == Blocage._Niveau_3_A && n_w_t_A >= 2) {
							if(n_t >= 2 || com_B >= 2) {
								n_u_B = n_u_B + n_w_t_A -1;
								com_B --;
							}
							else if(posLast == 1 && n_w_t_A > n_w_p_A){
								n_u_B = n_u_B + n_w_t_A - n_w_p_A;
							}
							else if(posLast == 2 && n_w_t_A > n_w_c_A){
								n_u_B = n_u_B + n_w_t_A - n_w_c_A;
							}
							else if(posLast == 3 && n_w_t_A > n_w_k_A){
								n_u_B = n_u_B + n_w_t_A - n_w_k_A;
							}
						}
						to_return = Math.min(n_p+n_c+n_k+n_t+n_u_B, maxTricks);
					}
					/*III�) n_u_A = n_u_B*/
					else
					{
						int n_w_p_B = getNbWinningCardColor(j2_p, j3_p);
						int n_w_c_B = getNbWinningCardColor(j2_c, j3_c);
						int n_w_k_B = getNbWinningCardColor(j2_k, j3_k);
						int n_w_t_B = getNbWinningCardColor(j2_t, j3_t);
						
						int n_w_p_A = getNbWinningCardColor(j1_p, j3_p);
						int n_w_c_A = getNbWinningCardColor(j1_c, j3_c);
						int n_w_k_A = getNbWinningCardColor(j1_k, j3_k);
						int n_w_t_A = getNbWinningCardColor(j1_t, j3_t);
						
						int posLastB = 0;
						int posLastA = 0;
						
						/*Spade A*/
						if(b_p == Blocage._Niveau_3_A && (n_p >= 2 || com_A >= 2) && n_w_p_B >= 2){
							n_u_A = n_u_A + n_w_p_B - 1;
							com_A --;
							posLastA = 1;
						}
						/*Heart A*/
						if(b_c == Blocage._Niveau_3_A  && n_w_c_B >= 2){
							if(n_c >= 2 || com_A >= 2) {
								n_u_A = n_u_A + n_w_c_B -1;
								com_A --;
								if(posLastA == 0 || (n_w_p_B > n_w_c_B) ){
									posLastA = 2;
								}
							}
							else if(posLastA == 1 && n_w_c_B > n_w_p_B){
								n_u_A = n_u_A + n_w_c_B - n_w_p_B;
								posLastA = 2;
							}
						}
						/*Diamond A*/
						if(b_k == Blocage._Niveau_3_A && n_w_k_B >= 2){
							if(n_k >= 2 || com_A >= 2){
								n_u_A = n_u_A + n_w_k_B -1;
								com_A --;
								if(posLastA == 0 || (n_w_p_B > n_w_k_B) || (n_w_c_B > n_w_k_B) ){
									posLastA = 3;
								}
							}
							else if(posLastA == 1 && n_w_c_B > n_w_p_B){
								n_u_A = n_u_A + n_w_k_B - n_w_p_B;
								posLastA = 3;
							}
							else if(posLastA == 2 && n_w_k_B > n_w_c_B){
								n_u_A = n_u_A + n_w_k_B - n_w_c_B;
								posLastA = 3;
							}
						}
						/*Trefle A*/
						if(b_t == Blocage._Niveau_3_A && n_w_t_B >= 2){
							if(n_t >= 2 || com_A >= 2){
								n_u_A = n_u_A + n_w_t_B -1;
								com_A --;
							}
							else if(posLastA == 1 && n_w_t_B > n_w_p_B){
								n_u_A = n_u_A + n_w_t_B - n_w_p_B;
							}
							else if(posLastA == 2 && n_w_t_B > n_w_c_B){
								n_u_A = n_u_A + n_w_t_B - n_w_c_B;
							}
							else if(posLastA == 3 && n_w_t_B > n_w_k_B){
								n_u_A = n_u_A + n_w_t_B - n_w_k_B;
							}
						}
						
						/*Spade B*/
						if(b_p == Blocage._Niveau_3_B && (n_p >= 2 || com_B >= 2) && n_w_p_A >= 2){
							n_u_B = n_u_B + n_w_p_A - 1;
							com_B --;
							posLastB = 1;
						}
						/*Heart B*/
						if(b_c == Blocage._Niveau_3_B  && n_w_c_A >= 2){
							if(n_c >= 2 || com_B >= 2){
								n_u_B = n_u_B + n_w_c_A -1;
								com_B --;
								if(posLastB == 0 || (n_w_p_A > n_w_c_A) ){
									posLastB = 2;
								}
							}
							else if(posLastB == 1 && n_w_c_A > n_w_p_A){
								n_u_B = n_u_B + n_w_c_A - n_w_p_A;
								posLastB = 2;
							}
						}
						/*Diamond B*/
						if(b_k == Blocage._Niveau_3_B && n_w_k_A >= 2){
							if(n_k >= 2 || com_B >= 2){
								n_u_B = n_u_B + n_w_k_A -1;
								com_B --;
								if(posLastB == 0 || (n_w_p_A > n_w_k_A) || (n_w_c_A > n_w_k_A) ){
									posLastB = 3;
								}
							}
							else if(posLastB == 1 && n_w_c_A > n_w_p_A){
								n_u_B = n_u_B + n_w_k_A - n_w_p_A;
								posLastB = 3;
							}
							else if(posLastB == 2 && n_w_k_A > n_w_c_A){
								n_u_B = n_u_B + n_w_k_A - n_w_c_A;
								posLastB = 3;
							}
						}
						/*Trefle B*/
						if(b_t == Blocage._Niveau_3_A && n_w_t_A >= 2){
							if(n_t >= 2 || com_B >= 2){
								n_u_B = n_u_B + n_w_t_A -1;
								com_B --;
							}
							else if(posLastB == 1 && n_w_t_A > n_w_p_A){
								n_u_B = n_u_B + n_w_t_A - n_w_p_A;
							}
							else if(posLastB == 2 && n_w_t_A > n_w_c_A){
								n_u_B = n_u_B + n_w_t_A - n_w_c_A;
							}
							else if(posLastB == 3 && n_w_t_A > n_w_k_A){
								n_u_B = n_u_B + n_w_t_A - n_w_k_A;
							}
						}
						
						to_return = Math.min(Math.max(n_u_A, n_u_B)+n_p+n_c+n_k+n_t, maxTricks);
					}					
					
				}
				/*Level 3 with com but all in the same hand*/
				else if( (b_p == Blocage._Niveau_3_A && (n_p >= 2 || joueurCourant == declarer)  ) || (b_c == Blocage._Niveau_3_A && (n_c >= 2 || joueurCourant == declarer)) || (b_k == Blocage._Niveau_3_A && (n_k >= 2 || joueurCourant == declarer)) || (b_t == Blocage._Niveau_3_A && (n_t >= 2 || joueurCourant == declarer) )
						|| (b_p == Blocage._Niveau_3_B && (n_p >= 2 || joueurCourant == declarer) ) || (b_c == Blocage._Niveau_3_B && (n_c >= 2 || joueurCourant == declarer) ) || (b_k == Blocage._Niveau_3_B && (n_k >= 2 || joueurCourant == declarer) ) || (b_t == Blocage._Niveau_3_B && (n_t >= 2 || joueurCourant == declarer) ))
				{
					to_return = Math.min(n_p+n_c+n_k+n_t+n_u_p+n_u_c+n_u_k+n_u_t-1, maxTricks);
				}
				/*Level 2/4 face to the current player or level 3 without com*/
				else if( ((   b_p == Blocage._Niveau_2_A || b_p == Blocage._Niveau_4_A
						   || b_c == Blocage._Niveau_2_A || b_c == Blocage._Niveau_4_A
						   || b_k == Blocage._Niveau_2_A || b_k == Blocage._Niveau_4_A
						   || b_t == Blocage._Niveau_2_A || b_t == Blocage._Niveau_4_A) 
						  && (joueurCourant==declarerPartner))
						|| (( b_p == Blocage._Niveau_2_B || b_p == Blocage._Niveau_4_B
						   || b_c == Blocage._Niveau_2_B || b_c == Blocage._Niveau_4_B
						   || b_k == Blocage._Niveau_2_B || b_k == Blocage._Niveau_4_B
						   || b_t == Blocage._Niveau_2_B || b_t == Blocage._Niveau_4_B) 
						  && (joueurCourant==declarer))
						|| b_p == Blocage._Niveau_3_A || b_p == Blocage._Niveau_3_B 
						|| b_c == Blocage._Niveau_3_A || b_c == Blocage._Niveau_3_B
						|| b_k == Blocage._Niveau_3_A || b_k == Blocage._Niveau_3_B
						|| b_t == Blocage._Niveau_3_A || b_t == Blocage._Niveau_3_B	)
				{
					to_return = Math.min(n_p+n_c+n_k+n_t, maxTricks);
				}
				/*Otherwise, there is no communication*/
				else {
					/*A to play*/
					if(joueurCourant == declarer){
						to_return = getNbWinningCardColor(j1_p,j3_p) + getNbWinningCardColor(j1_c,j3_c) + getNbWinningCardColor(j1_k,j3_k) + getNbWinningCardColor(j1_t,j3_t);
					}
					/*B to play*/
					else if(joueurCourant == declarerPartner){
						to_return = getNbWinningCardColor(j2_p,j3_p) + getNbWinningCardColor(j2_c,j3_c) + getNbWinningCardColor(j2_k,j3_k) + getNbWinningCardColor(j2_t,j3_t);
					}
					/*We don't know who will get the hand ... so we imagine the worst case*/
					else {
						to_return = Math.min(getNbWinningCardColor(j1_p,j3_p) + getNbWinningCardColor(j1_c,j3_c) + getNbWinningCardColor(j1_k,j3_k) + getNbWinningCardColor(j1_t,j3_t), getNbWinningCardColor(j2_p,j3_p) + getNbWinningCardColor(j2_c,j3_c) + getNbWinningCardColor(j2_k,j3_k) + getNbWinningCardColor(j2_t,j3_t));
					}
				}
							
				/*If A or B to play*/
				if(joueurCourant == declarer || joueurCourant == declarerPartner){
					return to_return;
				}
				/*Else, we have to check the losers*/
				else {
					/*Immediate Losers*/
					int p_p = getNbImmediateLoserColor(j1_p,0,j2_p,0,j3_p);
					int p_c = getNbImmediateLoserColor(j1_c,0,j2_c,0,j3_c);
					int p_k = getNbImmediateLoserColor(j1_k,0,j2_k,0,j3_k);
					int p_t = getNbImmediateLoserColor(j1_t,0,j2_t,0,j3_t);
					
					int discard_A = Math.max(0, p_p - j1_p.size()) + Math.max(0, p_c - j1_c.size()) + Math.max(0, p_k - j1_k.size()) + Math.max(0, p_t - j1_t.size());
					int discard_B = Math.max(0, p_p - j2_p.size()) + Math.max(0, p_c - j2_c.size()) + Math.max(0, p_k - j2_k.size()) + Math.max(0, p_t - j2_t.size());
				
					int useless_card_A_p = Math.max(0,getNbUselessCard(j1_p,j2_p,j3_p) -p_p);
					int useless_card_A_c = Math.max(0,getNbUselessCard(j1_c,j2_c,j3_c) -p_c);
					int useless_card_A_k = Math.max(0,getNbUselessCard(j1_k,j2_k,j3_k) -p_k);
					int useless_card_A_t = Math.max(0,getNbUselessCard(j1_t,j2_t,j3_t) -p_t);
					int useless_card_B_p = Math.max(0,getNbUselessCard(j2_p,j1_p,j3_p) -p_p);
					int useless_card_B_c = Math.max(0,getNbUselessCard(j2_c,j1_c,j3_c) -p_c);
					int useless_card_B_k = Math.max(0,getNbUselessCard(j2_k,j1_k,j3_k) -p_k);
					int useless_card_B_t = Math.max(0,getNbUselessCard(j2_t,j1_t,j3_t) -p_t);
					
					
					boolean A_mort =   (b_p == Blocage._Niveau_2_B || b_p == Blocage._Niveau_4_B ||b_p == Blocage._Niveau_5_B || b_p == Blocage._Niveau_6)
									&& (b_c == Blocage._Niveau_2_B || b_c == Blocage._Niveau_4_B ||b_c == Blocage._Niveau_5_B || b_c == Blocage._Niveau_6)
									&& (b_k == Blocage._Niveau_2_B || b_k == Blocage._Niveau_4_B ||b_k == Blocage._Niveau_5_B || b_k == Blocage._Niveau_6)
									&& (b_t == Blocage._Niveau_2_B || b_t == Blocage._Niveau_4_B ||b_t == Blocage._Niveau_5_B || b_t == Blocage._Niveau_6);
					boolean B_mort =   (b_p == Blocage._Niveau_2_A || b_p == Blocage._Niveau_4_A ||b_p == Blocage._Niveau_5_A || b_p == Blocage._Niveau_6)
									&& (b_c == Blocage._Niveau_2_A || b_c == Blocage._Niveau_4_A ||b_c == Blocage._Niveau_5_A || b_c == Blocage._Niveau_6)
									&& (b_k == Blocage._Niveau_2_A || b_k == Blocage._Niveau_4_A ||b_k == Blocage._Niveau_5_A || b_k == Blocage._Niveau_6)
									&& (b_t == Blocage._Niveau_2_A || b_t == Blocage._Niveau_4_A ||b_t == Blocage._Niveau_5_A || b_t == Blocage._Niveau_6);

					if(useless_card_A_p + useless_card_A_c + useless_card_A_k + useless_card_A_t <= discard_A || useless_card_B_p + useless_card_B_c + useless_card_B_k + useless_card_B_t <= discard_B)
					{
						if( Math.max(discard_A - (useless_card_A_p + useless_card_A_c + useless_card_A_k + useless_card_A_t),0 ) + Math.max(discard_B - (useless_card_B_p + useless_card_B_c + useless_card_B_k + useless_card_B_t),0 ) <= to_return - Math.min(to_return,maxTricks-p_p-p_c-p_k-p_t) 
								|| (Math.max(discard_A - (useless_card_A_p + useless_card_A_c + useless_card_A_k + useless_card_A_t),0 ) <= to_return - Math.min(to_return,maxTricks-p_p-p_c-p_k-p_t) && (B_mort))
								|| (Math.max(discard_B - (useless_card_B_p + useless_card_B_c + useless_card_B_k + useless_card_B_t),0 ) <= to_return - Math.min(to_return,maxTricks-p_p-p_c-p_k-p_t) && (A_mort)))
						{
							
							/*Discard of winners isn't a problem TODO unsafe*/
							if(Math.max(0, n_p - 1)+Math.max(0, n_c - 1)+Math.max(0, n_k - 1)+Math.max(0, n_t - 1) >= discard_A + discard_B - ( useless_card_B_p + useless_card_B_c + useless_card_B_k + useless_card_B_t) - (useless_card_A_p + useless_card_A_c + useless_card_A_k + useless_card_A_t)
							||(Math.max(0, n_p - 1)+Math.max(0, n_c - 1)+Math.max(0, n_k - 1)+Math.max(0, n_t - 1) >= discard_A - ( useless_card_A_p + useless_card_A_c + useless_card_A_k + useless_card_A_t)&&(B_mort))
							||(Math.max(0, n_p - 1)+Math.max(0, n_c - 1)+Math.max(0, n_k - 1)+Math.max(0, n_t - 1) >= discard_B - ( useless_card_B_p + useless_card_B_c + useless_card_B_k + useless_card_B_t)&&(A_mort)))
							{
								return Math.min(to_return, maxTricks - p_p - p_c - p_k - p_t);
							}
							/*Discard of winners cost trick(s)*/
							else {
								return 0;
							}
						}
						else {
							return 0;
						}
					}
					else {
						return Math.min(to_return, maxTricks - p_p - p_c - p_k - p_t);
					}
					
				}
			}
			/*Case trump */
			else {
				List<BridgeCard> j1_c0 = new ArrayList<BridgeCard>();
				List<BridgeCard> j1_c1 = new ArrayList<BridgeCard>();
				List<BridgeCard> j1_c2 = new ArrayList<BridgeCard>();
				List<BridgeCard> j1_c3 = new ArrayList<BridgeCard>();
				
				List<BridgeCard> j2_c0 = new ArrayList<BridgeCard>();
				List<BridgeCard> j2_c1 = new ArrayList<BridgeCard>();
				List<BridgeCard> j2_c2 = new ArrayList<BridgeCard>();
				List<BridgeCard> j2_c3 = new ArrayList<BridgeCard>();
				
				List<BridgeCard> j3_c0 = new ArrayList<BridgeCard>();
				List<BridgeCard> j3_c1 = new ArrayList<BridgeCard>();
				List<BridgeCard> j3_c2 = new ArrayList<BridgeCard>();
				List<BridgeCard> j3_c3 = new ArrayList<BridgeCard>();
				
				if(game.contract.getColor() == BidColor.Spade)
				{
					j1_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Spade);
					j1_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Heart);
					j1_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Diamond);
					j1_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Club);
					
					j2_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Spade);
					j2_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Heart);
					j2_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Diamond);
					j2_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Club);
					
					j3_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Spade);
					j3_c0.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Spade));
					j3_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Heart); 
					j3_c1.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Heart));
					j3_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Diamond);
					j3_c2.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Diamond));
					j3_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Club); 
					j3_c3.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Club));
				}
				else if(game.contract.getColor() == BidColor.Heart) {
					j1_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Heart);
					j1_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Spade);					
					j1_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Diamond);
					j1_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Club);
					
					j2_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Heart);
					j2_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Spade);
					j2_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Diamond);
					j2_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Club);
					
					j3_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Heart);
					j3_c0.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Heart));
					j3_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Spade); 
					j3_c1.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Spade));
					j3_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Diamond);
					j3_c2.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Diamond));
					j3_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Club); 
					j3_c3.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Club));

				}
				else if(game.contract.getColor() == BidColor.Diamond) {
					j1_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Diamond);
					j1_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Spade);					
					j1_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Heart);
					j1_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Club);
					
					j2_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Diamond);
					j2_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Spade);
					j2_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Heart);
					j2_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Club);
					
					j3_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Diamond);
					j3_c0.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Diamond));
					j3_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Spade); 
					j3_c1.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Spade));
					j3_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Heart);
					j3_c2.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Heart));
					j3_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Club); 
					j3_c3.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Club));
				}
				else if(game.contract.getColor() == BidColor.Club) {
					j1_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Club);
					j1_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Spade);					
					j1_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Heart);
					j1_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarer,CardColor.Diamond);
					
					j2_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Club);
					j2_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Spade);
					j2_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Heart);
					j2_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerPartner,CardColor.Diamond);
					
					j3_c0 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Club);
					j3_c0.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Club));
					j3_c1 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Spade); 
					j3_c1.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Spade));
					j3_c2 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Heart);
					j3_c2.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Heart));
					j3_c3 = GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,declarerNext,CardColor.Diamond); 
					j3_c3.addAll(GameBridgeRule.getRemainingCardsOnColorForPlayer(game.distribution,game.cardPlayed,GameBridgeRule.getNextPosition(declarerPartner),CardColor.Diamond));
				}
				else {
					throw new GameClaimException("ERROR getnbTotalTrick game="+game);
				}
				
				int to_return = 0;
				
				/*The blocage's situation in each suit*/
				Blocage b_c0 = getBlocage(j1_c0, j2_c0, j3_c0);
				Blocage b_c1 = getBlocage(j1_c1, j2_c1, j3_c1);
				Blocage b_c2 = getBlocage(j1_c2, j2_c2, j3_c2);
				Blocage b_c3 = getBlocage(j1_c3, j2_c3, j3_c3);
				
				/*The number of direct tricks in each suit*/
				int n_c0 = getNbTricksColor(j1_c0,j2_c0,j3_c0,b_c0);
				int n_c1 = getNbTricksColor(j1_c1,j2_c1,j3_c1,b_c1);
				int n_c2 = getNbTricksColor(j1_c2,j2_c2,j3_c2,b_c2);
				int n_c3 = getNbTricksColor(j1_c3,j2_c3,j3_c3,b_c3);
				
				/*The number of unlockable tricks in each suit*/
				int n_u_c0 = getNbTricksUnlockableColor(j1_c0,j2_c0,j3_c0,b_c0);
				int n_u_c1 = getNbTricksUnlockableColor(j1_c1,j2_c1,j3_c1,b_c1);
				int n_u_c2 = getNbTricksUnlockableColor(j1_c2,j2_c2,j3_c2,b_c2);
				int n_u_c3 = getNbTricksUnlockableColor(j1_c3,j2_c3,j3_c3,b_c3);
				
				/*The number of potential ruff : > 0 if A ruffs; < 0 if B ruffs*/
				int n_r_c1 = getNbRuff(j1_c1,j2_c1,j3_c1);
				int n_r_c2 = getNbRuff(j1_c2,j2_c2,j3_c2);
				int n_r_c3 = getNbRuff(j1_c3,j2_c3,j3_c3);
				
				/**/
				int ad_tr = 0;
				/**/
				int tr_tr = 0;
				/**/
				int lo_tr = 0;
				
				/****Study on trumps****/
				if(j3_c0.size() > 0 && (joueurCourant == declarer || joueurCourant == declarerPartner))
				{
					if( (j3_c0.size() > 1 && n_c0 + 1 < j3_c0.size()) || j3_c0.size() > Math.max(j1_c0.size(), j2_c0.size()) || (joueurCourant == declarer && j1_c0.size() == 0) || (joueurCourant == declarerPartner && j2_c0.size() == 0)) {
						return 0;
					} else if(j3_c0.size() >= 1 && n_c0 + 1 >= j3_c0.size()){
						if(n_c0 >= 1){
							tr_tr += Math.min(n_c0,j3_c0.size());
							removeTrumps(j1_c0, j2_c0, j3_c0);
						}
					}
					if(j3_c0.size() == 1){
						removeTrump(j1_c0,j2_c0,j3_c0);
						joueurCourant = declarerNext;
					}
				}
				b_c0 = getBlocage(j1_c0, j2_c0, j3_c0);
				
				/****Study on communications****/
				/*if there is at least one level 0/1 --> we can take all the tricks*/
				if(b_c1 == Blocage._Niveau_0 || b_c1 == Blocage._Niveau_1_A || b_c1 == Blocage._Niveau_1_B 
				|| b_c2 == Blocage._Niveau_0 || b_c2 == Blocage._Niveau_1_A || b_c2 == Blocage._Niveau_1_B 
				|| b_c3 == Blocage._Niveau_0 || b_c3 == Blocage._Niveau_1_A || b_c3 == Blocage._Niveau_1_B )
				{
					ad_tr = Math.min(n_c1 + n_c2 + n_c3 + n_u_c1 + n_u_c2 + n_u_c3, maxTricks);
				}
				/*if there is a level 2 face to a level 2 --> we can take all the tricks*/
				else if( (b_c1 == Blocage._Niveau_2_A || b_c2 == Blocage._Niveau_2_A || b_c3 == Blocage._Niveau_2_A) 
				&&  (b_c1 == Blocage._Niveau_2_B || b_c2 == Blocage._Niveau_2_B || b_c3 == Blocage._Niveau_2_B ))
				{
					ad_tr = Math.min(n_c1 + n_c2 + n_c3 + n_u_c1 + n_u_c2 + n_u_c3, maxTricks);
				}
				/*if there is a level 3 face to a level 2 --> we can take all the tricks*/
				else if( ((b_c1 == Blocage._Niveau_2_A || b_c2 == Blocage._Niveau_2_A || b_c3 == Blocage._Niveau_2_A) 
						&& (b_c1 == Blocage._Niveau_3_B || b_c2 == Blocage._Niveau_3_B || b_c3 == Blocage._Niveau_3_B))
					||((b_c1 == Blocage._Niveau_2_B || b_c2 == Blocage._Niveau_2_B || b_c3 == Blocage._Niveau_2_B) 
						&& (b_c1 == Blocage._Niveau_3_A || b_c2 == Blocage._Niveau_3_A || b_c3 == Blocage._Niveau_3_A)))
				{
					ad_tr = Math.min(n_c1 + n_c2 + n_c3 + n_u_c1 + n_u_c2 + n_u_c3, maxTricks);
				}
				/*if there is a level 4 face to a level 2 --> we can take all the tricks*/
				else if(  ((b_c1 == Blocage._Niveau_2_A || b_c2 == Blocage._Niveau_2_A || b_c3 == Blocage._Niveau_2_A) 
						&& (b_c1 == Blocage._Niveau_4_B || b_c2 == Blocage._Niveau_4_B || b_c3 == Blocage._Niveau_4_B))
					||    ((b_c1 == Blocage._Niveau_2_B || b_c2 == Blocage._Niveau_2_B || b_c3 == Blocage._Niveau_2_B) 
						&& (b_c1 == Blocage._Niveau_4_A || b_c2 == Blocage._Niveau_4_A || b_c3 == Blocage._Niveau_4_A)))
				{
					ad_tr = Math.min(n_c1 + n_c2 + n_c3 + n_u_c1 + n_u_c2 + n_u_c3, maxTricks);
				}
				/*If there is a level 2 face to a ruff --> we can take all the tricks*/
				else if ( 
						((b_c1 == Blocage._Niveau_2_A || b_c2 == Blocage._Niveau_2_A || b_c3 == Blocage._Niveau_2_A) 
						&& (n_r_c1 < 0|| n_r_c2 < 0|| n_r_c3 < 0) 
						&& j1_c0.size() > 0)
					|| 	((b_c1 == Blocage._Niveau_2_B || b_c2 == Blocage._Niveau_2_B || b_c3 == Blocage._Niveau_2_B) 
						&& (n_r_c1 > 0|| n_r_c2 > 0|| n_r_c3 > 0)
						&& j2_c0.size() > 0))
				{
					ad_tr = n_c1+n_c2+n_c3+n_u_c1+n_u_c2+n_u_c3;
				}
				/*If there is a level 3 face to a ruff --> we can take all the tricks*/
				else if ( 
						((b_c1 == Blocage._Niveau_3_A || b_c2 == Blocage._Niveau_3_A || b_c3 == Blocage._Niveau_3_A) 
						&& (n_r_c1 < 0|| n_r_c2 < 0|| n_r_c3 < 0) 
						&& j1_c0.size() > 0)
					|| 	((b_c1 == Blocage._Niveau_3_B || b_c2 == Blocage._Niveau_3_B || b_c3 == Blocage._Niveau_3_B) 
						&& (n_r_c1 > 0|| n_r_c2 > 0|| n_r_c3 > 0)
						&& j2_c0.size() > 0))
				{
					ad_tr = n_c1+n_c2+n_c3+n_u_c1+n_u_c2+n_u_c3;
				}
				/*If there is a level 4 face to a ruff --> we can take all the tricks*/
				else if ( 
						((b_c1 == Blocage._Niveau_4_A || b_c2 == Blocage._Niveau_4_A || b_c3 == Blocage._Niveau_4_A) 
						&& (n_r_c1 < 0|| n_r_c2 < 0|| n_r_c3 < 0) 
						&& j1_c0.size() > 0)
					|| 	((b_c1 == Blocage._Niveau_4_B || b_c2 == Blocage._Niveau_4_B || b_c3 == Blocage._Niveau_4_B) 
						&& (n_r_c1 > 0|| n_r_c2 > 0|| n_r_c3 > 0)
						&& j2_c0.size() > 0))
				{
					ad_tr = n_c1+n_c2+n_c3+n_u_c1+n_u_c2+n_u_c3;
				}
				/*If there is a level 3 face to a trump's communication --> we can take all the tricks, but we lose a ruff trick*/
				else if ( 
					(	(b_c0 == Blocage._Niveau_0 || b_c0 == Blocage._Niveau_1_A || b_c0 == Blocage._Niveau_1_B || b_c0 == Blocage._Niveau_2_A || b_c0 == Blocage._Niveau_3_A || b_c0 == Blocage._Niveau_4_A) 
						&& (b_c1 == Blocage._Niveau_3_B || b_c2 == Blocage._Niveau_3_B || b_c3 == Blocage._Niveau_3_B))
					|| ((b_c0 ==Blocage. _Niveau_0 || b_c0 == Blocage._Niveau_1_A || b_c0 == Blocage._Niveau_1_B || b_c0 == Blocage._Niveau_2_B || b_c0 == Blocage._Niveau_3_B || b_c0 == Blocage._Niveau_4_B) 
						&& (b_c1 == Blocage._Niveau_3_A || b_c2 == Blocage._Niveau_3_A || b_c3 == Blocage._Niveau_3_A)))
				{
					
					/**MAJ DES ATOUTS : on enleve un atout de chaque main, le plus gros pour le d�clarant*/
					updateTrump(j1_c0, j2_c0);
					ad_tr = n_c1+n_c2+n_c3+n_u_c1+n_u_c2+n_u_c3;
					tr_tr += 1;
				}
				/*If there is a level 4 face to a trump's communication --> we can take all the tricks, but we lose a ruff trick*/
				else if ( 
					(	(b_c0 == Blocage._Niveau_0 || b_c0 == Blocage._Niveau_1_A || b_c0 == Blocage._Niveau_1_B || b_c0 == Blocage._Niveau_2_A || b_c0 == Blocage._Niveau_3_A || b_c0 == Blocage._Niveau_4_A) 
						&& (b_c1 == Blocage._Niveau_4_B || b_c2 == Blocage._Niveau_4_B || b_c3 == Blocage._Niveau_4_B))
					|| ((b_c0 == Blocage._Niveau_0 || b_c0 == Blocage._Niveau_1_A || b_c0 == Blocage._Niveau_1_B || b_c0 == Blocage._Niveau_2_B || b_c0 == Blocage._Niveau_3_B || b_c0 == Blocage._Niveau_4_B) 
						&& (b_c1 == Blocage._Niveau_4_A || b_c2 == Blocage._Niveau_4_A || b_c3 ==Blocage._Niveau_4_A)))
				{
					/**MAJ DES ATOUTS : on enleve un atout de chaque main, le plus gros du mort*/
					updateTrump(j2_c0, j1_c0);
					ad_tr = n_c1+n_c2+n_c3+n_u_c1+n_u_c2+n_u_c3;
					tr_tr += 1;
				}
				/*If there is a level 5 face to a trump's communication --> we can take all the tricks, but we lose a ruff trick*/
				else if ( 
					(	(b_c0 == Blocage._Niveau_0 || b_c0 == Blocage._Niveau_1_A || b_c0 == Blocage._Niveau_1_B || b_c0 == Blocage._Niveau_2_A || b_c0 == Blocage._Niveau_3_A || b_c0 == Blocage._Niveau_4_A) 
						&& (b_c1 == Blocage._Niveau_5_B || b_c2 == Blocage._Niveau_5_B || b_c3 == Blocage._Niveau_5_B))
					|| ((b_c0 == Blocage._Niveau_0 || b_c0 == Blocage._Niveau_1_A || b_c0 == Blocage._Niveau_1_B || b_c0 == Blocage._Niveau_2_B || b_c0 == Blocage._Niveau_3_B || b_c0 == Blocage._Niveau_4_B) 
						&& (b_c1 == Blocage._Niveau_5_A || b_c2 == Blocage._Niveau_5_A || b_c3 ==Blocage._Niveau_5_A)))
				{
					/**MAJ DES ATOUTS : on enleve un atout de chaque main, le plus gros du mort*/
					updateTrump(j2_c0, j1_c0);
					ad_tr = n_c1+n_c2+n_c3+n_u_c1+n_u_c2+n_u_c3;
					tr_tr += 1;
				}
				/*if there is a level 3/4 face to a level 3/4*/
				else if( ((b_c1 == Blocage._Niveau_3_A || b_c2 == Blocage._Niveau_3_A || b_c3 == Blocage._Niveau_3_A) 
						|| b_c1 == Blocage._Niveau_4_A || b_c2 == Blocage._Niveau_4_A || b_c3 == Blocage._Niveau_4_A)
					&&   ((b_c1 == Blocage._Niveau_3_B || b_c2 == Blocage._Niveau_3_B || b_c3 == Blocage._Niveau_3_B) 
						 ||b_c1 == Blocage._Niveau_4_B || b_c2 == Blocage._Niveau_4_B || b_c3 == Blocage._Niveau_4_B))
				{
					/*Unlockable tricks for A*/
					int n_u_A = 0;
					/*Unlockable tricks for B*/
					int n_u_B = 0;
					/*Communications to A*/
					int com_A = 0;
					/*Communications to B*/
					int com_B = 0;
					
					/*c1*/
					if(b_c1 == Blocage._Niveau_3_A || b_c1 == Blocage._Niveau_4_A){
						n_u_B += n_u_c1;
						com_A ++;
					} else {
						if(b_c1 == Blocage._Niveau_3_B || b_c1 == Blocage._Niveau_4_B) {
							n_u_A += n_u_c1;
							com_B ++;							
						}
					}
					/*c2*/
					if(b_c2 == Blocage._Niveau_3_A || b_c2 == Blocage._Niveau_4_A){
						n_u_B += n_u_c2;
						com_A ++;
					}else{
						if(b_c2 == Blocage._Niveau_3_B || b_c2 == Blocage._Niveau_4_B){
							n_u_A += n_u_c2;
							com_B ++;							
						}
					}
					/*c3*/
					if(b_c3 == Blocage._Niveau_3_A || b_c3 == Blocage._Niveau_4_A){
						n_u_B += n_u_c3;
						com_A ++;
					}else{
						if(b_c3 == Blocage._Niveau_3_B || b_c3 == Blocage._Niveau_4_B){
							n_u_A += n_u_c3;
							com_B ++;							
						}
					}
					
					/*I�) n_u_A > n_u_B --> more tricks unlockable in declarant*/
					if(n_u_A > n_u_B){
						int n_w_c1_B = getNbWinningCardColor(j2_c1, j3_c1);
						int n_w_c2_B = getNbWinningCardColor(j2_c2, j3_c2);
						int n_w_c3_B = getNbWinningCardColor(j2_c3, j3_c3);
						
						int posLast = 0;
						
						/*c1*/
						if(b_c1 == Blocage._Niveau_3_A && (n_c1 >= 2 || com_A >= 2) && n_w_c1_B >= 2){
							n_u_A = n_u_A + n_w_c1_B - 1;
							com_A --;
							posLast = 1;
						}
						/*c2*/
						if(b_c2 == Blocage._Niveau_3_A  && n_w_c2_B >= 2){
							if(n_c2 >= 2 || com_A >= 2){
								n_u_A = n_u_A + n_w_c2_B -1;
								com_A --;
								if(posLast == 0 || (n_w_c1_B > n_w_c2_B)){
									posLast = 2;
								}
							} else if(posLast == 1 && n_w_c2_B > n_w_c1_B){
								n_u_A = n_u_A + n_w_c2_B - n_w_c1_B;
								posLast = 2;
							}
						}
						/*c3*/
						if(b_c3 == Blocage._Niveau_3_A && n_w_c3_B >= 2){
							if(n_c3 >= 2 || com_A >= 2){
								n_u_A = n_u_A + n_w_c3_B -1;
								com_A --;
							}else if(posLast == 1 && n_w_c3_B > n_w_c1_B){
								n_u_A = n_u_A + n_w_c3_B - n_w_c1_B;
							}else if(posLast == 2 && n_w_c3_B > n_w_c2_B){
								n_u_A = n_u_A + n_w_c3_B - n_w_c2_B;
							}
						}
						ad_tr = Math.min(n_c1+n_c2+n_c3+n_u_A, maxTricks);
					}
					/*II�) n_u_A < n_u_B*/
					else if(n_u_A < n_u_B){
						int n_w_c1_A = getNbWinningCardColor(j1_c1, j3_c1);
						int n_w_c2_A = getNbWinningCardColor(j1_c2, j3_c2);
						int n_w_c3_A = getNbWinningCardColor(j1_c3, j3_c3);
						int posLast = 0;
						
						/*c1*/
						if(b_c1 == Blocage._Niveau_3_B && (n_c1 >= 2 || com_B >= 2) && n_w_c1_A >= 2){
							n_u_B = n_u_B + n_w_c1_A - 1;
							com_B --;
							posLast = 1;
						}
						/*c2*/
						if(b_c2 == Blocage._Niveau_3_B  && n_w_c2_A >= 2){
							if(n_c2 >= 2 || com_B >= 2){
								n_u_B = n_u_B + n_w_c2_A -1;
								com_B --;
								if(posLast == 0 || (n_w_c1_A > n_w_c2_A) ){
									posLast = 2;
								}
							}
							else if(posLast == 1 && n_w_c2_A > n_w_c1_A){
								n_u_B = n_u_B + n_w_c2_A - n_w_c1_A;
								posLast = 2;
							}
						}
						/*c3*/
						if(b_c3 == Blocage._Niveau_3_B && n_w_c3_A >= 2){
							if(n_c3 >= 2 || com_B >= 2){
								n_u_B = n_u_B + n_w_c3_A -1;
								com_B --;
							}else if(posLast == 1 && n_w_c3_A > n_w_c1_A){
								n_u_B = n_u_B + n_w_c3_A - n_w_c1_A;
							}else if(posLast == 2 && n_w_c3_A > n_w_c2_A){
								n_u_B = n_u_B + n_w_c3_A - n_w_c2_A;
							}
						}
						ad_tr = Math.min(n_c1+n_c2+n_c3+n_u_B, maxTricks);
					}
					/*III�) n_u_A = n_u_B*/
					else
					{
						int n_w_c1_A = getNbWinningCardColor(j1_c1, j3_c1);
						int n_w_c2_A = getNbWinningCardColor(j1_c2, j3_c2);
						int n_w_c3_A = getNbWinningCardColor(j1_c3, j3_c3);
						
						int n_w_c1_B = getNbWinningCardColor(j2_c1, j3_c1);
						int n_w_c2_B = getNbWinningCardColor(j2_c2, j3_c2);
						int n_w_c3_B = getNbWinningCardColor(j2_c3, j3_c3);
						
						int posLastA = 0;
						int posLastB = 0;
						
						/*c1 A*/
						if(b_c1 == Blocage._Niveau_3_A && (n_c1 >= 2 || com_A >= 2) && n_w_c1_B >= 2){
							n_u_A = n_u_A + n_w_c1_B - 1;
							com_A --;
							posLastA = 1;
						}
						/*c2 A*/
						if(b_c2 == Blocage._Niveau_3_A  && n_w_c2_B >= 2){
							if(n_c2 >= 2 || com_A >= 2){
								n_u_A = n_u_A + n_w_c2_B -1;
								com_A --;
								if(posLastA == 0 || (n_w_c1_B > n_w_c2_B) ){
									posLastA = 2;
								}
							}
							else if(posLastA == 1 && n_w_c2_B > n_w_c1_B){
								n_u_A = n_u_A + n_w_c2_B - n_w_c1_B;
								posLastA = 2;
							}
						}
						/*c3 A*/
						if(b_c3 == Blocage._Niveau_3_A && n_w_c3_B >= 2){
							if(n_c3 >= 2 || com_A >= 2){
								n_u_A = n_u_A + n_w_c3_B -1;
								com_A --;
							}else if(posLastA == 1 && n_w_c3_B > n_w_c1_B){
								n_u_A = n_u_A + n_w_c3_B - n_w_c1_B;
							}else if(posLastA == 2 && n_w_c3_B > n_w_c2_B){
								n_u_A = n_u_A + n_w_c3_B - n_w_c2_B;
							}
						}
						
						/*c1 B*/
						if(b_c1 == Blocage._Niveau_3_B && (n_c1 >= 2 || com_B >= 2) && n_w_c1_A >= 2){
							n_u_B = n_u_B + n_w_c1_A - 1;
							com_B --;
							posLastB = 1;
						}
						/*c2 B*/
						if(b_c2 == Blocage._Niveau_3_B  && n_w_c2_A >= 2){
							if(n_c2 >= 2 || com_B >= 2){
								n_u_B = n_u_B + n_w_c2_A -1;
								com_B --;
								if(posLastB == 0 || (n_w_c1_A > n_w_c2_A) ){
									posLastB = 2;
								}
							}else if(posLastB == 1 && n_w_c2_A > n_w_c1_A){
								n_u_B = n_u_B + n_w_c2_A - n_w_c1_A;
								posLastB = 2;
							}
						}
						/*c3 B*/
						if(b_c3 == Blocage._Niveau_3_B && n_w_c3_A >= 2){
							if(n_c3 >= 2 || com_B >= 2){
								n_u_B = n_u_B + n_w_c3_A -1;
								com_B --;
							}else if(posLastB == 1 && n_w_c3_A > n_w_c1_A){
								n_u_B = n_u_B + n_w_c3_A - n_w_c1_A;
							}else if(posLastB == 2 && n_w_c3_A > n_w_c2_A){
								n_u_B = n_u_B + n_w_c3_A - n_w_c2_A;
							}
						}
						ad_tr = Math.min(Math.max(n_u_A, n_u_B)+n_c1+n_c2+n_c3, maxTricks);
					}
				}
				/*Level 3  but all in the same hand*/
				else if(  (b_c1 == Blocage._Niveau_3_A && (n_c1 >= 2 || joueurCourant == declarer)  ) || (b_c2 == Blocage._Niveau_3_A && (n_c2 >= 2 || joueurCourant == declarer)) || (b_c3 == Blocage._Niveau_3_A && (n_c3 >= 2 || joueurCourant == declarer)) 
						||(b_c1 == Blocage._Niveau_3_B && (n_c1 >= 2 || joueurCourant == declarer)  ) || (b_c2 == Blocage._Niveau_3_B && (n_c2 >= 2 || joueurCourant == declarer)) || (b_c3 == Blocage._Niveau_3_B && (n_c3 >= 2 || joueurCourant == declarer) ))
				{
					ad_tr = Math.min(n_c1+n_c2+n_c3+n_u_c1+n_u_c2+n_u_c3 - 1, maxTricks);
				}
				/*Level 2/4 face to the current player or level 3*/
				else if( ((   b_c1 == Blocage._Niveau_2_A || b_c1 == Blocage._Niveau_4_A
						   || b_c2 == Blocage._Niveau_2_A || b_c2 == Blocage._Niveau_4_A
						   || b_c3 == Blocage._Niveau_2_A || b_c3 == Blocage._Niveau_4_A) 
						  && (joueurCourant==declarerPartner))
						|| (( b_c1 == Blocage._Niveau_2_B || b_c1 == Blocage._Niveau_4_B
						   || b_c2 == Blocage._Niveau_2_B || b_c2 == Blocage._Niveau_4_B
						   || b_c3 == Blocage._Niveau_2_B || b_c3 == Blocage._Niveau_4_B) 
						  && (joueurCourant==declarer))
						|| b_c1 == Blocage._Niveau_3_A || b_c1 == Blocage._Niveau_3_B
						|| b_c2 == Blocage._Niveau_3_A || b_c2 == Blocage._Niveau_3_B
						|| b_c3 == Blocage._Niveau_3_A || b_c3 == Blocage._Niveau_3_B	)
				{
					ad_tr = Math.min(n_c1+n_c2+n_c3, maxTricks);
				}
				/*Otherwise, there is no communication*/
				else
				{
					/*A to play*/
					if(joueurCourant == declarer){
						ad_tr = getNbWinningCardColor(j1_c1,j3_c1) + getNbWinningCardColor(j1_c2,j3_c2) + getNbWinningCardColor(j1_c3,j3_c3);
					}
					/*B to play*/
					else if(joueurCourant == declarerPartner){
						ad_tr = getNbWinningCardColor(j2_c1,j3_c1) + getNbWinningCardColor(j2_c2,j3_c2) + getNbWinningCardColor(j2_c3,j3_c3);
					}
					/*We don't know who will get the hand ... so we imagine the worst case*/
					else{
						ad_tr = Math.min(getNbWinningCardColor(j1_c1,j3_c1) + getNbWinningCardColor(j1_c2,j3_c2) + getNbWinningCardColor(j1_c3,j3_c3) , getNbWinningCardColor(j2_c1,j3_c1) + getNbWinningCardColor(j2_c2,j3_c2) + getNbWinningCardColor(j2_c3,j3_c3));
					}
				}
				
				/****Study on trump tricks****/
				int com_A_B_c1 = getNbGoBack(j1_c1, j2_c1, j3_c1);
				int com_B_A_c1 = getNbGoBack(j2_c1, j1_c1, j3_c1);
				int com_A_B_c2 = getNbGoBack(j1_c2, j2_c2, j3_c2);
				int com_B_A_c2 = getNbGoBack(j2_c2, j1_c2, j3_c2);
				int com_A_B_c3 = getNbGoBack(j1_c3, j2_c3, j3_c3);
				int com_B_A_c3 = getNbGoBack(j2_c3, j1_c3, j3_c3);
				
				int com_A_B = com_A_B_c1 + com_A_B_c2 + com_A_B_c3+Math.max(0, n_r_c1)+Math.max(0, n_r_c2)+Math.max(0, n_r_c3);
				int com_B_A = com_B_A_c1 + com_B_A_c2 + com_B_A_c3- Math.min(0, n_r_c1) - Math.min(0, n_r_c2) - Math.min(0, n_r_c3);
				
				if(joueurCourant == declarer){
					com_A_B ++;
				}else if (joueurCourant == declarerPartner){
					com_B_A ++;
				}
				
				int n_r_A = Math.min(Math.min(Math.max(0, n_r_c1)+Math.max(0, n_r_c2)+Math.max(0, n_r_c3),com_A_B), j1_c0.size());
				int n_r_B = Math.min(Math.min(0 - Math.min(0, n_r_c1) - Math.min(0, n_r_c2) - Math.min(0, n_r_c3),com_B_A), j2_c0.size());
				
				tr_tr += n_r_A + n_r_B + Math.max(j1_c0.size() - n_r_A,j2_c0.size() - n_r_B);
				
				/****Study on discards****/
				if(joueurCourant == declarerNext){
					if(j3_c0.size() > 0){
						return 0;
					}else{
						/*Immediate Losers*/
						int p_c1 = getNbImmediateLoserColor(j1_c1,j1_c0.size(),j2_c1,j2_c0.size(),j3_c1);
						int p_c2 = getNbImmediateLoserColor(j1_c2,j1_c0.size(),j2_c2,j2_c0.size(),j3_c2);
						int p_c3 = getNbImmediateLoserColor(j1_c3,j1_c0.size(),j2_c3,j2_c0.size(),j3_c3);
						
						int discard_A = Math.max(0, p_c1 - j1_c1.size()) + Math.max(0, p_c2 - j1_c2.size()) + Math.max(0, p_c3 - j1_c3.size()) ;
						int discard_B = Math.max(0, p_c1 - j2_c1.size()) + Math.max(0, p_c2 - j2_c2.size()) + Math.max(0, p_c3 - j2_c3.size()) ;
					
						int useless_card_A_c1 = Math.max(0,getNbUselessCard(j1_c1,j2_c1,j3_c1) -p_c1);
						int useless_card_A_c2 = Math.max(0,getNbUselessCard(j1_c2,j2_c2,j3_c2) -p_c2);
						int useless_card_A_c3 = Math.max(0,getNbUselessCard(j1_c3,j2_c3,j3_c3) -p_c3);
						int useless_card_B_c1 = Math.max(0,getNbUselessCard(j2_c1,j1_c1,j3_c1) -p_c1);
						int useless_card_B_c2 = Math.max(0,getNbUselessCard(j2_c2,j1_c2,j3_c2) -p_c2);
						int useless_card_B_c3 = Math.max(0,getNbUselessCard(j2_c3,j1_c3,j3_c3) -p_c3);
						
						boolean A_mort =  (b_c1 == Blocage._Niveau_2_B || b_c1 == Blocage._Niveau_4_B ||b_c1 == Blocage._Niveau_5_B || b_c1 == Blocage._Niveau_6)
										&& (b_c2 == Blocage._Niveau_2_B || b_c2 == Blocage._Niveau_4_B ||b_c2 == Blocage._Niveau_5_B || b_c2 == Blocage._Niveau_6)
										&& (b_c3 == Blocage._Niveau_2_B || b_c3 == Blocage._Niveau_4_B ||b_c3 == Blocage._Niveau_5_B || b_c3 == Blocage._Niveau_6);
						boolean B_mort =   (b_c1 == Blocage._Niveau_2_A || b_c1 == Blocage._Niveau_4_A ||b_c1 == Blocage._Niveau_5_A || b_c1 == Blocage._Niveau_6)
										&& (b_c2 == Blocage._Niveau_2_A || b_c2 == Blocage._Niveau_4_A ||b_c2 == Blocage._Niveau_5_A || b_c2 == Blocage._Niveau_6)
										&& (b_c3 == Blocage._Niveau_2_A || b_c3 == Blocage._Niveau_4_A ||b_c3 == Blocage._Niveau_5_A || b_c3 == Blocage._Niveau_6);
						
						if(useless_card_A_c1 + useless_card_A_c2 + useless_card_A_c3 <= discard_A || useless_card_B_c1 + useless_card_B_c2 + useless_card_B_c3 <= discard_B){
							if( Math.max(discard_A - (useless_card_A_c1 + useless_card_A_c2 + useless_card_A_c3),0 ) + Math.max(discard_B - ( useless_card_B_c1 + useless_card_B_c2 + useless_card_B_c3),0 ) <= to_return - Math.min(to_return,maxTricks-p_c1-p_c2-p_c3) 
									|| (Math.max(discard_A - (useless_card_A_c1 + useless_card_A_c2 + useless_card_A_c3),0 ) <= to_return - Math.min(to_return,maxTricks-p_c1-p_c2-p_c3) && (B_mort))
									|| (Math.max(discard_B - (useless_card_B_c1 + useless_card_B_c2 + useless_card_B_c3),0 ) <= to_return - Math.min(to_return,maxTricks-p_c1-p_c2-p_c3) && (A_mort)))
							{								
								/*Discard of winners isn't a problem TODO unsafe*/
								if(Math.max(0, n_c1 - 1)+Math.max(0, n_c2 - 1)+Math.max(0, n_c3 - 1) >= discard_A + discard_B - ( useless_card_B_c1 + useless_card_B_c2 + useless_card_B_c3) - (useless_card_A_c1 + useless_card_A_c2 + useless_card_A_c3)
								||(Math.max(0, n_c1 - 1)+Math.max(0, n_c2 - 1)+Math.max(0, n_c3 - 1) >= discard_A - ( useless_card_A_c1 + useless_card_A_c2 + useless_card_A_c3)&&(B_mort))
								||(Math.max(0, n_c1 - 1)+Math.max(0, n_c2 - 1)+Math.max(0, n_c3 - 1) >= discard_B - ( useless_card_B_c1 + useless_card_B_c2 + useless_card_B_c3)&&(A_mort)))
								{
									return Math.min(to_return, maxTricks -  p_c1 - p_c2 - p_c3);
								}
								/*Discard of winners cost trick(s)*/
								else{
									return 0;
								}
							}else{
								return 0;
							}
						}else{
							return Math.max(Math.min(tr_tr+ad_tr, maxTricks) - Math.max(lo_tr+Math.min(tr_tr+ad_tr, maxTricks)-maxTricks, 0), 0);
						}
					}
				}else{
					return Math.min(tr_tr+ad_tr, maxTricks);
				}
			}
		}else{
			return -1;
		}
	}
}
