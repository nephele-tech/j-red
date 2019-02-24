package com.nepheletech.flows.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.flows.editor.nodes.NodeRegistry;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/nodes/*" })
public class NodesServlet extends HttpServlet {
  private static final long serialVersionUID = 2566873476623669713L;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (HttpServletUtil.acceptsJSON(req)) {
      HttpServletUtil.sendJSON(res, getNodeRegistry().getNodeList());
    } else if (HttpServletUtil.acceptsHTML(req)) {
      HttpServletUtil.sendHTML(res, getNodeRegistry().getNodeConfigs());
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  private NodeRegistry getNodeRegistry() {
    return (NodeRegistry) getServletContext().getAttribute(NodeRegistry.class.getName());
  }
}
