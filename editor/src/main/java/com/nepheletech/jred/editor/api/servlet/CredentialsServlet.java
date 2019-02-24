package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/credentials/*" })
public class CredentialsServlet extends HttpServlet {
  private static final long serialVersionUID = 4277682523653703254L;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (HttpServletUtil.acceptsJSON(req)) {
      final String pathInfo = req.getPathInfo();

      if (pathInfo != null) {
        final String[] parts = pathInfo.split("/");
        if (parts.length > 2) {
          @SuppressWarnings("unused")
          final String type = parts[1];
          final String id = parts[2];
          // sendJSON(res, getRuntimeContext().getCredentials(id));
          return;
        }
      }
    }
    res.sendError(HttpServletResponse.SC_BAD_REQUEST);
  }
}
