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
import java.util.Objects;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;
import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jton.JtonParser;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/library/*" })
public class LibraryServlet extends HttpServlet {
  private static final long serialVersionUID = -3749930415204832387L;

  private static final Logger logger = LoggerFactory.getLogger(LibraryServlet.class);

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    final String pathInfo = req.getPathInfo();
    logger.trace(">>> doGet: pathInfo={}", pathInfo);
    final String[] pathInfoParts = pathInfo.split("/");
    final String library = pathInfoParts.length > 1 ? pathInfoParts[1] : null;
    final String type = pathInfoParts.length > 2 ? pathInfoParts[2] : null;
    final String path = pathInfo.substring(library.length() + 1);
    logger.info("library={}, type={}, path={}", library, type, path);
    final Object result = getRuntime().getStorage().getLibraryEntry(type, path);

      if ("flows".equals(type)) {
        HttpServletUtil.sendJSON(res, (JtonElement) result);
      } else {
        HttpServletUtil.sendText(res, Objects.toString(result));
      }
/*
    
    if (HttpServletUtil.acceptsJSON(req)) {
      final Storage storage = getRuntime().getStorage();
      if ("flows".equals(type)) {
        HttpServletUtil.sendJSON(res, (JtonElement) storage.getLibraryEntry(type, path));
      } else {
        HttpServletUtil.sendJSON(res, (JtonElement) storage.getLibraryEntry(type, path));
      }
    } else {
      final Storage storage = getRuntime().getStorage();
      final Object entry = storage.getLibraryEntry(type, path);
      if (entry instanceof JtonElement) {
        HttpServletUtil.sendJSON(res, (JtonElement) entry);
      } else {
        HttpServletUtil.sendText(res, (String) entry);
      }
    }*/
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    final String pathInfo = req.getPathInfo();
    logger.trace(">>> doPost: pathInfo={}", pathInfo);
    final String[] pathInfoParts = pathInfo.split("/");
    final String library = pathInfoParts.length > 1 ? pathInfoParts[1] : null;
    final String type = pathInfoParts.length > 2 ? pathInfoParts[2] : null;
    final String path = pathInfo.substring(library.length() + 1);
    final String body = HttpServletUtil.getBody(req);
    logger.info("library={}, type={}, path={}", library, type, path);
    try {
      if ("flows".equals(type)) {
        final JtonArray flow = JtonParser.parse(body).asJtonArray();
        getRuntime().getStorage().saveLibraryEntry(type, path, new JtonObject(), flow.toString("\t"));
      } else {
        final JtonObject meta = JtonParser.parse(body).asJtonObject();
        final String text = meta.get("text").asString(null);
        meta.remove("text");
        logger.info("type={}, path={}s, meta={}", type, path, meta);
        getRuntime().getStorage().saveLibraryEntry(type, path, meta, text);
      }
      res.setStatus(HttpServletResponse.SC_NO_CONTENT);
    } catch (JsonParseException e) {
      throw new ServletException(e); // TODO
    }
  }

  private FlowsRuntime getRuntime() {
    return (FlowsRuntime) getServletContext().getAttribute(FlowsRuntime.class.getName());
  }
}
