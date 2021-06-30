/*
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

import static com.nepheletech.jred.runtime.util.JRedUtil.getMessageProperty;
import static com.nepheletech.jred.runtime.util.JRedUtil.toJtonArray;

import java.util.Arrays;
import java.util.function.BiConsumer;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jred.runtime.util.JRedUtil;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonNull;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;

public final class DebugNode extends AbstractNode {

  // The maximum length, in characters, of any message sent to the debug sidebar
  // tab
  private final int DEBUG_MAX_LENGTH = 1000; // TODO settings

// Colourise the console output of the debug node 
  private final boolean USE_COLORS = false; // TODO settings

  private boolean active;

  private final boolean tosidebar;
  private final boolean console;
  private final boolean tostatus;
  private final String statusType;
  private final String statusVal;
  private final String complete;
  private final int severity;

  private JtonObject oldState;

  private final String preparedEditExpression;
  private final String preparedStatExpression;

  public DebugNode(Flow flow, JtonObject config) {
    super(flow, config);

    final String targetType = config.getAsString("targetType", "msg");
    final boolean hasEditExpression = "jsonpath".equals(targetType);
    final String editExpression = hasEditExpression ? config.getAsString("complete") : null;

    final String complete = hasEditExpression ? null : config.getAsString("complete", "payload");
    this.complete = "false".equals(complete) ? "payload" : complete;

    this.console = config.getAsBoolean("console", false);
    this.tostatus = !"true".equals(this.complete) && config.getAsBoolean("tostatus", false);
    this.statusVal = config.getAsString("", this.complete);
    this.statusType = config.getAsString("statusType", "auto");
    this.tosidebar = config.getAsBoolean("tosidebar", true);

    this.severity = config.get("severity").asInt(40);

    this.active = config.getAsBoolean("active", true);

    if (this.tostatus) {
      this.status(new JtonObject().set("fill", "grey").set("shape", "ring"));
      this.oldState = new JtonObject();
    }

    boolean hasStatExpression = "jsonpath".equals(this.statusType);
    String statExpression = hasStatExpression ? config.getAsString("statusVal") : null;

    if (editExpression != null) {
      preparedEditExpression = editExpression;
    } else {
      preparedEditExpression = null;
    }

    if (statExpression != null) {
      preparedStatExpression = statExpression;
    } else {
      preparedStatExpression = null;
    }
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    logger.trace(">>> setActive: {}", active);

    this.active = active;
  }

  public boolean isTosidebar() {
    return tosidebar;
  }

  public boolean isTostatus() {
    return tostatus;
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    logger.trace(">>> onMessage: exchange={}, msg={}", exchange, msg);

    if (getId().equals(getMessageProperty(msg, "status.source.id").asString(null))) {
      // done()
      return;
    }

    if (tostatus) {
      prepareStatus(msg, (err, debugMsg) -> {
        if (err != null) {
          error(err, msg);
          return;
        }
        final JtonElement output = debugMsg.get("msg");
        String st = output.isJtonPrimitive() && output.asJtonPrimitive().isString()
            ? output.asString()
            : output.toString();
        String fill = "grey";
        String shape = "dot";
        if (output.isJtonObject()
            && output.asJtonObject().has("fill")
            && output.asJtonObject().has("shape")
            && output.asJtonObject().has("text")) {
          final JtonObject _output = output.asJtonObject();
          fill = _output.getAsString("fill");
          shape = _output.getAsString("shape");
          st = _output.getAsString("text");
        }
        if ("auto".equals(statusType)) {
          if (msg.has("error")) {
            fill = "red";
            st = getMessageProperty(msg, "error.message").asString("");
          }
          if (msg.has("status")) {
            fill = getMessageProperty(msg, "status.fill").asString("grey");
            shape = getMessageProperty(msg, "status.shape").asString("ring");
            st = getMessageProperty(msg, "status.text").asString("");
          }
        }

        if (st.length() > 32) {
          st = st.substring(0, 32) + "...";
        }
        
        final JtonObject newStatus = new JtonObject()
            .set("fill", fill)
            .set("shape", shape)
            .set("text", st);

        if (!newStatus.equals(oldState)) { // only send if we have to
          status(newStatus);
          oldState = newStatus; // TODO JSON.stringify(newStatus);
        }
      });
    }

    if ("true".equals(complete)) {
      // debug complete msg object
      if (this.console) {
        log(msg); // TODO: colors & depth
      }
      if (active && tosidebar) {
        sendDebug(new JtonObject()
            .set("id", getId())
            .set("z", getZ())
            .set("_alias", getAlias())
            .set("path", getFlow().getPath())
            .set("name", StringUtils.stripToEmpty(getName()))
            .set("topic", msg.get("topic"))
            .set("msg", msg));
      }
    } else {
      prepareValue(msg, (error, debugMsg) -> {
        if (error != null) {
          error(error, msg); // FIXME
          return;
        }
        final JtonElement output = debugMsg.get("msg");
        if (this.console) {
          if (output.isJtonPrimitive() && output.asJtonPrimitive().isString()) {
            log(msg); // FIXME
          } else if (output.isJtonObject()) {
            log(msg);
          } else {
            log(msg);
          }
        }
        if (active) {
          if(tosidebar) {
            sendDebug(debugMsg);
          }
        }
      });
    }
  }

  @Override
  protected void onClosed(boolean removed) {
    if (oldState != null) {
      status(new JtonObject());
    }
  }

  private void sendDebug(JtonObject msg) {
    logger.trace(">>> sendDebug: active={}, msg={}", active, msg);

    // don't put blank errors in sidebar (but do add to logs)
    msg = JRedUtil.encodeObject(msg, new JtonObject().set("maxLength", DEBUG_MAX_LENGTH));

    JRedUtil.publish("debug", "debug", msg);
  }

  private void prepareValue(JtonObject msg, BiConsumer<Throwable, JtonObject> done) {
    // Either apply the jsonpath expression or...
    if (preparedEditExpression != null) {
      final JtonElement value = JRedUtil.evaluateJsonPathExpression(msg, preparedEditExpression);
      done.accept(null, new JtonObject()
          .set("id", getId())
          .set("z", getZ())
          .set("_alias", getAlias())
          .set("path", getFlow().getPath())
          .set("name", StringUtils.trimToEmpty(getName()))
          .set("topic", msg.get("topic"))
          .set("msg", value));
    } else {
      // Extract the required message property
      String property = "payload";
      JtonElement output = msg.get(property);
      if (!"false".equals(complete) && complete != null) {
        property = this.complete;
        try {
          output = JRedUtil.getMessageProperty(msg, property);
        } catch (Exception err) {
          err.printStackTrace(); // FIXME
          output = JtonNull.INSTANCE;
        }
      }
      done.accept(null, new JtonObject()
          .set("id", getId())
          .set("z", getZ())
          .set("_alias", getAlias())
          .set("path", getFlow().getPath())
          .set("name", StringUtils.stripToEmpty(getName()))
          .set("topic", msg.get("topic"))
          .set("property", property)
          .set("msg", output));
    }
  }

  private void prepareStatus(JtonObject msg, BiConsumer<Throwable, JtonObject> done) {
    if ("auto".equals(statusType)) {
      if ("true".equals(complete)) {
        done.accept(null, new JtonObject()
            .set("msg", msg.get("payload")));
      } else {
        prepareValue(msg, (err, debugMsg) -> {
            if (err != null) {
              done.accept(err, null);
            } else {
              done.accept(null, new JtonObject()
                  .set("msg", debugMsg.get("msg")));
            }
          }
        );
      }
    } else {
      // Either apply the jsonata expression or...
      if (preparedStatExpression != null) {
        try {
          done.accept(null, new JtonObject()
              .set("msg", JRedUtil.evaluateJsonPathExpression(msg, preparedStatExpression)));
        } catch (Exception err) {
          done.accept(err, null);
        }
      } else {
        // Extract the required message property
        JtonElement output = null;

        try {
          output = JRedUtil.getMessageProperty(msg, statusVal);
          done.accept(null, new JtonObject()
              .set("msg", output));
        } catch (Exception err) {
          done.accept(err, new JtonObject()
              .set("msg", output));
        }
      }
    }
  }
}
