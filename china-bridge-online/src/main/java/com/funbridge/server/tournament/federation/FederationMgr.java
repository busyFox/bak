package com.funbridge.server.tournament.federation;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.common.ContextManager;
import com.funbridge.server.ws.FBExceptionType;
import com.funbridge.server.ws.FBWSException;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import static com.funbridge.server.message.MessageNotifMgr.MESSAGE_CATEGORY_CBO;

/**
 * Created by luke on 08/08/2017.
 */
public class FederationMgr {

    private static SimpleDateFormat sdfGlobal = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public static List<Integer> categories = new ArrayList<>();
    static {
        categories.add(Constantes.TOURNAMENT_CATEGORY_TOUR_CBO);
    }

    /**
     * Get a federation tournament manager by its name
     * @param federation
     * @return
     * @throws FBWSException
     */
    public static TourFederationMgr getTourFederationMgr(String federation) throws FBWSException {
       if(federation.equalsIgnoreCase(Constantes.TOURNAMENT_CBO)) {
            return ContextManager.getTourCBOMgr();
        } else {
            throw new FBWSException(FBExceptionType.COMMON_PARAMETER_NOT_VALID);
        }
    }

    /**
     * Get a federation tournament manager by its product category
     * @param productCategory
     * @return
     * @throws FBWSException
     */
    public static TourFederationMgr getTourFederationMgrFromProductCategory(int productCategory) throws FBWSException {
        return getTourFederationMgr(getFederationNameFromProductCategory(productCategory));
    }

    /**
     * Get federation name from tour category
     * @param tourCategory
     * @return
     */
    public static String getFederationNameFromTourCategory(int tourCategory) {
        switch (tourCategory) {
            case Constantes.TOURNAMENT_CATEGORY_TOUR_CBO:
                return Constantes.TOURNAMENT_CBO;
        }
        return "";
    }

    /**
     * Get federation name from product category
     * @param productCategory
     * @return
     */
    public static String getFederationNameFromProductCategory(int productCategory) {
        switch (productCategory) {
            case Constantes.PRODUCT_CATEGORY_TOUR_CBO:
                return Constantes.TOURNAMENT_CBO;
        }
        return "";
    }

    /**
     * Get message category from federation name
     * @param federation
     * @return
     */
    public static int getMessageCategoryFromFederationName(String federation) {
        if(federation.equalsIgnoreCase(Constantes.TOURNAMENT_CBO)) {
            return MESSAGE_CATEGORY_CBO;
        } else {
            return 0;
        }
    }

    /**
     * Get message category from federation name
     * @param federation
     * @return
     */
    public static SimpleDateFormat getSdfFromFederationName(String federation) {
            return sdfGlobal;

    }

    /**
     * Check if category corresponds to a federation
     * @param category
     * @return
     */
    public static boolean isCategoryFederation(int category) {
        switch (category) {
            case Constantes.TOURNAMENT_CATEGORY_TOUR_CBO:
                return true;
        }
        return false;
    }

    /**
     * Check if product category corresponds to a federation
     * @param category
     * @return
     */
    public static boolean isProductCategoryFederation(int category) {
        switch (category) {
            case Constantes.PRODUCT_CATEGORY_TOUR_CBO:
                return true;
        }
        return false;
    }

    /**
     * Check if name corresponds to a federation
     * @param name
     * @return
     */
    public static boolean isNameFederation(String name) {
        return Constantes.TOURNAMENT_CBO.equalsIgnoreCase(name);
    }

}
