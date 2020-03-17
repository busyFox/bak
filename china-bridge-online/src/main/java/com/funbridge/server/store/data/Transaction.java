package com.funbridge.server.store.data;

import com.funbridge.server.common.Constantes;

import javax.persistence.*;

@Entity
@Table(name="transaction")
@NamedQueries({
	@NamedQuery(name="transaction.listForPlayer", query="select t from Transaction t where t.playerID=:playerID order by t.transactionDate desc"),
	@NamedQuery(name="transaction.countForPlayerAndBonus", query="select count(t) from Transaction t where t.playerID=:playerID and t.product.bonus=:bonus order by t.transactionDate desc")
})
public class Transaction {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id")
	private long ID;
	
	@Column(name="store_transaction_id")
	private String storeTransactionID = "";
	
	@ManyToOne
	@JoinColumn(name="product_id", nullable=false)
	private Product product;
	
	@Column(name="player_id")
	private long playerID;
	
	@Column(name="transaction_date")
	private long transactionDate;
	
	@Column(name="version")
	private String version;
	
	@Column(name="information")
	private String information = "";

    @Column(name="transaction_type")
    private String transactionType = "";


	public String getStoreTransactionID() {
		return storeTransactionID;
	}
	public void setStoreTransactionID(String storeTransactionID) {
		this.storeTransactionID = storeTransactionID;
	}
	public Product getProduct() {
		return product;
	}
	public void setProduct(Product sp) {
		this.product = sp;
	}
	public long getPlayerID() {
		return playerID;
	}
	public void setPlayerID(long playerID) {
		this.playerID = playerID;
	}
	public long getTransactionDate() {
		return transactionDate;
	}
	public void setTransactionDate(long transactionDate) {
		this.transactionDate = transactionDate;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public long getID() {
		return ID;
	}
	public void setID(long iD) {
		ID = iD;
	}
	public String getInformation() {
		return information;
	}
	public void setInformation(String info) {
		this.information = info;
	}
    public String getTransactionType() {
        return transactionType;
    }
    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    @Override
    public String toString() {
        return "ID="+ID+" - playerID="+playerID+" - date="+ Constantes.timestamp2StringDateHour(transactionDate)+" - product="+product+" - type="+transactionType;
    }
}
