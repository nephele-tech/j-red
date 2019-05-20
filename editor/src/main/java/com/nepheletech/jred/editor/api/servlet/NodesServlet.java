/*
 *     This file is part of J-RED project.
 *
 *   J-RED is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   J-RED is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with J-RED.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.jred.editor.nodes.NodeRegistry;
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
