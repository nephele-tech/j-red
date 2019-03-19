package com.nepheletech.jred.runtime.nodes;

import static com.nepheletech.servlet.utils.HttpServletUtil.APPLICATION_JSON;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.flows.Flow;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonPrimitive;
import com.nepheletech.servlet.utils.HttpServletUtil;

public class HttpResponseNode extends AbstractNode {
  private static final Logger logger = LoggerFactory.getLogger(HttpResponseNode.class);

  private final int statusCode;
  private final JsonObject headers;

  public HttpResponseNode(Flow flow, JsonObject config) {
    super(flow, config);

    this.statusCode = config.get("statusCode").asInt(-1);

    if (config.has("headers")) {
      headers = config.getAsJsonObject("headers");
    } else {
      headers = null;
    }
  }

  @Override
  protected void onMessage(JsonObject msg) {
    logger.trace(">>> onMessage: msg={}", msg);

    final JsonElement _res = msg.remove("_res");
    if (_res.isJsonTransient()
        && _res.asJsonTransient().getValue() instanceof HttpServletResponse) {
      final HttpServletResponse res = (HttpServletResponse) _res.asJsonTransient().getValue();
      final int statusCode = (this.statusCode == -1) ? msg.get("statusCode").asInt(200) : this.statusCode;
      final JsonObject headers = (this.headers == null || this.headers.size() == 0)
          ? msg.get("headers").asJsonObject(true)
          : this.headers;
      final JsonObject cookies = msg.get("cookies").asJsonObject(true);

      try {
        res.setStatus(statusCode);

        for (String name : headers.keySet()) {
          res.setHeader(name, headers.get(name).asString(""));
        }

        for (String name : cookies.keySet()) {
          final JsonElement value = cookies.get(name);
          if (value.isJsonPrimitive()) {
            final String cookieValue = value.asString(null);
            if (StringUtils.trimToNull(cookieValue) != null) {
              res.addCookie(new Cookie(name, cookieValue));
            }
          } else if (value.isJsonObject()) {
            final JsonObject o = value.asJsonObject();
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
            final JsonElement payload = msg.get("payload");
            if (payload.isJsonPrimitive()) {
              final JsonPrimitive primitive = payload.asJsonPrimitive();
              if (!primitive.isJsonTransient()) {
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
