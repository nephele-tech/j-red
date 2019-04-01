package com.nepheletech.jred.editor.api.websocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.websocket.EndpointConfig;
import javax.websocket.HandshakeResponse;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParser;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;


@ServerEndpoint(value = "/comms", configurator = CommsEndpoint.CommsEndpointConfigurator.class)
public class CommsEndpoint implements MessageBusListener<JsonObject> {
  private static final Logger logger = LoggerFactory.getLogger(CommsEndpoint.class);

  public static class CommsEndpointConfigurator extends ServerEndpointConfig.Configurator {

    static final String HANDSHAKE_REQUEST_KEY = "handshake-req";

    @Override
    public void modifyHandshake(ServerEndpointConfig conf, HandshakeRequest req, HandshakeResponse resp) {
      super.modifyHandshake(conf, req, resp);
      conf.getUserProperties().put(HANDSHAKE_REQUEST_KEY, req);
    }
  }

  // ---

  private final Map<String, String> subscriptions = new HashMap<>();

  private Session session;

  private final JsonArray stack = new JsonArray();

  @OnOpen
  public void onOpen(Session session, EndpointConfig config) {
    logger.trace(">>> onOpen: id={}", session.getId());

    this.session = session;
  }

  @OnClose
  public void onClose() {
    logger.trace(">>> onClose: id={}", session.getId());

    synchronized (subscriptions) {
      for (Iterator<String> i = subscriptions.keySet().iterator(); i.hasNext();) {
        try {
          MessageBus.unsubscribe(i.next(), this);
        } catch (Exception e) {
          // ignore
        } finally {
          i.remove();
        }
      }
    }
  }

  @OnMessage
  public void onMessage(String message) {
    logger.trace("onMessage: message={}", message);

    try {
      JsonElement jsonMsg = JsonParser.parse(message);
      if (jsonMsg.isJsonObject()) {
        JsonObject msg = jsonMsg.asJsonObject();
        if (msg.has("subscribe")) {
          // if (null != accountInfo) {
          String topic = msg.get("subscribe").asString(null);
          // String project = msg.get("project").getAsString(null);
          // if (null != topic && null != project) {
          StringBuilder sb = new StringBuilder();
          sb.append(topic);
          // .append(";")
          // .append(project)
          // .append(";")
          // .append(accountInfo.getAccount());
          String _topic = sb.toString();

          //LOG.d("Subscribe to: %s", _topic);

          synchronized (subscriptions) {
            if (!subscriptions.containsKey(_topic)) {
              subscriptions.put(_topic, topic);
              MessageBus.subscribe(_topic, this);

              if (topic.startsWith("status")) {
                // CommsEndpointEvent evt = new CommsEndpointEvent(this, "status", project,
                // accountInfo.getAccount());
                // MessageBus.sendMessage(evt);
              }

              return;
            }
          }

          // }
          // }

        } else {
          throw new UnsupportedOperationException(message);
        }
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  @Override
  public void messageSent(String topic, JsonObject message) {
    logger.trace(">>> messageSent: topic={}, message={}", topic, message );

    if (topic != null && message != null) {
      this.stack.push(message);
    }

    sendMessages();
  }

  private synchronized void sendMessages() {
    if (null != session && session.isOpen()) {
//  if (this.ok2tx && this.stack.size() > 0) {
//  this.ok2tx = false;
      
      try {
      session.getBasicRemote().sendText(this.stack.toString());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      //e.printStackTrace();
      //LOG.d (e.getMessage());
    }
/*      session.getAsyncRemote().sendText(this.stack.toString(), (r) -> {
        if (!r.isOK()) {
          LOG.w(r.getException(), "setText failed");
        }
      });
*/
      this.stack.clear();
      ;
//  this.ok2tx = true;
//}
    } else {
      //LOG.w("WebSocket session is null or closed; message queued...");
    }
  }

  @OnError
  public void onError(Session session, Throwable thr) {
    thr.printStackTrace();
  }
}
