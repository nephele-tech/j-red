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

import static com.nepheletech.servlet.utils.HttpServletUtil.APPLICATION_JSON;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JtonPrimitive;
import com.nepheletech.servlet.utils.HttpServletUtil;

public class HttpResponseNode extends AbstractNode {
  private final int statusCode;
  private final JtonObject headers;

  public HttpResponseNode(Flow flow, JtonObject config) {
    super(flow, config);

    this.statusCode = config.get("statusCode").asInt(-1);

    if (config.has("headers")) {
      headers = config.getAsJtonObject("headers");
    } else {
      headers = null;
    }
  }

  @Override
  protected void onMessage(JtonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final JtonElement _res = msg.remove("_res");
    if (_res.isJtonPrimitive() && _res.asJtonPrimitive().isJtonTransient()
        && _res.asJtonPrimitive().getValue() instanceof HttpServletResponse) {
      final HttpServletResponse res = (HttpServletResponse) _res.asJtonPrimitive().getValue();
      final int statusCode = (this.statusCode == -1) ? msg.get("statusCode").asInt(200) : this.statusCode;
      final JtonObject headers = (this.headers == null || this.headers.size() == 0)
          ? msg.get("headers").asJtonObject(true)
          : this.headers;
      final JtonObject cookies = msg.get("cookies").asJtonObject(true);

      try {
        res.setStatus(statusCode);

        for (String name : headers.keySet()) {
          res.setHeader(name, headers.get(name).asString(""));
        }

        for (String name : cookies.keySet()) {
          final JtonElement value = cookies.get(name);
          if (value.isJtonPrimitive()) {
            final String cookieValue = value.asString(null);
            if (StringUtils.trimToNull(cookieValue) != null) {
              res.addCookie(new Cookie(name, cookieValue));
            }
          } else if (value.isJtonObject()) {
            final JtonObject o = value.asJtonObject();
            final String cookieValue = o.get("value").asString("");
            if (StringUtils.trimToEmpty(cookieValue) != null) {
              final Cookie cookie = new Cookie(name, cookieValue);
              // cookie.setDomain(domain);
              // cookie.setHttpOnly(isHttpOnly);
              cookie.setMaxAge(o.get("maxAge").asInt(-1));
              // cookie.setPath(uri);
              // cookie.setSecure(flag);
              // cookie.setVersion(v);
              res.addCookie(cookie);
            }
          }
        }

        if (headers.has("Content-Type") || headers.has("content-type")) {
          final String contentType = headers.has("Content-Type")
              ? headers.get("Content-Type").asString()
              : headers.get("content-type").asString();

          if (contentType.startsWith(APPLICATION_JSON)) {
            HttpServletUtil.sendJSON(res, msg.get("payload"));
          } else if (contentType.startsWith("application/octet-stream")) { // TODO
            throw new UnsupportedOperationException();
          } else {
            final JtonElement payload = msg.get("payload");
            if (payload.isJtonPrimitive()) {
              final JtonPrimitive primitive = payload.asJtonPrimitive();
              if (!primitive.isJtonTransient()) {
                HttpServletUtil.send(res, contentType, primitive.asString());
              } else {
                throw new UnsupportedOperationException();
              }
            } else {
              throw new UnsupportedOperationException();
            }
          }

        } else {
          HttpServletUtil.sendJSON(res, msg.get("payload"));
        }

      } catch (Exception e) {
        e.printStackTrace();

        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        } else {
          throw new RuntimeException(e);
        }
      }

    } else {
      // TODO node.warn(RED._("httpin.errors.no-response"));
      logger.warn("No response object");
    }
  }

}
