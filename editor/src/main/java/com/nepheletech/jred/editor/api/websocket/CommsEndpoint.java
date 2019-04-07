package com.nepheletech.jred.editor.api.websocket;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

import com.nepheletech.jred.runtime.util.JRedUtil;
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

  private static final MessageBusListener<JsonObject> handleStatusEvent = new MessageBusListener<JsonObject>() {
    @Override
    public void messageSent(String topic, JsonObject message) {
      logger.trace(">>> handleStatusEvent: topic={}, message={}", topic, message);
      
      final String id = message.getAsString("id", null);
      if (id != null) {
        final JsonObject status = message.getAsJsonObject("status", false);
        if (status != null) {
          JRedUtil.publish("status/#", "status/" + id, status, true);
        }
      }
    }
  };

  // ---

  // ConcurrentHashSet derived from ConcurrentHashMap (Java 8 or newer)
  private final Set<String> subscriptions = ConcurrentHashMap.newKeySet();

  private Session session;

  private final JsonArray stack = new JsonArray();

  @OnOpen
  public void onOpen(Session session, EndpointConfig config) {
    logger.trace(">>> onOpen: id={}", session.getId());

    this.session = session;
    this.session.setMaxIdleTimeout(-1L);
    // this.session.setMaxTextMessageBufferSize(8192);

    on("node-status", handleStatusEvent);
  }

  private void on(String topic, MessageBusListener<JsonObject> messageListener) {
    try {
      MessageBus.unsubscribe(topic, messageListener);
    } catch (IllegalArgumentException e) {
      // ignore
    } finally {
      MessageBus.subscribe(topic, messageListener);
    }
  }

  @OnClose
  public void onClose() {
    logger.trace(">>> onClose: id={}", session.getId());

    for (Iterator<String> i = subscriptions.iterator(); i.hasNext();) {
      try {
        MessageBus.unsubscribe(i.next(), this);
      } catch (Exception e) {
        // ignore
      } finally {
        i.remove();
      }
    }
  }

  @OnMessage
  public void onMessage(String message) {
    logger.trace(">>> onMessage: message={}", message);

    try {
      JsonElement jsonMsg = JsonParser.parse(message);
      if (jsonMsg.isJsonObject()) {
        JsonObject msg = jsonMsg.asJsonObject();
        if (msg.has("subscribe")) {
          String topic = msg.getAsString("subscribe", null);

          logger.debug("Subscribe to: {}", topic);

          if (!subscriptions.contains(topic)) {
            subscriptions.add(topic);

            MessageBus.subscribe(topic, this);

            return;
          }

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
    logger.trace(">>> messageSent: topic={}, message={}", topic, message);

    if (message != null) {
      synchronized (stack) {
        stack.push(message); // queue messages

        if (null != session && session.isOpen()) {
          try {
            session.getBasicRemote().sendText(stack.toString());
            stack.clear();
          } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        } else {
          logger.warn("WebSocket session is null or closed; message queued...");
        }
      }
    }
  }

  @OnError
  public void onError(Session session, Throwable thr) {
    thr.printStackTrace();
  }
}
