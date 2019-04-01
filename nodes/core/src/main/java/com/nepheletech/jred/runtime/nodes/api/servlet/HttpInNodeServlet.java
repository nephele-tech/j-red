package com.nepheletech.jred.runtime.nodes.api.servlet;

import static com.nepheletech.servlet.utils.HttpServletUtil.APPLICATION_FORM_URLENCODED;
import static com.nepheletech.servlet.utils.HttpServletUtil.APPLICATION_JSON;
import static com.nepheletech.servlet.utils.HttpServletUtil.MULTIPART_FORM_DATA;
import static com.nepheletech.servlet.utils.HttpServletUtil.TEXT_PLAIN;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.nodes.HttpInNode;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonPrimitive;
import com.nepheletech.messagebus.MessageBus;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/http-in/*" })
public class HttpInNodeServlet extends HttpServlet {
  private static final long serialVersionUID = 2459007704268778103L;

  private static final Logger logger = LoggerFactory.getLogger(HttpInNodeServlet.class);

  @Override
  protected void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    final String method = req.getMethod();
    final String pathInfo = req.getPathInfo();

    logger.trace(">>> service: method={}, pathInfo={}", method, pathInfo);

    //
    // Request object.
    //

    final JsonObject request = new JsonObject()
        .set("method", req.getMethod())
        .set("body", getBody(req))
        .set("headers", getHeaders(req))
        .set("query", getQuery(req))
        // TODO params
        .set("cookies", getCookies(req));

    //
    // Message object.
    //

    final JsonObject msg = new JsonObject()
        .set("req", request)
        .set("_req", req, true)
        .set("_res", res, true);

    //
    // For a GET request, 'payload', contains an object of any query string
    // parameters. Otherwise, contains the body of the HTTP request.
    //

    if ("GET".equals(method)) {
      msg.set("payload", request.get("query"));
    } else {
      msg.set("payload", request.get("body"));
    }

    //
    // Message dispatcher...
    //

    final String[] parts = pathInfo != null
        ? pathInfo.split("/")
        : new String[0];

    final StringBuilder sb = new StringBuilder();
    JsonObject _msg = null;
    for (int i = 1; i < parts.length; i++) {
      sb.append("/")
          .append(parts[i]);

      final String url = sb.toString();
      request.set("_pathInfo", pathInfo.substring(url.length()));

      logger.debug("url={}, pathInfo={}", url, request.get("_pathInfo").asString());

      try {
        if (_msg == null || _msg.has("_res")) {
          MessageBus.sendMessage(HttpInNode.createListenerTopic(method, url), _msg = msg.deepCopy());
        } else {
          break;
        }
      } catch (RuntimeException e) {
        throw new ServletException(e);
      }
    }

    if (_msg != null && _msg.has("_res")) {
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  /**
   * 
   * @param req
   * @return the body of the incoming request. The format will depend on the
   *         request.
   * @throws IOException
   */
  private JsonElement getBody(HttpServletRequest req) throws IOException {
    final String method = req.getMethod();

    if ("GET".equals(method)) {
      return new JsonObject();
    } else if ("POST".equals(method)) {
      final String contentType = HttpServletUtil.getContentType(req, APPLICATION_FORM_URLENCODED);
      if (contentType.startsWith(MULTIPART_FORM_DATA)) {
        // TODO file upload
      } else if (contentType.startsWith(APPLICATION_FORM_URLENCODED)) {
        return parseParameters(HttpServletUtil.getBody(req));
      } else if (contentType.startsWith(APPLICATION_JSON)) {
        return HttpServletUtil.getJSONBody(req);
      } else {
        return new JsonPrimitive(HttpServletUtil.getBody(req));
      }
    } else {
      final String contentType = HttpServletUtil.getContentType(req, TEXT_PLAIN);
      if (contentType.startsWith(APPLICATION_JSON)) {
        return HttpServletUtil.getJSONBody(req);
      } else {
        return new JsonPrimitive(HttpServletUtil.getBody(req));
      }
    }

    return new JsonObject(); // should never happen
  }

  /**
   * 
   * @param req
   * @return an object containing the HTTP request headers.
   */
  protected JsonObject getHeaders(HttpServletRequest req) {
    final JsonObject result = new JsonObject();

    for (final Enumeration<String> e = req.getHeaderNames(); e.hasMoreElements();) {
      final String name = e.nextElement();
      result.set(name, req.getHeader(name));
    }

    return result;
  }

  /**
   * 
   * @param req an object containing any query string parameters.
   * @return
   */
  protected JsonObject getQuery(HttpServletRequest req) {
    return parseParameters(req.getQueryString());
  }

  protected JsonObject getCookies(HttpServletRequest req) {
    final JsonObject result = new JsonObject();

    final Cookie[] cookies = req.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        result.set(cookie.getName(), new JsonObject()
            .set("comment", cookie.getComment()))
            .set("domain", cookie.getDomain())
            .set("maxAge", cookie.getMaxAge())
            .set("path", cookie.getPath())
            .set("value", cookie.getValue())
            .set("version", cookie.getVersion())
            .set("secure", cookie.getSecure());
      }
    }

    return result;
  }

  private static JsonObject parseParameters(String params) {
    final JsonObject result = new JsonObject();

    if (params != null && !params.isEmpty()) {
      final String[] pairs = params.split("&");
      if (pairs != null) {
        for (String pair : pairs) {
          final String[] p = pair.split("=");
          if (p != null) {
            final String key;
            final String value;
            if (p.length == 1) {
              key = decode(p[0]);
              value = "";
            } else {
              key = decode(p[0]);
              value = decode(p[1]);
            }
            if (result.has(key)) {
              final JsonElement e = result.get(key);
              if (e.isJsonArray()) {
                e.asJsonArray().push(value);
              } else {
                result.set(key, new JsonArray()
                    .push(e)
                    .push(value));
              }
            } else {
              result.set(key, value);
            }
          }
        }
      }
    }

    return result;
  }

  private static String decode(String s) {
    try {
      return URLDecoder.decode(s, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return s;
    }
  }
}