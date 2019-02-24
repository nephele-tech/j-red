package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.nepheletech.jred.editor.Constants;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParser;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/settings/*" })
public class SettingsServlet extends HttpServlet implements Constants {
  private static final long serialVersionUID = -5461293724531203226L;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (HttpServletUtil.acceptsJSON(req)) {
      final String pathInfo = req.getPathInfo();
      final JsonObject result;
      if (null != pathInfo && "/user".equals(pathInfo)) {
        result = new JsonObject()
            .set("editor", new JsonObject()
                .set("view", new JsonObject()
                    .set("view-grid-size", 20)
                    .set("view-node-status", true)
                    .set("view-show-tips", true)
                    .set("view-snap-grid", true)
                    .set("view-show-grid", true)));
      } else {
        final String contextPath = req.getContextPath();
        result = new JsonObject()
            .set("httpNodeRoot", contextPath + "/http-in/")
            .set("version", NODE_RED_VERSION)
            .set("context", new JsonObject()
                .set("default", "memory")
                .set("stores", new JsonArray()
                    .push("memory")))
            .set("editorTheme", new JsonObject()
                .set("menu", new JsonObject()
                    .set("menu-item-help", new JsonObject()
                        .set("label", "J-RED website")
                        .set("url", "https://github.com/nephele-tech/j-red")))
                .set("palette", new JsonObject()
                    .set("editable", false)))
            .set("flowEncryptionType", "system")
            .set("tlsConfigDisableLocalFiles", false);
      }
      HttpServletUtil.sendJSON(res, result);
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    final String pathInfo = req.getPathInfo();

    if ("/user".equals(pathInfo)) {
      if (HttpServletUtil.acceptsJSON(req)) {
        try {
          final HttpSession session = req.getSession();
          session.setAttribute("/settins/user", JsonParser.parse(HttpServletUtil.getBody(req)));
          res.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (RuntimeException e) {
          // LOG.w(e, ExceptionUtils.getRootCauseMessage(e));
          res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
      }
    }
  }
}
