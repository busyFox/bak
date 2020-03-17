package com.gotogames.bridge.engineserver.ws;


/**
 * Interceptor that allows to check if the header contains a sessionID
 * Check the sessionID and throw an exception if the sessionID is not valid
 * 
 * @author pascal
 *
 */
public class SessionInterceptor /*extends AbstractSoapInterceptor*/{

//	private Log log = LogFactory.getLog(SessionInterceptor.class);
//	private static final String SESSION_LOCAL_PART = "sessionID";
//	
//	public SessionInterceptor() {
//		super(Phase.READ); // IMPORTANT SETTINGS !!
//		addAfter(ReadHeadersInterceptor.class.getName()); // IMPORTANT SETTINGS : the header are loaded by this interceptor
//	}
//	
//	@Override
//	public void handleMessage(SoapMessage message) throws Fault {
//		boolean isNodeSessionFound = false;
//		List<Header> headers = message.getHeaders();
//		if (headers != null) {
//			
//			// loop on header to find session
//			for (Iterator<Header> iterator = headers.iterator(); iterator.hasNext();) {
//				Header header = iterator.next();
//				if (header.getName().getLocalPart().equals(SESSION_LOCAL_PART)) {
//					String sessionID = "";
//					isNodeSessionFound = true;
//					// read value on node
//					if (header.getObject() instanceof ElementNSImpl) {
//						Node nodeValue = ((ElementNSImpl) header.getObject()).getFirstChild();
//						if (nodeValue != null) {
//							
//							try {
//								sessionID = nodeValue.getNodeValue();
//								// check sessionID
//								ContextManager.getSessionMgr().checkSession(sessionID);
//								ContextManager.getSessionMgr().touchSession(sessionID);
//								// session valid => exit loop
//								break;
//							} catch (ServiceException e) {
//								log.info("handleMessage - checkSession : "+e.getMessage());
//								throw new Fault(e);
//							} catch (DOMException e) {
//								log.error("handleMessage - DOMException on nodeValue.getNodeValue()", e);
//								throw new Fault(new ServiceException(ServiceExceptionType.SESSION_MESSAGE_NOT_VALID, "DOM Exception : node value too long"));
//							}
//						} else {
//							// node not found !
//							log.error("handleMessage - getFirstChild return null value");
//							throw new Fault(new ServiceException(ServiceExceptionType.SESSION_MESSAGE_NOT_VALID, "No value found for node session"));
//						}
//					} else {
//						// header not valid !
//						log.error("handleMessage - header not an ElementNSImpl !");
//						throw new Fault(new ServiceException(ServiceExceptionType.SESSION_MESSAGE_NOT_VALID, ""));
//					}
//				} // end test header name
//			} // end loop			
//		}
//		
//		if (!isNodeSessionFound) {
//			// node session not found => throw exception
//			log.error("handleMessage - No header session found !");
//			//throw new Fault(new FBWSExc
//			throw new Fault(new ServiceException(ServiceExceptionType.SESSION_MESSAGE_NOT_VALID, "Header sessionID missing"));
//		}
//	}

}
