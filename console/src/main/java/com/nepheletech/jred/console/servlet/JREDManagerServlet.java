package com.nepheletech.jred.console.servlet;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Context;
import org.apache.catalina.DistributedManager;
import org.apache.catalina.Manager;
import org.apache.catalina.manager.ManagerServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.json.JsonArray;
import com.nepheletech.json.JsonObject;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = { "/manager/*" })
public class JREDManagerServlet extends ManagerServlet {
  private static final long serialVersionUID = -7134551811306924674L;

  private static final Logger logger = LoggerFactory.getLogger(JREDManagerServlet.class);

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doGet: queryString={}, pathInfo={}", req.getQueryString(), req.getPathInfo());

    final String command = req.getPathInfo();

    if (command == null || command.isEmpty() || command.startsWith("/list")) {
      list(req, res);
    } else {
      // send not found
    }
  }

  protected void list(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doList");

    final JsonArray workspaces = new JsonArray();

    Arrays.asList(host.findChildren()).forEach(child -> {
      final String name = child.getName();
      final Context context = (Context) host.findChild(child.getName());
      if (context != null) {
        final String displayName = context.getDisplayName();
        if (displayName == null || !displayName.startsWith("J-RED Editor")) {
          return;
        }
        
        final JsonObject workspace = new JsonObject();
        String displayPath = context.getPath();
        if ("".equals(displayPath)) {
          displayPath = "/";
        }

        workspace.set("name", name);
        workspace.set("version", displayName);
        workspace.set("displayPath", displayPath);

        // TODO see parallel deployements...
        workspace.set("webappVersion", context.getWebappVersion());
        
        workspace.set("isAvailable", context.getState().isAvailable());

        final Manager manager = context.getManager();
        if (manager instanceof DistributedManager) {
          workspace.set("activeSessions", ((DistributedManager) manager).getActiveSessionsFull());
        } else if (manager != null) {
          workspace.set("activeSessions", manager.getActiveSessions());
        } else {
          workspace.set("activeSessions", "0");
        }

        try {
          workspace.set("isDeployed", isDeployed(name));
        } catch (Exception e) { // JMX invocation error
          workspace.set("isDeployed", false);
        }
        
        workspaces.push(workspace);
      }

    });
    
    HttpServletUtil.sendJSON(res, workspaces);
  }
}
