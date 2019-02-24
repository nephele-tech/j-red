package com.nepheletech.jred.editor.api.servlet;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jred.editor.Constants;
import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonElement;
import com.nepheletech.json.JsonObject;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/flows/*" })
public class FlowsServlet extends HttpServlet {
  private static final long serialVersionUID = 1737212371334475104L;

  private static Logger log = Logger.getLogger(FlowsServlet.class.getName());

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    if (HttpServletUtil.acceptsJSON(req)) {
      final String version = req.getHeader(Constants.NODE_RED_API_VERSION);
      if (!(Constants.NODE_RED_API_V1.equals(version)
          || Constants.NODE_RED_API_V2.equals(version))) {
        res.sendError(HttpServletResponse.SC_BAD_REQUEST, new JsonObject()
            .set("code", "invalid_api_version")
            .set("message", "Invalid API Version requested")
            .toString());
        return;
      }
      
      // XXX opts

      final JsonObject result = getRuntime().getFlows();
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
      res.sendError(HttpServletResponse.SC_BAD_REQUEST, new JsonObject()
          .set("code", "invalid_api_version")
          .set("message", "Invalid API Version requested")
          .toString());
      return;
    }
    
    // XXX opts

    final JsonElement flows = HttpServletUtil.getJSONBody(req);
    log.finest(() -> "flows: " + flows.toString(" "));

    final String deploymentType = req.getHeader(Constants.NODE_RED_DEPLOYMENT_TYPE);
    log.fine(() -> String.format("deploymentType: %s, version: %s", deploymentType, version));

    if (Constants.NODE_RED_DEPLOYMENT_TYPE_RELOAD.equals(deploymentType)) {
      final String flowRevision = getRuntime().loadFlows(true);
      if (Constants.NODE_RED_API_V1.equals(version)) {
        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        HttpServletUtil.sendJSON(res, new JsonObject()
            .set("rev", flowRevision));
      }
    } else {
      final JsonArray flowConfig;
      if (Constants.NODE_RED_API_V2.equals(version)) {
        final JsonObject _flows = flows.asJsonObject();
        flowConfig = _flows.get("flows").asJsonArray();
        if (_flows.has("rev")) {
          final String currentVersion = getRuntime().getFlows().get("rev").asString();
          if (!currentVersion.equals(_flows.get("rev").asString())) {
            res.sendError(HttpServletResponse.SC_CONFLICT, new JsonObject()
                .set("code", "version_mismatch")
                .toString());
            return;
          }
        }
      } else {
        flowConfig = flows.asJsonArray();
      }

      // Set flows...
      final String flowRevision = getRuntime().setFlows(flowConfig, deploymentType);
      if (Constants.NODE_RED_API_V1.equals(version)) {
        res.setStatus(HttpServletResponse.SC_NO_CONTENT);
      } else {
        HttpServletUtil.sendJSON(res, new JsonObject()
            .set("rev", flowRevision));
      }
    }
  }

  private FlowsRuntime getRuntime() {
    return (FlowsRuntime) getServletContext().getAttribute(FlowsRuntime.class.getName());
  }
}
