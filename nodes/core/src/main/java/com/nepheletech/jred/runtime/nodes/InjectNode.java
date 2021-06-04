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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

/**
 * 
 */
public class InjectNode extends AbstractNode implements Processor {
  private final Logger logger = LoggerFactory.getLogger(InjectNode.class);

  private final String topic;
  private final String payload;
  private final String payloadType;
  private final long repeat;
  private String crontab;
  private final boolean once;
  private final long onceDelay;

  public InjectNode(Flow flow, JtonObject config) {
    super(flow, config);
    this.topic = config.get("topic").asString("");
    this.payload = config.get("payload").asString("");
    this.payloadType = config.get("payloadType").asString("");
    this.repeat = config.get("repeat").asLong(0L);
    this.crontab = config.get("crontab").asString(null);
    this.once = config.get("once").asBoolean(false);
    this.onceDelay = (long) Math.max(config.get("onceDelay").asDouble(0.1D) * 1000D, 0);
  }

  @Override
  public void configure() throws Exception {
    super.configure();

    if (this.once && this.onceDelay > 0L) {
      final long delay = onceDelay;
      fromF("timer:%s?delay=%d&repeatCount=1", getId(), delay)
          .toF("log:%s?level=TRACE", logger.getName())
          .process(InjectNode.this)
          .toF("direct:%s", getId());
    } else if (this.repeat > 0L) {
      final long period = repeat * 1000L;
      fromF("timer:%s?period=%d&delay=%d", getId(), period, period)
          .toF("log:%s?level=TRACE", logger.getName())
          .process(InjectNode.this)
          .toF("direct:%s", getId());
    } else if (this.crontab != null) {
      final String schedule = crontab.replace(' ', '+');
      fromF("cron4j:%s?schedule=%s", getId(), schedule)
          .toF("log:%s?level=TRACE", logger.getName())
          .process(InjectNode.this)
          .toF("direct:%s", getId());
    }
  }

  @Override
  public void process(Exchange exchange) throws Exception {
    exchange.getIn().setBody(ensureMsg(exchange, null));
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    logger.trace(">>> onMessage: {}", getId());

    msg.set("topic", topic);

    if (!"flow".equals(payloadType) && !"global".equals(payloadType)) {
      try {
        if ((payloadType == null && payload.isEmpty())
            || "date".equals(payloadType)) {
          msg.set("payload", System.currentTimeMillis());
        } else if (payloadType == null) {
          msg.set("payload", payload);
        } else if ("none".equals(payloadType)) {
          msg.set("payload", "");
        } else {
          msg.set("payload", evaluateNodeProperty(payload, payloadType, this, msg));
        }
      } catch (Exception err) {
        // error(err, msg);
      }
    } else {
      try {
        msg.set("payload", evaluateNodeProperty(payload, payloadType, this, msg));
      } catch (Exception err) {
        // error(err, msg);
      }
    }

    send(exchange, msg);
  }
}
