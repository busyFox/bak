package com.funbridge.server.ws.servlet;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ChatWebSocketServlet extends WebSocketServlet {
	private static List<ChatMsgInbound> listChat = new ArrayList<ChatWebSocketServlet.ChatMsgInbound>();
	SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
	protected Logger log = LogManager.getLogger(this.getClass());
	
	private class ChatMsgInbound extends MessageInbound {
		public ChatMsgInbound() {
		}
		
		@Override
		protected void onOpen(WsOutbound outbound) {
			System.out.println("onOpen");
			listChat.add(this);
			try {
				outbound.writeTextMessage(CharBuffer.wrap("Hello, welcome on server. Current date is : "+sdf.format(new Date(System.currentTimeMillis()))));
			} catch (IOException e) {
				log.error("IOException !", e);
			}
		}
		
		@Override
		protected void onClose(int status) {
            if (log.isDebugEnabled()) {
                log.debug("onClose : status=" + status);
            }
			listChat.remove(this);
		}
		
		@Override
		protected void onBinaryMessage(ByteBuffer message) throws IOException {
			log.error("onBinaryMessage : not supported !");
			throw new UnsupportedOperationException("Binary message not supported");
		}

		@Override
		protected void onTextMessage(CharBuffer message) throws IOException {
			String msg = message.toString();
            if (log.isDebugEnabled()) {
                log.debug("Message received : " + msg);
            }
			String txtToReply = "Message received : "+msg+" - at "+sdf.format(new Date(System.currentTimeMillis()));
            if (log.isDebugEnabled()) {
                log.debug("Message reply : " + txtToReply);
            }
			this.getWsOutbound().writeTextMessage(CharBuffer.wrap(txtToReply));
		}

	}
	
	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol, HttpServletRequest request) {
		return new ChatMsgInbound();
	}
}
