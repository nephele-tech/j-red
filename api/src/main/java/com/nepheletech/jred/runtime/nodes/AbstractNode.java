/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED API project.
 *
 * J-RED API is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED API is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED API; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.runtime.nodes;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
import com.nepheletech.jred.runtime.events.NodesStoppedEvent;
import com.nepheletech.jred.runtime.events.NodesStoppedEventListener;
//import com.nepheletech.jred.runtime.events.NodesStartedEvent;
//import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
//import com.nepheletech.jred.runtime.events.NodesStoppedEvent;
//import com.nepheletech.jred.runtime.events.NodesStoppedEventListener;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;
import com.nepheletech.messagebus.Subscription;

public abstract class AbstractNode extends RouteBuilder implements Node {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private static final String JRED_MSG = "__jred_msg_";

  // ---

  private final String id;
  private final String type;
  private final String z;

  private final String topicPrefix;
  private boolean hasSubscribers;
  // TODO _closeCallbacks

  private final String name;
  private final String _alias;

  private JtonArray wires;
  private String _wire;
  private int _wireCount;

  protected final Flow flow;

  protected final CamelContext camelContext;
  protected final ProducerTemplate template;

  // Event listeners...
  // private final Subscription subscription;
  private final Subscription nodesStartedSubscription;
  private final Subscription nodesStoppedSubscription;

  public AbstractNode(final Flow flow, final JtonObject config) {
    this.flow = flow;
    this.camelContext = flow.getCamelContext();
    this.template = camelContext.createProducerTemplate();

    this.id = config.getAsString("id");
    this.type = config.getAsString("type");
    this.z = config.getAsString("z", null);
    this.name = config.getAsString("name", null);
    this._alias = config.getAsString("_alias", null);

    this.topicPrefix = this.type.concat(this.id);

    flow.setup(this);

    updateWires(config.getAsJtonArray("wires", true));

    if (this instanceof NodesStartedEventListener) {
      nodesStartedSubscription = MessageBus
          .subscribe(NodesStartedEvent.class, new MessageBusListener<NodesStartedEvent>() {
            @Override
            public void messageSent(String topic, NodesStartedEvent message) {
              try {
                ((NodesStartedEventListener) AbstractNode.this).onNodesStarted(message);
              } catch (Exception e) {
                logger.error("Unhandled exception", e);
                logger.debug(e.getMessage(), e);
              }
            }
          });
    } else {
      nodesStartedSubscription = null;
    }

    if (this instanceof NodesStoppedEventListener) {
      nodesStoppedSubscription = MessageBus
          .subscribe(NodesStoppedEvent.class, new MessageBusListener<NodesStoppedEvent>() {
            @Override
            public void messageSent(String topic, NodesStoppedEvent message) {
              try {
                ((NodesStoppedEventListener) AbstractNode.this).onNodesStopped(message);
              } catch (Exception e) {
                logger.error("Unhandled exception", e);
                logger.debug(e.getMessage(), e);
              }
            }
          });
    } else {
      nodesStoppedSubscription = null;
    }
  }

  @Override
  public void configure() throws Exception {
    logger.trace(">>> configure: {}", getId());

    errorHandler(deadLetterChannel("direct:" + getId() + "#deadLetterQueue")
        .useOriginalMessage());

    from("direct:" + getId() + "#deadLetterQueue")
        .log(LoggingLevel.ERROR, logger, "Runtime exception")
        .process((x) -> {
          final JtonObject msg = getMsg(x);

          final Exception e = x.getProperty(Exchange.EXCEPTION_CAUGHT,
              RuntimeException.class);

          try {
            AbstractNode.this.error(e, msg);
          } catch (Exception ex) {
            ex.printStackTrace();
          }
        });

    final String additionalFlow = getAdditionalRoute();
    logger.debug("additionalFlow={}", additionalFlow);

    if (additionalFlow != null) {
      fromF("direct:%s", getId()).routeId(getType() + ":" + getId())
          .toF("log:%s?level=TRACE&showAll=true", logger.getName())
          .process(this::onMessage)
          .to(getAdditionalRoute());
    } else {
      fromF("direct:%s", getId()).routeId(getType() + ":" + getId())
          .toF("log:%s?level=TRACE&showAll=true", logger.getName())
          .process(this::onMessage);
    }
  }

  protected String getAdditionalRoute() {
    return null;
  }

  protected final void onMessage(Exchange exchange) {
    logger.trace(">>> onMessage: exchange={} ({})", exchange,
        Thread.currentThread().getId());

    /*
     * logger.trace("properties={}", exchange != null ? exchange.getAllProperties()
     * : null, getId());
     */
    onMessage(exchange, getMsg(exchange));
  }

  protected abstract void onMessage(Exchange exchange, JtonObject msg);

