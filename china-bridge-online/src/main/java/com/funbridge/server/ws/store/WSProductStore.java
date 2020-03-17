package com.funbridge.server.ws.store;

import com.funbridge.server.store.data.Product;

/**
 * Created by pserent on 18/06/2015.
 */
public class WSProductStore {
    public String productID;
    public boolean promo;
    public String originalProductID;
    public String color;
    public int type; //0 : packs donnes, 1 : Abonnement donnes, 2 : pack tournoi donnes commentés
    public int nbUnits;

    public WSProductStore() {
    }

    public WSProductStore(Product sp) {
        this.productID = sp.getProductID();
        this.promo = (sp.getOriginalProductID() != null) && (sp.getOriginalProductID().length() > 0);
        this.originalProductID = sp.getOriginalProductID();
        if (sp.getColor() != null && sp.getColor().length() > 0) {
            this.color = sp.getColor();
        }
        this.type = sp.getCategory();
        this.nbUnits = sp.getCreditAmount();
    }
}
