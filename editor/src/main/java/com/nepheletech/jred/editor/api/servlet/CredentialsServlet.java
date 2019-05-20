/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED project.
 *
 * J-RED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * J-RED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with J-RED.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/credentials/*" })
public class CredentialsServlet extends HttpServlet {
  private static final long serialVersionUID = 4277682523653703254L;
  
  private static final Logger logger = LoggerFactory.getLogger(CredentialsServlet.class);

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doGet: pathInfo={} (accepts={})", req.getPathInfo(), req.getHeader("Accept"));
    
    logger.trace("---------------");
    
    if (HttpServletUtil.acceptsJSON(req)) {
      final String pathInfo = req.getPathInfo();

      if (pathInfo != null) {
        final String[] parts = pathInfo.split("/");

        logger.trace("---------------{}", Arrays.asList(parts));
        
        if (parts.length > 2) {
          final String type = parts[1];
          final String id = parts[2];
          final JtonObject credentials = getRuntime().getNodeCredentials(type, id);
          logger.debug("credentials: {}", credentials);
          HttpServletUtil.sendJSON(res, credentials);
          return;
        } else {
          logger.warn("invalid request (pathInfo={})", pathInfo);
        }
      }
    }
    res.sendError(HttpServletResponse.SC_BAD_REQUEST);
  }

  private final FlowsRuntime getRuntime() {
    return (FlowsRuntime) getServletContext().getAttribute(FlowsRuntime.class.getName());
  }
}
