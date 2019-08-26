package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.jred.editor.Constants;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.logging.Log;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/theme/*" })
public class ThemeServlet extends HttpServlet implements Constants {
  private static final long serialVersionUID = 3329782740958432573L;
  
  private static final Log LOG = Log.get(ThemeServlet.class);
  
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    LOG.t(">>> doGet: ", req.getRequestURI());
    
    if (HttpServletUtil.acceptsJSON(req)) {
      HttpServletUtil.sendJSON(res, new JtonObject()
          .set("page", new JtonObject()
              .set("title", "J-RED Editor: " + req.getContextPath())
              .set("favicon", "favicon.ico")
              .set("tabicon", "red/images/node-red-icon-black.svg"))
          .set("header", new JtonObject()
              .set("title", "J-RED Editor: " + req.getContextPath())
              .set("image", "red/images/node-red.svg"))
          .set("asset", new JtonObject()
              .set("red", "red/red.js")
              .set("main", "red/main.js")));
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

}
