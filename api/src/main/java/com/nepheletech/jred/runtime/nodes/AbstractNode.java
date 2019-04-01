package com.nepheletech.jred.runtime.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.events.NodesStartedEvent;
import com.nepheletech.jred.runtime.events.NodesStartedEventListener;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;

public abstract class AbstractNode implements Node {
  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final String id;
  private final String type;
  private final String z;
  // TODO _closeCallbacks

  private final String name;
  private final String _alias;

  private JsonArray wires;
  private String _wire;
  private int _wireCount;

  protected final Flow flow;

  // Event listeners...
  private MessageBusListener<NodesStartedEvent> nodesStartedEventListener = null;

  public AbstractNode(final Flow flow, final JsonObject config) {
    this.flow = flow;
    this.id = config.get("id").asString();
    this.type = config.get("type").asString();
    this.z = config.get("z").asString();
    this.name = config.get("name").asString(null);
    this._alias = config.get("_alias").asString(null);

    updateWires(config.getAsJsonArray("wires", true));

    if (this instanceof NodesStartedEventListener) {
      nodesStartedEventListener = new MessageBusListener<NodesStartedEvent>() {
        @Override
        public void messageSent(String topic, NodesStartedEvent message) {
          try {
            ((NodesStartedEventListener) AbstractNode.this).onNodesStarted(message);
          } catch (Exception e) {
            logger.error("Unhandled exception", e);
            logger.debug(e.getMessage(), e);
          } finally {
            MessageBus.unsubscribe(NodesStartedEvent.class, nodesStartedEventListener);
            nodesStartedEventListener = null;
          }
        }
      };

      MessageBus.subscribe(NodesStartedEvent.class, nodesStartedEventListener);
    }
  }
  
  @Override
  public Flow getFlow() { return flow; }

  @Override
  public JsonObject getContext(String type) {
    return getFlow().getContext(type);
  }

  @Override
  public String getId() { return id; }

  @Override
  public String getType() { return type; }

  @Override
  public String getZ() { return z; }

  @Override
  public String getName() { return name; }

  @Override
  public String getAlias() { return _alias; }

  @Override
  public void updateWires(JsonArray wires) {
    logger.debug("UPDATE {}", this.id);

    this.wires = wires;
    this._wire = null;

    int wc = 0;
    for (final JsonElement w : this.wires) {
      wc += w.asJsonArray().size();
    }

    this._wireCount = wc;
    if (wc == 0) {
      // With nothing wired to the node, no-op send
    } else {
      if (this.wires.size() == 1 && this.wires.get(0).asJsonArray().size() == 1) {
        // Single wire, so we can shortcut the send when a single message is sent
        this._wire = this.wires.getAsJsonArray(0).getAsString(0);
      }
    }
  }

  @Override
  public void close() {
    logger.debug("type:{}, id:{}, removed", type, id);

    if (nodesStartedEventListener != null) {
      try {
        MessageBus.unsubscribe(NodesStartedEvent.class, nodesStartedEventListener);
      } catch (IllegalArgumentException e) {
        // ignore...
      } finally {
        nodesStartedEventListener = null;
      }
    }
  }

  @Override
  public final void send(JsonElement _msg) {
    logger.trace(">>> send: _msg={}", _msg);
    logger.trace(">>> send: wires={}", wires);

    if (this._wireCount == 0) {
      // With nothing wired to the node, no-op send
      return;
    }

    Node node = null;

    if (_msg == null
        || !(_msg.isJsonObject() || _msg.isJsonArray())) {
      return;
    } else if (!_msg.isJsonArray()) {
      if (this._wire != null) {
        //@formatter:off
        // A single message and a single wire on output 0
        // TODO: pre-load flows.get calls - cannot do in constructor as not all nodes
        //       are defined at that point
        //@formatter:on
        final JsonObject msg = _msg.asJsonObject();
        if (!msg.has("_msgid")) {
          msg.set("_msgid", UUID.randomUUID().toString());
        }
        // this.metric("send", msg)
        node = flow.getNode(this._wire);
        if (node != null) {
          node.receive(msg);
          return;
        }
      } else {
        _msg = new JsonArray().push(_msg);
      }
    }

    // Note: msg is an array of arrays
    final JsonArray msg = _msg.asJsonArray();

    final int numOutputs = this.wires.size();

    // Build a list of send events so that all cloning is done before any calls to
    // node.receive
    final List<SendEvent> sendEvents = new ArrayList<>();

    String sentMessageId = null;

    // for each output of node eg. [msgs to output 0, msgs to output 1, ...]
    boolean msgSent = false;
    for (int i = 0, imax = numOutputs; i < imax; i++) {
      final JsonArray wires = this.wires.get(i).asJsonArray(); // wires leaving output 1
      if (i < msg.asJsonArray().size()) {
        JsonElement msgs = msg.asJsonArray().get(i); // msgs going to output i
        if (!msgs.isJsonNull() && !msgs.isJsonPrimitive()) {
          if (!msgs.isJsonArray()) {
            msgs = new JsonArray().push(msgs);
          }
          int k = 0, kmax;
          // for each recipient node of that output
          for (int j = 0, jmax = wires.size(); j < jmax; j++) {
            node = flow.getNode(wires.get(j).asString()); // node at end of wire
            if (node != null) {
              // for each msg to send eg. [[m1, m2, ...], ...]
              for (k = 0, kmax = msgs.asJsonArray().size(); k < kmax; k++) {
                final JsonElement m = msgs.asJsonArray().get(k);
                if (m.isJsonObject()) {
                  if (sentMessageId == null) {
                    sentMessageId = m.asJsonObject().get("_msgid").asString();
                  }
                  if (msgSent) {
                    final JsonElement clonedMsg = m.deepCopy();
                    sendEvents.add(new SendEvent(node, clonedMsg.asJsonObject()));
                  } else {
                    sendEvents.add(new SendEvent(node, m.asJsonObject()));
                    msgSent = false;
                  }
                }
              }
            }
          }
        }
      }
    }

    if (sentMessageId == null) {
      sentMessageId = UUID.randomUUID().toString();
    }
    // this.metric

    for (SendEvent ev : sendEvents) {
      if (!ev.m.has("_msgid")) {
        ev.m.set("_msgid", sentMessageId);
      }
      ev.n.receive(ev.m);
    }
  }

