package com.nepheletech.jred.editor.api.servlet;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.json.JsonObject;
import com.nepheletech.json.JsonParser;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/locales/*" })
public class LocalesServlet extends HttpServlet {
  private static final long serialVersionUID = 5725767012838271487L;

  private static final Logger logger = Logger.getLogger(LocalesServlet.class.getName());

  private static final String DEFAULT_LNG_VALUE = "en-US";
  private static final String DEFAULT_BUNDLE_VALUE = "messages";
  private static final String LOCALES_DIR = "/WEB-INF/locales";

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    final String bundle = getBundle(req);
    final String lng = getLocale(req);
    
    final File file = new File(getDir(req, lng), bundle + ".json");
    logger.fine(() -> String.format("bundle: %s, lng: %s, file: %s", bundle, lng, file));

    if (file.exists() && file.isFile()) {
      HttpServletUtil.sendJSON(res, JsonParser.parse(new String(Files.readAllBytes(file.toPath()), "UTF-8")));
    } else {
      // TODO third party nodes...
      HttpServletUtil.sendJSON(res, new JsonObject());
    }
  }

  private static File getDir(HttpServletRequest req, String lng) {
    final ServletContext servletContext = req.getServletContext();
    final File dir = new File(servletContext.getRealPath(LOCALES_DIR), lng);
    return (dir.exists() && dir.isDirectory()) ? dir
        : new File(servletContext.getRealPath(LOCALES_DIR), DEFAULT_LNG_VALUE);
  }

  private static String getBundle(HttpServletRequest req) {
    final String bundle = req.getPathInfo();
    return ("/node-red".equals(bundle)) ? DEFAULT_BUNDLE_VALUE : bundle;
  }

  private static String getLocale(HttpServletRequest req) {
    final String lng = req.getParameter("lng");
    return (lng != null) ? lng : DEFAULT_LNG_VALUE;
  }
}
