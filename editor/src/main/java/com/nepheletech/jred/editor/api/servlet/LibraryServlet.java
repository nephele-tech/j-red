package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.jton.JtonObject;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/library/*" })
public class LibraryServlet extends HttpServlet {
  private static final long serialVersionUID = -3749930415204832387L;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (HttpServletUtil.acceptsJSON(req)) {
      HttpServletUtil.sendJSON(res, new JtonObject());
    } else {
      throw new UnsupportedOperationException("other");
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    throw new UnsupportedOperationException();
  }
}
