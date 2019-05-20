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
package com.nepheletech.jred.editor;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class MainServlet extends HttpServlet {
  private static final long serialVersionUID = 3874543497304645724L;

  private static final String TEMPLATE_PARAMETER = "template";

  private String template = null;

  @Override
  public void init() throws ServletException {
    final ServletConfig config = getServletConfig();
    this.template = config.getInitParameter(TEMPLATE_PARAMETER);
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (req.getParameter("logout") != null) {
      req.logout();
      res.sendRedirect(req.getContextPath());
    } else if (!req.getRequestURI().endsWith("/")) {
      // XXX normalize request url, required by Node-RED's comms client.
      String queryString = req.getQueryString();
      queryString = (queryString != null) ? "?" + queryString : "";
      res.sendRedirect(req.getContextPath() + queryString);
    } else {
      final Object template = req.getAttribute(TEMPLATE_PARAMETER);
      if (template != null && template instanceof String) {
        forward(req, res, String.format("/WEB-INF/pages/%s.jsp", (String) template));
      } else {
        forward(req, res, this.template);
      }
    }
  }

  private static void forward(HttpServletRequest req, HttpServletResponse res, String template)
      throws ServletException, IOException {
    RequestDispatcher dispatcher = req.getServletContext()
        .getRequestDispatcher(template);
    dispatcher.forward(req, res);
  }
}