  private final class SendEvent {
    final Node n;
    final JsonObject m;

    public SendEvent(final Node n, final JsonObject m) {
      this.n = n;
      this.m = m;
    }
  }

  @Override
  public final void receive(JsonObject msg) {
    if (msg == null) {
      msg = new JsonObject();
    }
    if (!msg.has("_msgid")) {
      msg.set("_msgid", UUID.randomUUID().toString());
    }
    // this.metric("receive",msg);
    try {
      onMessage(msg);
    } catch (RuntimeException e) {
      e.printStackTrace();
      error(e, msg);
    }
  }

  protected abstract void onMessage(JsonObject msg);
  
  private void log_helper(int level, JsonObject msg) {
    logger.trace(">>> log_helper: level={}, msg={}", level, msg);
    
    final JsonObject o = new JsonObject()
        .set("id", getId()) // id or alias ?
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
      LoggerFactory.getLogger(getClass()).error(o.toString());
      break;
    case ERROR:
      LoggerFactory.getLogger(getClass()).error(o.toString());
      break;
    case WARN:
      LoggerFactory.getLogger(getClass()).warn(o.toString());
      break;
    case INFO:
      LoggerFactory.getLogger(getClass()).info(o.toString());
      break;
    case DEBUG:
      LoggerFactory.getLogger(getClass()).debug(o.toString());
      break;
    case TRACE:
      LoggerFactory.getLogger(getClass()).trace(o.toString());
      break;
    case AUDIT:
      LoggerFactory.getLogger(getClass()).trace(o.toString());
      break;
    case METRIC:
      LoggerFactory.getLogger(getClass()).trace(o.toString());
      break;
    }

    if (level > OFF) {
      o.set("level", level);
    }

    publish("debug", o);
  }

  protected void log(JsonObject msg) {
    log_helper(INFO, msg);
  }

  protected void warn(JsonObject msg) {
    log_helper(WARN, msg);
  }

  /**
   * Log processing error.
   * 
   * @param t
   * @param msg
   */
  protected void error(Throwable t, JsonObject msg) {
    if (t == null) {
      throw new IllegalArgumentException("Throwable is null");
    }
    if (msg == null) {
      throw new IllegalArgumentException("Message is null");
    }
    
    if (t != null) {
      Throwable rootCause = ExceptionUtils.getRootCause(t);
      if (rootCause == null) {
        rootCause = t;
      }

      final StringBuilder sb = new StringBuilder()
          .append(rootCause.getClass())
          .append(": ")
          .append(rootCause.getMessage());

      error0(sb.toString(), msg.deepCopy()
          .set("error", new JsonObject()
              .set("message", sb.toString())
              .set("stackTrace", stackTrace(t))));
    }
  }

  // TODO should be moved to a JSON helper class.
  private static JsonArray stackTrace(Throwable t) {
    final JsonArray stackTrace = new JsonArray();
    StackTraceElement elements[] = t.getStackTrace();
    for (StackTraceElement e : elements) {
      stackTrace.push(new JsonObject()
          .set("className", e.getClassName())
          .set("methodName", e.getMethodName())
          .set("fileName", e.getFileName())
          .set("lineNumber", e.getLineNumber()));
    }
    return stackTrace;
  }

  /**
   * Log processing error.
   * 
   * @param logMessage
   * @param msg
   */
  protected void error(String logMessage, JsonObject msg) {
    if (logMessage == null) {
      throw new IllegalArgumentException("`logMessage` is null");
    }
    if (msg == null) {
      throw new IllegalArgumentException("Message is null");
    }
    
    error0(logMessage, msg.deepCopy()
        .set("error", new JsonObject()
            .set("message", logMessage)));
  }

  private void error0(String logMessage, JsonObject msg) {
    boolean handled = false;
    if (msg != null) {
      handled = flow.handleError(this, logMessage, msg, null);
    }
    if (!handled) {
      log_helper(ERROR, msg);
    }
  }

  protected void debug(JsonObject msg) {
    log_helper(DEBUG, msg);
  }

  protected void trace(JsonObject msg) {
    log_helper(TRACE, msg);
  }

  protected void metric() {
    throw new UnsupportedOperationException();
  }

  protected void status(JsonObject status) {
    flow.handleStatus(this, status, null, false);
  }

  private static final int OFF = 1;
  private static final int FATAL = 10;
  private static final int ERROR = 20;
  private static final int WARN = 30;
  private static final int INFO = 40;
  private static final int DEBUG = 50;
  private static final int TRACE = 60;
  private static final int AUDIT = 98;
  private static final int METRIC = 99;

  // ---

  protected static void publish(String topic, JsonObject data) {
    publish(topic, topic, data);
  }

  protected static void publish(String localTopic, String topic, JsonObject data) {
    MessageBus.sendMessage(localTopic, new JsonObject()
        .set("topic", topic)
        .set("data", data));
  }
}
