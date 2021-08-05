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
package com.nepheletech.jred.runtime.nodes.api.servlet;

import static org.apache.commons.lang3.StringUtils.trim;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.JRedRuntime;
import com.nepheletech.jred.runtime.nodes.DebugNode;
import com.nepheletech.jred.runtime.nodes.Node;

/**
 * Handle {@code "/debug/:id/:state"} HTTP POST requests.
 */
@WebServlet(urlPatterns = { "/_debug/*" })
public class DebugNodeServlet extends HttpServlet {
  private static final long serialVersionUID = 1831852522502901838L;

  private static final Logger logger = LoggerFactory.getLogger(DebugNodeServlet.class);

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doPost");
    
    final PathInfo input = getPathInfo(req);
    if (input == null) {
      logger.warn("input is null");
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    final DebugNode debugNode = getDebugNode(input.id);
    if (debugNode != null) {
      if ("enable".equals(input.state)) {
        debugNode.setActive(true);
        res.sendError(HttpServletResponse.SC_OK);
        if (debugNode.isTostatus()) {
//          debugNode.status(new JsonObject()
//              .set("fill", "grey")
//              .set("shape", "dot"));
        }
      } else if ("disable".equals(input.state)) {
        debugNode.setActive(false);
        res.sendError(HttpServletResponse.SC_CREATED);
        if (debugNode.isTostatus()) {
//          debugNode.status(new JsonObject()
//              .set("fill", "grey")
//              .set("shape", "dot"));
        }
      } else {
        logger.warn("invalid state {}", input.state);
        res.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    } else {
      logger.warn("not found: {}", input.id);
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
    }
  }

  private DebugNode getDebugNode(String nodeId) {
    final JRedRuntime flowsRuntime = getFlowsRuntime();
    final Node node = flowsRuntime.getNode(nodeId);
    if (node instanceof DebugNode) {
      return (DebugNode) node;
    } else {
      return null;
    }
  }

  private final JRedRuntime getFlowsRuntime() {
    return (JRedRuntime) getServletContext().getAttribute(JRedRuntime.class.getName());
  }

  private static final PathInfo getPathInfo(HttpServletRequest req) {
    String input = trimToNull(req.getPathInfo());
    if (input != null && input.startsWith("/")) {
      input = input.substring(1);
    }

    final String[] inputs = input.split("/");
    if (inputs.length == 2) {
      return new PathInfo(trim(inputs[0]), trim(inputs[1]));
    } else {
      return null;
    }
  }

  private static class PathInfo {
    final String id;
    final String state;

    public PathInfo(String id, String state) {
      this.id = id;
      this.state = state;
    }
  }
}
