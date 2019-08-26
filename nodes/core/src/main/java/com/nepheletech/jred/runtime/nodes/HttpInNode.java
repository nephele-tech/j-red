/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Nodes project.
 *
 * J-RED Nodes is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Nodes is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Nodes; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;

import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonUtil;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;

public class HttpInNode extends AbstractNode implements NodesStartedEventListener {

  public static String createListenerTopic(String method, String url) {
    return HttpInNode.class.getName() + ':' + method + ':' + url;
  }

  // ---

  private final String url;
  private final String method;
  private final boolean upload;

  private final MessageBusListener<JtonObject> requestListener = (topic, msg) -> receive(msg);
  private final String requestListenerTopic;

  public HttpInNode(Flow flow, JtonObject config) {
    super(flow, config);

    final String url = config.get("url").asString(null);
    if (url == null) { throw new RuntimeException("missing path"); }
    this.url = (url.charAt(0) != '/') ? '/' + url : url;
    this.method = config.get("method").asString("GET").toUpperCase();
    this.upload = config.get("upload").asBoolean();
    // TODO swagger

    this.requestListenerTopic = createListenerTopic(this.method, this.url);
  }

  @Override
  public void onNodesStarted(NodesStartedEvent event) {
    logger.trace(">>> ----------------------------- onNodesStarted: event={}", event);

    logger.debug("subscribe to: {}", requestListenerTopic);
    MessageBus.subscribe(requestListenerTopic, requestListener);
  }

  @Override
  protected void onClosed(boolean removed) {
    logger.trace(">>> onClosed");

    MessageBus.subscribe(requestListenerTopic, requestListener);
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    // TODO uploads...

    logger.info("-------------------------{}", JtonUtil.getProperty(msg, "req._pathInfo"));

    send(msg);
  }
}
