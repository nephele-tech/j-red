package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.jton.JtonObject;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/context/*" })
public class ContextServlet extends HttpServlet {
  private static final long serialVersionUID = 5970804334034275774L;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    final String pathInfo = req.getPathInfo();
    if (pathInfo != null && pathInfo.startsWith("flow/")) {
      HttpServletUtil.sendJSON(res, new JtonObject()
          .set("memory", new JtonObject()));
    } else {
      HttpServletUtil.sendJSON(res, new JtonObject()
          .set("memory", new JtonObject()));
    }
  }
}
