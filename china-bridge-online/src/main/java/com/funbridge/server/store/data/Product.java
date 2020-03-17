package com.funbridge.server.store.data;

import com.funbridge.server.common.Constantes;

import javax.persistence.*;

@Entity
@Table(name="product")
@NamedQueries({
	@NamedQuery(name="product.getForProductID", query="select p from Product p where p.productID=:productID"),
	@NamedQuery(name="product.listAllEnable", query="select p from Product p where p.enable=1"),
	@NamedQuery(name="product.listEnableForTypeAndCategory", query="select p from Product p where p.enable=1 and p.type=:type and p.category=:category order by p.category asc, p.creditAmount asc"),
	@NamedQuery(name="product.listAll", query="select p from Product p")
})
public class Product {
	@Id
	@GeneratedValue(strategy=GenerationType.AUTO)
	@Column(name="id", nullable=false)
	private long ID;
	
	@Column(name="product_id")
	private String productID;

	@Column(name="enable")
	private boolean enable;
	
	@Column(name="amount_credit")
	private int creditAmount = 0;

	@Column(name="credit_type")
	private String creditType;

	@Column(name="price")
	private int price;
	
	@Column(name="bonus")
	private boolean bonus;
	
	@Column(name="type")
	private String type;
	
	@Column(name="description")
	private String description;
	
	@Column(name="prod_cat")
	private int category = Constantes.PRODUCT_CATEGORY_DEAL;

    @Column(name="original_product_id")
    private String originalProductID;

    @Column(name="color")
    private String color;
	
	public String toString() {
		return "ID="+ID+" - productID="+productID+" - category="+category+" - enable="+enable+" - credit="+creditAmount+" - bonus="+bonus+" - type="+type+" - description="+description;
	}
	
	public long getID() {
		return ID;
	}

	public void setID(long iD) {
		ID = iD;
	}

	public String getProductID() {
		return productID;
	}

	public void setProductID(String productID) {
		this.productID = productID;
	}

	public boolean isEnable() {
		return enable;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
	}

	public int getCreditAmount() {
		return creditAmount;
	}

	public void setCreditAmount(int creditAmount) {
		this.creditAmount = creditAmount;
	}

	public String getCreditType() {
		return creditType;
	}

	public void setCreditType(String creditType) {
		this.creditType = creditType;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
	
	public boolean isTypeSupport() {
		if (type != null) {
			return type.equals(Constantes.PRODUCT_TYPE_SUPPORT);
		}
		return false;
	}

	public int getCategory() {
		return category;
	}

	public void setCategory(int category) {
		this.category = category;
	}

    public boolean isBonus() {
        return bonus;
    }

    public void setBonus(boolean bonus) {
        this.bonus = bonus;
    }


    public String getOriginalProductID() {
        return originalProductID;
    }

    public void setOriginalProductID(String originalProductID) {
        this.originalProductID = originalProductID;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

}
