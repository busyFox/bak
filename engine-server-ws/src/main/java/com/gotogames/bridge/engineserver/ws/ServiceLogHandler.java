package com.gotogames.bridge.engineserver.ws;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.ByteArrayOutputStream;
import java.util.Set;

public class ServiceLogHandler implements SOAPHandler<SOAPMessageContext> {
	private Logger log = LogManager.getLogger(this.getClass());

	@Override
	public Set<QName> getHeaders() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close(MessageContext context) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean handleFault(SOAPMessageContext context) {
		logToSystemOut(context);
		return true;
	}

	@Override
	public boolean handleMessage(SOAPMessageContext context) {
		logToSystemOut(context);
		return true;
	}

	private void logToSystemOut(SOAPMessageContext smc) {
		Boolean outboundProperty = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (log.isDebugEnabled()) {
            if (outboundProperty.booleanValue()) {
                log.debug("Outgoing message from web service provider:");
            } else {
                log.debug("Incoming message to web service provider:");
            }
        }
		SOAPMessage message = smc.getMessage();
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			message.writeTo(baos);
			baos.flush();
			log.info(baos.toString());
			baos.close();
		} catch (Exception e) {
			log.error("Exception in handler: " + e);
		}

	}
}
