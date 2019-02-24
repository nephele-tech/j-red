package com.nepheletech.flows.runtime.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.flows.runtime.events.NodesStartedEvent;
import com.nepheletech.flows.runtime.events.NodesStartedEventListener;
import com.nepheletech.flows.runtime.flows.Flow;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.messagebus.MessageBusListener;

public abstract class AbstractNode implements Node {
  private final Logger logger = LoggerFactory.getLogger(AbstractNode.class);

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
    updateWires(config.get("wires"));

    if (this instanceof NodesStartedEventListener) {
      nodesStartedEventListener = new MessageBusListener<NodesStartedEvent>() {
        @Override
        public void messageSent(String topic, NodesStartedEvent message) {
          try {
            ((NodesStartedEventListener) AbstractNode.this).onNodesStarted(message);
          } catch (Exception e) {
            logger.error("Unhandled exception", e);
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
  public void updateWires(JsonElement wires) {
    logger.debug("UPDATE {}", this.id);

    this.wires = wires.isJsonArray() ? wires.asJsonArray() : new JsonArray();
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
        this._wire = this.wires.get(0).asJsonArray().get(0).asString();
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
      error(e, msg);
    }
  }

  protected abstract void onMessage(JsonObject msg);

  protected void log(JsonObject msg) {
    final JsonObject o = new JsonObject()
        .set("id", getId())
        .set("type", getType())
        .set("msg", msg);

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

    LoggerFactory.getLogger(getClass()).info(o.toString());
  }

  protected void error(String logMessage, JsonObject msg) {
    boolean handled = false;
    if (msg != null) {
      handled = flow.handleError(this, new RuntimeException(logMessage), msg, null);
    }
    if (!handled) {
      final JsonObject o = new JsonObject()
          .set("id", getId())
          .set("type", getType())
          .set("msg", logMessage);

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

      LoggerFactory.getLogger(getClass()).error(o.toString());
    }
  }

  protected void error(RuntimeException e, JsonObject msg) {
    // TODO Auto-generated method stub

  }

}
