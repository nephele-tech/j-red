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
