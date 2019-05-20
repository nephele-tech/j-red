/*
 * Copyright NepheleTech and other contributorns, http://www.nephelerech.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nepheletech.jred.runtime.nodes.api.servlet;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jred.runtime.nodes.InjectNode;
import com.nepheletech.jred.runtime.nodes.Node;

/**
 * Handle {@code "/inject/:id"} HTTP POST requests.
 */
@WebServlet(urlPatterns = { "/inject/*" })
public class InjectNodeServlet extends HttpServlet {
  private static final long serialVersionUID = -5971350336978314935L;

  private static final Logger logger = LoggerFactory.getLogger(InjectNodeServlet.class);

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doPost: pathInfo={}, queryString={}", req.getPathInfo(), req.getQueryString());

    final String nodeId = getNodeId(req);
    if (nodeId == null) {
      logger.warn("nodeId is null");
      res.sendError(HttpServletResponse.SC_NOT_FOUND);
      return;
    }

    final InjectNode node = getInjectNode(nodeId);
    if (node instanceof InjectNode) {
      try {
        node.receive(null);
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
    final FlowsRuntime flowsRuntime = getFlowsRuntime();
    final Node node = flowsRuntime.getNode(nodeId);
    if (node instanceof InjectNode) {
      return (InjectNode) node;
    } else {
      return null;
    }
  }

  private final FlowsRuntime getFlowsRuntime() {
    return (FlowsRuntime) getServletContext().getAttribute(FlowsRuntime.class.getName());
  }

  private static final String getNodeId(HttpServletRequest req) {
    String nodeId = trimToNull(req.getPathInfo());
    if (nodeId != null && nodeId.startsWith("/")) {
      nodeId = nodeId.substring(1);
    }
    return nodeId;
  }
}
