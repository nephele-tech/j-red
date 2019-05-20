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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.jred.editor.Constants;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/flows/*" })
public class FlowsServlet extends HttpServlet {
  private static final long serialVersionUID = 1737212371334475104L;

  private static Logger logger = LoggerFactory.getLogger(FlowsServlet.class);

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doGet:");
    
    if (HttpServletUtil.acceptsJSON(req)) {
      final String version = req.getHeader(Constants.NODE_RED_API_VERSION);
      if (!(Constants.NODE_RED_API_V1.equals(version)
          || Constants.NODE_RED_API_V2.equals(version))) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, new JtonObject()
            .set("code", "invalid_api_version")
            .set("message", "Invalid API Version requested")
            .toString());
        return;
      }
      
      logger.debug("{} = {}", Constants.NODE_RED_API_VERSION, version);

      final JtonObject result = getRuntime().getFlows();
      if (Constants.NODE_RED_API_V1.equals(version)) {
        HttpServletUtil.sendJSON(res, result.get("flows"));
      } else if (Constants.NODE_RED_API_V2.equals(version)) {
        HttpServletUtil.sendJSON(res, result);
      }
    } else {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST);
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    final String version = req.getHeader(Constants.NODE_RED_API_VERSION);
    if (!(Constants.NODE_RED_API_V1.equals(version)
        || Constants.NODE_RED_API_V2.equals(version))) {
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, new JtonObject()
          .set("code", "invalid_api_version")
          .set("message", "Invalid API Version requested")
          .toString());
      return;
    }
    
    // XXX opts

    final JtonElement flows = HttpServletUtil.getJSONBody(req);
    
    if (logger.isDebugEnabled()) {
      logger.debug("flows: {}", flows.toString(" "));
    }

    final String deploymentType = req.getHeader(Constants.NODE_RED_DEPLOYMENT_TYPE);
    
    if (logger.isDebugEnabled()) {
      logger.debug("deploymentType: {}, version: {}", deploymentType, version);
    }

    if (Constants.NODE_RED_DEPLOYMENT_TYPE_RELOAD.equals(deploymentType)) {
      final String flowRevision = getRuntime().loadFlows(true);
      if (Constants.NODE_RED_API_V1.equals(version)) {
        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        HttpServletUtil.sendJSON(res, new JtonObject()
            .set("rev", flowRevision));
      }
    } else {
      final JtonArray flowConfig;
      if (Constants.NODE_RED_API_V2.equals(version)) {
        final JtonObject _flows = flows.asJtonObject();
        flowConfig = _flows.get("flows").asJtonArray();
        if (_flows.has("rev")) {
          final String currentVersion = getRuntime().getFlows().get("rev").asString();
          if (!currentVersion.equals(_flows.get("rev").asString())) {
            res.sendError(HttpServletResponse.SC_CONFLICT, new JtonObject()
                .set("code", "version_mismatch")
                .toString());
            return;
          }
        }
      } else {
        flowConfig = flows.asJtonArray();
      }

      // Set flows...
      final String flowRevision = getRuntime().setFlows(flowConfig, deploymentType);
      if (Constants.NODE_RED_API_V1.equals(version)) {
        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        HttpServletUtil.sendJSON(res, new JtonObject()
            .set("rev", flowRevision));
      }
    }
  }

  private FlowsRuntime getRuntime() {
    return (FlowsRuntime) getServletContext().getAttribute(FlowsRuntime.class.getName());
  }
}
