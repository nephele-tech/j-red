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

import static com.nepheletech.servlet.utils.HttpServletUtil.getJSONBody;
import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.JRedRuntime;
import com.nepheletech.jred.runtime.nodes.InjectNode;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JtonObject;

/**
 * Handle {@code "/inject/:id"} HTTP POST requests.
 */
@WebServlet(urlPatterns = { "/inject/*" })
public class InjectNodeServlet extends HttpServlet {
  private static final long serialVersionUID = -5971350336978314935L;

  private static final Logger logger = LoggerFactory.getLogger(InjectNodeServlet.class);

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doPost: pathInfo={}", req.getPathInfo());

    final String nodeId = getNodeId(req);
    if (nodeId == null) {
      logger.warn("nodeId is null");
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    final InjectNode node = getInjectNode(nodeId);
    if (node != null) {
      try {
        final Optional<JtonObject>  data = getJSONBody(req).asOptJtonObject();
        if (data.isPresent() 
            && data.get().has("__user_inject_props__")) {
          node.receive(data.get());
        } else {
          node.receive(null);
        }
      } catch (Exception e) {
        logger.warn("inject failed", e); // TODO send error
        res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
      }
    } else {
      logger.warn("node is null (id={})", nodeId);
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }
  }

  private InjectNode getInjectNode(String nodeId) {
    final Node node = getFlowsRuntime().getNode(nodeId);
    if (node instanceof InjectNode) {
      return (InjectNode) node;
    } else {
      return null;
    }
  }

  private final JRedRuntime getFlowsRuntime() {
    return (JRedRuntime) getServletContext()
        .getAttribute(JRedRuntime.class.getName());
  }

  private static final String getNodeId(HttpServletRequest req) {
    String nodeId = trimToNull(req.getPathInfo());
    if (nodeId != null && nodeId.startsWith("/")) {
      nodeId = nodeId.substring(1);
    }
    return nodeId;
  }
}
