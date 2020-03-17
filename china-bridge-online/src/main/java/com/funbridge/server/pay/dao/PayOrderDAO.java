package com.funbridge.server.pay.dao;

import com.funbridge.server.pay.data.PayOrder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

@Repository(value="payOrderDAO")
public class PayOrderDAO {
    @PersistenceContext
    private EntityManager em;

    protected Logger log = LogManager.getLogger(this.getClass());

    public boolean insertPayOrder(PayOrder payOrder){
        if (payOrder != null) {
            try {
                em.persist(payOrder);
                return true;
            } catch (Exception e) {
                log.error("Error trying to persist payOrder : id="+ payOrder.getuId(), e);
            }
        } else {
            log.error("Parameter payOrder is null");
        }

        return false ;
    }

    public  boolean isLocalOrderNoExist(String localOrderId){
        try {
            Query query = em.createNamedQuery("payOrder.getForLocalOrderNo");
            query.setParameter("localOrderNo", localOrderId);
            if (query.getResultList().size() > 0 && query.getResultList().get(0) != null){
                return true ;
            }
            return false;
        }
        catch (NoResultException e) {
            log.error("Exception on getSingleResult for localOrderId="+localOrderId, e);
            return false;
        } catch (Exception e) {
            log.error("Exception on getSingleResult for localOrderId="+localOrderId, e);
            return false;
        }
    }

    public  PayOrder getPayOrderforLocalOrderNo(String localOrderId){
        try {
            Query query = em.createNamedQuery("payOrder.getForLocalOrderNo");
            query.setParameter("localOrderNo", localOrderId);
            PayOrder payOrder = (PayOrder) query.getSingleResult() ;
            return payOrder;
        }
        catch (NoResultException e) {
            log.error("Exception on getSingleResult for localOrderId="+localOrderId, e);
            return null;
        } catch (Exception e) {
            log.error("Exception on getSingleResult for localOrderId="+localOrderId, e);
            return null;
        }
    }

    public PayOrder updatePayOrder(PayOrder p){
        if (p != null) {
            try {
                return em.merge(p);
            } catch (Exception e) {
                log.error("Error trying to merge player : id="+p.toString(), e);
            }
        } else {
            log.error("Parameter player is null");
        }
        return null;
    }
}
