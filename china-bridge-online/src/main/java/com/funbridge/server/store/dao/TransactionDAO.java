package com.funbridge.server.store.dao;

import com.funbridge.server.store.data.Transaction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.List;

@Repository(value="transactionDAO")
public class TransactionDAO {
	@PersistenceContext
	private EntityManager em;
	
	protected Logger log = LogManager.getLogger(this.getClass());
	
	public Transaction createTransaction(Transaction st) {
		em.persist(st);
		return st;
	}

	@SuppressWarnings("unchecked")
	public List<Transaction> listForPlayer(long playerID, int offset, int max) {
		Query q = em.createNamedQuery("transaction.listForPlayer");
		q.setParameter("playerID", playerID);
		if (offset > 0) q.setFirstResult(offset);
		if (max > 0) q.setMaxResults(max);
		return q.getResultList();
	}
	
	public int countForPlayerAndBonus(long playerID, boolean bonus) {
		Query q = em.createNamedQuery("transaction.countForPlayerAndBonus");
		q.setParameter("playerID", playerID);
		q.setParameter("bonus", bonus);
		long result = (Long)q.getSingleResult();
		return (int)result;
	}
}
