package com.funbridge.server.ws.support;

import javax.xml.bind.annotation.XmlRootElement;

import com.funbridge.server.common.Constantes;
import com.funbridge.server.store.data.Transaction;

@XmlRootElement(name="transaction")
public class WSSupTransaction {
	public long transactionID = -1;
	public String date = "";
	public String type = "";
	public String description = "";
	public String information = "";
	
	public WSSupTransaction(Transaction st) {
		transactionID = st.getID();
		if (st.getTransactionDate() > 0) {
			date = Constantes.timestamp2StringDateHour(st.getTransactionDate());
		}
		if (st.getProduct().getDescription() != null) {
			description = st.getProduct().getDescription();
		}
		type = st.getProduct().getType();
		if (st.getProduct().isTypeSupport() && st.getInformation() != null) {
			information = st.getInformation();
		}
	}
}
