package com.funbridge.server.store.dao;

import com.funbridge.server.store.data.Product;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Repository;

import javax.persistence.*;
import java.util.List;

@Repository(value="productDAO")
public class ProductDAO {
	@PersistenceContext
	private EntityManager em;
	
	protected Logger log = LogManager.getLogger(this.getClass());
	
	/**
	 * Return a store product for this product ID
	 * @param productID
	 * @return null if not found or many with the same product ID
	 */
	public Product getProductForProductID(String productID) {
		Query q = em.createNamedQuery("product.getForProductID");
		q.setParameter("productID", productID);
		try {
			return (Product)q.getSingleResult();
		} catch (NoResultException e) {
			// no product found !
		} catch (NonUniqueResultException e) {
			log.error("Many result for productID="+productID);
		} catch (Exception e) {
			log.error("Error to getForProductID - productID="+productID,e);
		}
		return null;
	}
	
	/**
	 * List all product with status enable = true
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Product> listAllProductEnable() {
		try {
			Query q = em.createNamedQuery("product.listAllEnable");
			return q.getResultList();
		} catch (Exception e) {
			log.error("Error to listAllProduct",e);
		}
		return null;
	}
	
	/**
	 * List all product for type with status enable = true
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Product> listEnableProductsForTypeAndCategory(String type, int category) {
		try {
			Query q = em.createNamedQuery("product.listEnableForTypeAndCategory");
			q.setParameter("type", type);
			q.setParameter("category", category);
			return q.getResultList();
		} catch (Exception e) {
			log.error("Error to listEnableForTypeAndCategory",e);
		}
		return null;
	}

	/**
	 * List all product with status enable = true
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Product> listAllProduct() {
		try {
			Query q = em.createNamedQuery("product.listAll");
			return q.getResultList();
		} catch (Exception e) {
			log.error("Error to listAllProduct",e);
		}
		return null;
	}
}
