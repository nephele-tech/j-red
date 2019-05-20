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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;

import com.google.common.base.Predicate;
import com.google.common.io.Resources;
import com.nepheletech.jred.runtime.nodes.Node;
import com.nepheletech.jton.JsonParser;
import com.nepheletech.jton.JsonSyntaxException;
import com.nepheletech.jton.JtonObject;
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
      final JtonObject data = JsonParser
          .parse(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8))
          .asJtonObject(true);

      if (DEFAULT_BUNDLE_VALUE.equals(bundle)) {
        final Predicate<String> filter = new FilterBuilder().include(".*\\.json");
        final Reflections r = new Reflections(new ConfigurationBuilder()
            .filterInputsBy(filter)
            .setScanners(new ResourcesScanner())
            .setUrls(ClasspathHelper.forPackage(Node.class.getPackage().getName() + ".locales")));
        final Set<String> resourceSet = r.getResources(Pattern.compile(".*"));
        resourceSet.forEach(resourceName -> {
          if (resourceName.contains(DEFAULT_LNG_VALUE)) {
            try {
              data.putAll(JsonParser
                  .parse(Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8))
                  .asJtonObject(true));
            } catch (JsonSyntaxException | IOException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        });

        if (!lng.equals(DEFAULT_LNG_VALUE)) {
          resourceSet.forEach(resourceName -> {
            if (resourceName.contains(lng)) {
              try {
                data.putAll(JsonParser
                    .parse(Resources.toString(Resources.getResource(resourceName), StandardCharsets.UTF_8))
                    .asJtonObject(true));
              } catch (JsonSyntaxException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
              }
            }
          });
        }
      }

      HttpServletUtil.sendJSON(res, data);
    } else {
      // TODO third party nodes...
      HttpServletUtil.sendJSON(res, new JtonObject());
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
