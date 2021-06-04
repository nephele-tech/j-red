/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED project.
 *
 * J-RED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * J-RED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with J-RED.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.nepheletech.jred.editor.api.websocket;

import java.io.IOException;
import java.util.Map;
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
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonParser;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;
import com.nepheletech.messagebus.Subscription;

@ServerEndpoint(value = "/comms", configurator = CommsEndpoint.CommsEndpointConfigurator.class)
public class CommsEndpoint implements MessageBusListener<JtonObject> {
  private static final Logger logger = LoggerFactory.getLogger(CommsEndpoint.class);

  public static class CommsEndpointConfigurator extends ServerEndpointConfig.Configurator {

    static final String HANDSHAKE_REQUEST_KEY = "handshake-req";

    @Override
    public void modifyHandshake(ServerEndpointConfig conf, HandshakeRequest req, HandshakeResponse resp) {
      super.modifyHandshake(conf, req, resp);
      conf.getUserProperties().put(HANDSHAKE_REQUEST_KEY, req);
    }
  }

  private static final MessageBusListener<JtonObject> handleStatusEvent = new MessageBusListener<JtonObject>() {
    @Override
    public void messageSent(String topic, JtonObject message) {
      logger.trace(">>> handleStatusEvent: topic={}, message={}", topic, message);

      final String id = message.getAsString("id", null);
      if (id != null) {
        final JtonObject status = message.getAsJtonObject("status", false);
        if (status != null) {
          JRedUtil.publish("status/#", "status/" + id, status, true);
        }
      }
    }
  };

  // ---

  private Map<String, Subscription> mbSubscriptions = new ConcurrentHashMap<>();

  private Session session;

  private final JtonArray stack = new JtonArray();

  @OnOpen
  public void onOpen(Session session, EndpointConfig config) {
    logger.trace(">>> onOpen: id={}", session.getId());

    this.session = session;
    this.session.setMaxIdleTimeout(0L);
    // this.session.setMaxTextMessageBufferSize(8192);

    on("node-status", handleStatusEvent);
  }

  private Subscription mbSubscription = null;

  private void on(String topic, MessageBusListener<JtonObject> messageListener) {
    if (mbSubscription != null) {
      mbSubscription.unsubscribe();
      mbSubscription = null;
    }

    mbSubscription = MessageBus.subscribe(topic, messageListener);
  }

  @OnClose
  public void onClose() {
    logger.trace(">>> onClose: id={}", session.getId());

    try {
      mbSubscriptions.values().forEach(Subscription::unsubscribe);
    } finally {
      mbSubscriptions.clear();
    }
  }

  @OnMessage
  public void onMessage(String message) {
    logger.trace(">>> onMessage: message={}", message);

    try {
      JtonElement jsonMsg = JtonParser.parse(message);
      if (jsonMsg.isJtonObject()) {
        JtonObject msg = jsonMsg.asJtonObject();
        if (msg.has("subscribe")) {
          String topic = msg.getAsString("subscribe", null);

          logger.debug("Subscribe to: {}", topic);

          if (!mbSubscriptions.containsKey(topic)) {
            mbSubscriptions.put(topic, MessageBus.subscribe(topic, this));
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
  public void messageSent(String topic, JtonObject message) {
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
