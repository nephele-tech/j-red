/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED project.
 *
 * J-RED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * J-RED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with J-RED.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.editor.Constants;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/settings/*" })
public class SettingsServlet extends HttpServlet implements Constants {
  private static final long serialVersionUID = -5461293724531203226L;

  private static final Logger logger = LoggerFactory.getLogger(SettingsServlet.class);

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doGet: {}", req.getRequestURI());

    if (HttpServletUtil.acceptsJSON(req)) {
      final String pathInfo = req.getPathInfo();
      final JtonObject result;
      if (null != pathInfo && "/user".equals(pathInfo)) {
        result = new JtonObject()
            .set("editor", new JtonObject()
                .set("view", new JtonObject()
                    .set("view-grid-size", 20)
                    .set("view-node-status", true)
                    .set("view-show-tips", true)
                    .set("view-snap-grid", true)
                    .set("view-show-grid", true)));
      } else {
        final String contextPath = req.getContextPath();
        result = new JtonObject()
            .set("apiRootUrl", HttpServletUtil.getBaseUrl(req))
            .set("httpNodeRoot", contextPath + "/http-in/")
            .set("version", NODE_RED_VERSION)
            .set("context", new JtonObject()
                .set("default", "memory")
                .set("stores", new JtonArray()
                    .push("memory")))
            .set("editorTheme", new JtonObject()
                .set("menu", new JtonObject()
                    .set("menu-item-help", new JtonObject()
                        .set("label", "J-RED website")
                        .set("url", "https://github.com/nephele-tech/j-red")))
                .set("palette", new JtonObject()
                    .set("editable", false))
                .set("languages", new JtonArray()
                    .push("de")
                    .push("en-US")
                    .push("ja")
                    .push("ko")
                    .push("zh-CN")))
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
