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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonUtil;

public class HttpInNode extends AbstractNode {

  private static final Logger logger = LoggerFactory.getLogger(HttpInNode.class);

  private static final ConcurrentMap<String, WeakReference<HttpInNode>> mappings = new ConcurrentHashMap<>();

  public static HttpInNode byPath(String path) {

    logger.info("-----> keys = {}", mappings.keySet());

    if (mappings.containsKey(path)) {
      final WeakReference<HttpInNode> value = mappings.get(path);
      if (value != null) {
        return value.get();
      }
    } else {
      for (final Entry<String, WeakReference<HttpInNode>> entry : mappings.entrySet()) {

        logger.info("-------------{} --------------- {}", path, entry.getKey());

        if (path.matches(entry.getKey())) {
          return entry.getValue().get();
        }
      }
    }

    return null;
  }

  // ---

  @SuppressWarnings("unused")
  private final String url;
  @SuppressWarnings("unused")
  private final String method;
  @SuppressWarnings("unused")
  private final boolean upload;

  private final List<String> params = new ArrayList<>();

  private final String key;

  public HttpInNode(Flow flow, JtonObject config) {
    super(flow, config);

    final String url = config.get("url").asString(null);
    if (url == null) {
      throw new RuntimeException("missing url");
    }

    // normalize url
    this.url = (url.charAt(0) != '/') ? '/' + url : url;

    this.method = config.get("method").asString("GET").toUpperCase();
    this.upload = config.get("upload").asBoolean();

    // extract path parameters from url
    final StringBuilder sb = new StringBuilder(this.method);
    final String[] parts = url.split("/");
    for (String part : parts) {
      if (StringUtils.trimToNull(part) == null) {
        continue;
      }

      if (part.startsWith(":")) {
        this.params.add(part.substring(1));
        sb.append("/(.+)");
      } else {
        this.params.add(null);
        sb.append("/")
            .append(part);
      }
    }
    this.key = sb.toString();

    // TODO swagger

    if (mappings.containsKey(this.key)) {
      // TODO show warning...
    }

    // last one wins
    logger.debug("Register with key: '{}'", this.key);
    mappings.put(this.key, new WeakReference<>(this));
  }

  @Override
  protected void onClosed(boolean removed) {
    mappings.remove(this.key);
  }

  @Override
  protected void onMessage(final Exchange exchange, final JtonObject msg) {
    logger.trace(">>> onMessage: {}", getId());

    // TODO uploads...

    //
    // pathInfo
    //

    final String pathInfo = JtonUtil.getProperty(msg, "req._pathInfo").asString(null);

    logger.debug("-------------------params={}", params);
    logger.debug("-------------------pathInfo={}", pathInfo);

    final JtonObject _params = new JtonObject();
    JtonUtil.setProperty(msg, "req.params", _params, true);

    if (this.params.size() > 0) {

      if (pathInfo != null) {
        final String[] parts = pathInfo.split("/");
        for (int i = 0, n = params.size(); i < n; i++) {
          if (parts.length > i + 1) {
            final String name = params.get(i);
            if (name != null) {
              _params.set(name, parts[i + 1]);
            }
          }
        }

        send(exchange, msg);
      }

    } else {

      if (pathInfo == null) {
        send(exchange, msg);
      }
    }
  }
}
