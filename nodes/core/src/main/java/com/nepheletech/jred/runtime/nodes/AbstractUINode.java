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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;

public abstract class AbstractUINode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(AbstractUINode.class);

  private final MustacheFactory mf;
  private Mustache mustache;

  public AbstractUINode(Flow flow, JtonObject config, String template) {
    super(flow, config);

    mf = new DefaultMustacheFactory();

    try (InputStream is = getClass().getClassLoader().getResourceAsStream(template)) {
      mustache = mf.compile(new InputStreamReader(is), template);
    } catch (IOException e) {
      mustache = null;

      // TODO log

      throw new RuntimeException(e);
    }
  }

  @Override
  protected void onMessage(Exchange exchange, JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final StringWriter w = new StringWriter();
    mustache.execute(w, prepare(msg.deepCopy()));
    msg.set("payload", w.toString());

    send(exchange, msg);
  }

  protected Object prepare(final JtonObject msg) {
    // TODO

    return msg;
  }
}