  @Override
  public Flow getFlow() {
    return flow;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getZ() {
    return z;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getAlias() {
    return _alias;
  }

  @Override
  public String getAliasOrIdIfNull() {
    return _alias != null ? _alias : id;
  }

  @Override
  public void updateWires(JtonArray wires) {
    logger.debug("UPDATE {}", getId());

    this.wires = wires;
    this._wire = null;

    int wc = 0;
    for (final JtonElement w : this.wires) {
      wc += w.asJtonArray().size();
    }

    this._wireCount = wc;
    if (wc == 0) {
      // With nothing wired to the node, no-op send
    } else {
      if (this.wires.size() == 1 && this.wires.get(0).asJtonArray().size() == 1) {
        // Single wire, so we can shortcut the send when a single message is sent
        this._wire = this.wires.getAsJtonArray(0).getAsString(0);
      }
    }
  }

  @Override
  public final void close(boolean removed) {
    logger.trace(">>> close: type={}, id={}, removed={}", type, id, removed);

    if (nodesStartedSubscription != null) {
      nodesStartedSubscription.unsubscribe();
    }

    if (nodesStoppedSubscription != null) {
      nodesStoppedSubscription.unsubscribe();

      if (this instanceof NodesStoppedEventListener) {
        try {
          ((NodesStoppedEventListener) AbstractNode.this).onNodesStopped(new NodesStoppedEvent(this));
        } catch (Exception e) {
          logger.error("Unhandled exception", e);
          logger.debug(e.getMessage(), e);
        }
      }
    }

    if (this.hasSubscribers) {
      MessageBus.sendMessage(topicPrefix.concat("#closed"), removed);
    }

    if (template != null) {
      try {
        template.stop();
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    try {
      for (RouteDefinition d : this.getRouteCollection().getRoutes()) {
        logger.debug("Removing route: {}", d);

        var routeId = d.getId();

        var rc = flow.getCamelContext().getRouteController();
        rc.stopRoute(routeId);

        flow.getCamelContext().removeRoute(routeId);
      }
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    onClosed(removed);
  }

  protected void onClosed(boolean removed) {
    // do nothing
  }

  @Override
  public <T> Subscription on(String event, MessageBusListener<T> messageListener) {
    if (!this.hasSubscribers) {
      this.hasSubscribers = true;
    }

    return MessageBus.subscribe(this.topicPrefix.concat(event), messageListener);
  }

  protected final void send(Exchange exchange, JtonElement _msg) {
    logger.trace(">>> send: exchange={}", exchange);

    // logger.debug("--- send: _msg={}", _msg);
    // logger.debug("--- send: wires={}", wires);

    if (this.hasSubscribers) {
      MessageBus.sendMessage(topicPrefix.concat("#send"), _msg);
    }

    if (this._wireCount == 0) {
      // With nothing wired to the node, no-op send
      return;
    }

    if (_msg == null
        || !(_msg.isJtonObject() || _msg.isJtonArray())) {
      return;
    } else if (!_msg.isJtonArray()) {
      if (this._wire != null) {
        //@formatter:off
        // A single message and a single wire on output 0
        // TODO: pre-load flows.get calls - cannot do in constructor as not all nodes
        //       are defined at that point
        //@formatter:on
        final JtonObject msg = _msg.asJtonObject();
//        if (!msg.has("_msgid")) {
//          msg.set("_msgid", UUID.randomUUID().toString());
//        }
        // this.metric("send", msg)
        send(exchange, msg, this._wire);
        return;
      } else {
        _msg = new JtonArray().push(_msg);
      }
    }

    // Note: msg is an array of arrays
    final JtonArray msg = _msg.asJtonArray();

    final int numOutputs = this.wires.size();

    // Build a list of send events so that all cloning is done
    // before any calls to node.receive
    final List<SendEvent> sendEvents = new ArrayList<>();

//    String sentMessageId = null;

    // for each output of node eg. [msgs to output 0, msgs to output 1, ...]
    for (int i = 0, imax = numOutputs; i < imax; i++) {
      final JtonArray wires = this.wires.get(i).asJtonArray(); // wires leaving output 1
      if (i < msg.asJtonArray().size()) {
        JtonElement msgs = msg.asJtonArray().get(i); // msgs going to output i
        if (!msgs.isJtonNull() && !msgs.isJtonPrimitive()) {
          if (!msgs.isJtonArray()) {
            msgs = new JtonArray().push(msgs);
          }
          int k = 0, kmax;
          // for each recipient node of that output
          for (int j = 0, jmax = wires.size(); j < jmax; j++) {
            final String nodeId = wires.get(j).asString();
            // for each msg to send eg. [[m1, m2, ...], ...]
            for (k = 0, kmax = msgs.asJtonArray().size(); k < kmax; k++) {
              final JtonElement m = msgs.asJtonArray().get(k);
              if (m.isJtonObject()) {
                if (sendEvents.size() > 0) {
                  final JtonElement clonedMsg = m.deepCopy();
                  sendEvents.add(new SendEvent(nodeId, 
                      clonedMsg.asJtonObject(), exchange.copy()));
                } else {
                  sendEvents.add(new SendEvent(nodeId, 
                      m.asJtonObject(), exchange));
                }
              }
            }
          }
        }
      }
    }

    // TODO: this.metric

    for (SendEvent ev : sendEvents) {
      send(ev.x, ev.m, ev.n);
    }
  }

  private final class SendEvent {
    final String n;
    final JtonObject m;
    final Exchange x;

    public SendEvent(String n, JtonObject m, Exchange x) {
      this.n = n;
      this.m = m;
      this.x = x;
    }
  }

  private void send(Exchange exchange, JtonObject msg, String nodeId) {
    logger.trace(">>> send: {} -> {}", getId(), nodeId);

    if (exchange.isTransacted()) {
      template.send("direct:" + nodeId,
          setMsg(exchange, ensureMsg(exchange, msg)));
    } else {
      template.asyncSend("direct:" + nodeId,
          setMsg(exchange, ensureMsg(exchange, msg)));
    }
  }

  @Override
  public final void receive(JtonObject msg) {
    logger.trace(">>> receive: msg={}", msg);

    template.send(format("direct:%s", getId()),
        (x) -> setMsg(x, ensureMsg(x, msg)));
  }

  protected static final JtonObject getMsg(Exchange exchange) {
    return exchange.getProperty(JRED_MSG, JtonObject.class);
  }

  protected static final Exchange setMsg(Exchange exchange, JtonObject msg) {
    exchange.setProperty(JRED_MSG, msg);
    return exchange;
  }

  protected static JtonObject ensureMsg(Exchange exchange, JtonObject msg) {
    if (msg == null) {
      msg = new JtonObject();
    }

    if (!msg.has("_msgid")) {
      msg.set("_msgid", exchange.getIn().getMessageId());
    }

    return msg;
  }

  protected void metric() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void status(JtonObject status) {
    logger.trace(">>> status: {}", status);

    flow.handleStatus(this, status != null ? status : new JtonObject(), null, false);
  }

  @Override
  public void status(String text) {
    status(new JtonObject().set("text", text));
  }

  protected static final int OFF = 1;
  protected static final int FATAL = 10;
  protected static final int ERROR = 20;
  protected static final int WARN = 30;
  protected static final int INFO = 40;
  protected static final int DEBUG = 50;
  protected static final int TRACE = 60;
  protected static final int AUDIT = 98;
  protected static final int METRIC = 99;

  protected void log(JtonObject msg) {
    log_helper(INFO, msg);
  }

  /**
   * Log processing error.
   * 
   * @param t
   * @param msg
   */
  protected void error(Throwable t, JtonObject msg) {
    if (t == null) {
      throw new IllegalArgumentException("Throwable is null");
    }
    if (msg == null) {
      throw new IllegalArgumentException("Message is null");
    }

    if (t != null) {
      error0(t, msg.deepCopy());
    }
  }

  /**
   * Log processing error.
   * 
   * @param logMessage
   * @param msg
   */
  private void error0(Throwable logMessage, JtonObject msg) {
    boolean handled = false;

    if (msg != null) {
      handled = flow.handleError(this, logMessage, msg, null);
    }

    if (!handled) {
      Throwable rootCause = ExceptionUtils.getRootCause(logMessage);
      if (rootCause == null) {
        rootCause = logMessage;
      }

      log_helper(ERROR, new JtonObject()
          .set("message", rootCause.toString())
          .set("stack", JRedUtil.stackTrace(rootCause))
          .set("msg", msg));
    }
  }

  protected void log_helper(int level, Object msg) {
    final JtonObject o = new JtonObject()
        .set("id", getAlias() != null ? getAlias() : getId())
        .set("type", getType())
        .set("msg", msg, false);

    final String _alias = getAlias();
    if (_alias != null) {
      o.set("_alias", _alias);
    }

    final String z = getZ();
    if (z != null) {
      o.set("z", z);
    }

    final String name = getName();
    if (name != null) {
      o.set("name", name);
    }

    switch (level) {
    case FATAL:
      logger.error(Objects.toString(msg));
      break;
    case ERROR:
      logger.error(Objects.toString(msg));
      break;
    case WARN:
      logger.warn(Objects.toString(msg));
      break;
    case INFO:
      logger.info(Objects.toString(msg));
      break;
    case DEBUG:
      logger.debug(Objects.toString(msg));
      break;
    case TRACE:
      logger.trace(Objects.toString(msg));
      break;
    case AUDIT:
      logger.trace(Objects.toString(msg));
      break;
    case METRIC:
      logger.trace(Objects.toString(msg));
      break;
    }

    if (level > OFF) {
      o.set("level", level);
    }

    JRedUtil.publish("debug", "debug", o);
  }
}
