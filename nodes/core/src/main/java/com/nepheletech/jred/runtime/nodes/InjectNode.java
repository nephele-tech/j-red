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

import static com.nepheletech.jred.runtime.util.JRedUtil.evaluateNodeProperty;
import static com.nepheletech.jred.runtime.util.JRedUtil.setMessageProperty;
import static java.time.Instant.now;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;

/**
 * Injects a message into a flow either manually or at regular intervals.
 */
public class InjectNode extends AbstractNode {
  private final Logger logger = LoggerFactory.getLogger(InjectNode.class);

  private final JtonArray props;
  private final long repeat;
  private String crontab;
  private final boolean once;
  private final long onceDelay;

  public InjectNode(Flow flow, JtonObject config) {
    super(flow, config);

    /* Handle legacy */
    if (!config.isJtonArray("props")) {
      config.set("props", new JtonArray()
          .push(new JtonObject()
              .set("p", "payload")
              .set("v", config.get("payload"))
              .set("vt", config.get("payloadType")))
          .push(new JtonObject()
              .set("p", "topic")
              .set("v", config.get("topic"))
              .set("vt", "str")));
    } else {
      final JtonArray props = config.getAsJtonArray("props");
      for (int i = 0, iMax = props.size(); i < iMax; i++) {
        final JtonObject propI = props.getAsJtonObject(i);
        if ("payload".equals(propI.getAsString("p", null))
            && !propI.has("v")) {
          propI.set("v", config.get("payload"));
          propI.set("vt", config.get("payloadType"));
        } else if("topic".equals(propI.getAsString("p", null))
            && "str".equals(propI.getAsString("vt", null))
            && !propI.has("v")) {
          propI.set("v", config.get("topic"));
        }
      }
    }
    
    this.props = config.getAsJtonArray("props");
    this.repeat = config.get("repeat").asLong(0L);
    this.crontab = config.get("crontab").asString(null);
    this.once = config.get("once").asBoolean(false);
    this.onceDelay = (long) Math.max(config.get("onceDelay").asDouble(0.1D) * 1000D, 0);
    
    // TODO parse jsonata/jsonpath expressions
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    if (this.once && this.onceDelay > 0L) {
      final long delay = onceDelay;
      fromF("timer:%s?delay=%d&repeatCount=1", getId(), delay)
          .routeId(getType() + ':' + getId() + ":once")
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> setMsg(x, ensureMsg(x, null)))
          .toF("direct:%s", getId());
    } else if (this.repeat > 0L) {
      final long period = repeat * 1000L;
      fromF("timer:%s?period=%d&delay=%d", getId(), period, period)
          .routeId(getType() + ':' + getId() + ":repeat")
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> setMsg(x, ensureMsg(x, null)))
          .toF("direct:%s", getId());
    } else if (this.crontab != null) {
      final String schedule = crontab.replace(' ', '+');
      fromF("cron4j:%s?schedule=%s", getId(), schedule)
          .routeId(getType() + ':' + getId() + ":crontab")
          .toF("log:%s?level=TRACE", logger.getName())
          .process((x) -> setMsg(x, ensureMsg(x, null)))
          .toF("direct:%s", getId());
    }
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    logger.trace(">>> onMessage: {}, {}", getId(), msg);
    
    JtonArray props = this.props;
    
    if (msg.has("__user_inject_props__") 
        && msg.isJtonArray("__user_inject_props__")) {
      props = msg.getAsJtonArray("__user_inject_props__");
    }
    
    props.stream().map(x -> x.asJtonObject())
      .filter(prop -> isNotBlank(prop.getAsString("p", null)))
      .forEach(p -> {
        final String property = p.getAsString("p");
        final String value = p.getAsString("v", "");
        final String valueType = p.getAsString("vt", "str");

        if (!"flow".equals(valueType) && !"global".equals(valueType)) {
          try {
            if ((valueType == null && value.isEmpty())
                || "date".equals(valueType)) {
              setMessageProperty(msg, property, now().toEpochMilli());
            } else if (valueType == null) {
              setMessageProperty(msg, property, value);
            } else if ("none".equals(valueType)) {
              setMessageProperty(msg, property, "");
            } else {
              setMessageProperty(msg, property, 
                  evaluateNodeProperty(value, valueType, this, msg));
            }
          } catch (Exception err) {
            // error(err, msg);
            throw new RuntimeException(err);
          }
        } else {
          try {
            setMessageProperty(msg, property, 
                evaluateNodeProperty(value, valueType, this, msg));
          } catch (Exception err) {
            // error(err, msg);
            throw new RuntimeException(err);
          }
        }
      });

    send(exchange, msg);
  }
}
