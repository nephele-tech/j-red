/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED Commons project.
 *
 * J-RED Commons is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * J-RED Commons is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this J-RED Commons; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.nepheletech.jred.console.servlet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.DistributedManager;
import org.apache.catalina.Manager;
import org.apache.catalina.manager.ManagerServlet;
import org.apache.catalina.util.ContextName;
import org.apache.commons.io.FileUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.tomcat.util.res.StringManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.console.Constants;
import com.nepheletech.jred.console.util.AntExecutor;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonObject;
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
      doPost(req, res);
    }
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doPost: queryString={}, pathInfo={}", req.getQueryString(), req.getPathInfo());

    final String command = req.getPathInfo();

    final String path = req.getParameter("path");
    final ContextName cn = (path != null)
        ? new ContextName(path, req.getParameter("version"))
        : null;

    String message = null;

    StringManager smClient = StringManager.getManager(org.apache.catalina.manager.Constants.Package, req.getLocales());

    if (command == null || command.isEmpty() || command.startsWith("/list")) {
      list(req, res);
    } else if ("/start".equals(command)) {
      message = start(cn, smClient);
    } else if ("/stop".equals(command)) {
      message = stop(cn, smClient);
    } else if ("/reload".equals(command)) {
      message = reload(cn, smClient);
    } else if ("/clone".equals(command)) {
      String workspace = req.getParameter("workspace");
      if (workspace == null || !workspace.matches("^/[a-zA-Z0-9_]+$")) {
        message = "FAIL - Invalid workspace name";
      } else {
        message = clone(cn, workspace, smClient);
      }
    } else if ("/deploy".equals(command)) {
      String workspace = req.getParameter("workspace");
      if (workspace == null || !workspace.matches("^/[a-zA-Z0-9_]+$")) {
        message = "FAIL - Invalid workspace name";
      } else {
        message = deploy(workspace, smClient);
      }
    } else if ("/undeploy".equals(command)) {
      message = undeploy(cn, smClient);
    } else if ("/export".equals(command)) {
      export(req, res, cn, smClient);
      return;
    } else {
      list(req, res);
      return;
    }

    HttpServletUtil.sendJSON(res, new JtonObject()
        .set("message", message)
        .set("items", list()));
  }

  protected String start(ContextName cn, StringManager smClient) {
    logger.trace(">>> start: {}", cn);

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);

    super.start(printWriter, cn, smClient);

    return stringWriter.toString();
  }

  protected String stop(ContextName cn, StringManager smClient) {
    logger.trace(">>> stop: {}", cn);

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);

    super.stop(printWriter, cn, smClient);

    return stringWriter.toString();
  }

  protected String reload(ContextName cn, StringManager smClient) {
    logger.trace(">>> reload: {}", cn);

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);

    super.reload(printWriter, cn, smClient);

    return stringWriter.toString();
  }

  protected String clone(ContextName cn, String workspace, StringManager smClient) {
    logger.trace(">>> clone: {}", workspace);

    String name = workspace.startsWith("/")
        ? workspace.substring(1)
        : workspace;

    try {
      name = URLEncoder.encode(name, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    name += ".war";

    File buildFile = new File(getServletContext().getRealPath(Constants.CLONE_WORKSPACE_BUILD_FILE));
    File buildDir = new File(ServletContext.TEMPDIR, UUID.randomUUID().toString());
    File webappsDir = new File(System.getProperty("user.dir"), "webapps");

    Map<String, String> props = new HashMap<>();
    props.put("build.dir", buildDir.getAbsolutePath());
    props.put("webapps.dir", webappsDir.getAbsolutePath());
    props.put("context.name", cn.getName());
    props.put("war.name", name);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintStream outputStream = new PrintStream(baos);

    try {
      AntExecutor.executeAntTask(buildFile, "clone", props,
          AntExecutor.createBuildLogger(outputStream, outputStream));
    } catch (Exception e) {
      logger.error("ANT failure", e);
    } finally {
      try {
        FileUtils.deleteDirectory(buildDir);
      } catch (IOException e) {
        logger.warn("Cleanup error.", e);
      }
    }

    return baos.toString();
  }

  protected synchronized String deploy(String workspace, StringManager smClient) {
    logger.trace(">>> deploy: {}", workspace);

    Path tmpDir = null;

    try {
      tmpDir = Files.createTempDirectory(null);
      
      final File templateDir = new File(getServletContext().getRealPath("/WEB-INF/template"));
      final File pomFile0 = new File(templateDir, "pom.xml");
      if (pomFile0.exists()) {
        logger.info("---POM file: {}", pomFile0);
      } else {
        logger.warn("---POM file does not exists: {}", pomFile0);
      }
      
      FileUtils.copyDirectory(templateDir, tmpDir.toFile());

      final File workingDirectory = tmpDir.toFile();
      final File pomFile = new File(workingDirectory, "pom.xml");
      if (pomFile.exists()) {
        logger.info("POM file: {}", pomFile);
      } else {
        logger.warn("POM file does not exists: {}", pomFile);
      }

      InvocationRequest request = new DefaultInvocationRequest();
      request.setBatchMode(true);
      request.setPomFile(pomFile);
      request.setGoals(Arrays.asList("clean", "package", "install"));

      Invoker invoker = new DefaultInvoker();
      // invoker.setMavenExecutable(new
      // File("/Users/ggeorg/Applications/apache-maven-3.6.0/bin/mvn"));
      // invoker.setMavenHome(new File(mavenHome));
      invoker.setWorkingDirectory(workingDirectory);

      @SuppressWarnings("unused")
      InvocationResult result = invoker.execute(request);

      String name = workspace.startsWith("/")
          ? workspace.substring(1)
          : workspace;
      name = URLEncoder.encode(name, "UTF-8");

      logger.info("----------------------------- name={}", name);

      // Identify the appBase of the owning Host of this Context (if any)
      final File target = new File(host.getAppBase(), name + ".war");
      logger.info("----------------------------- name={}", target);
      if (target.exists()) { return smClient.getString("htmlManagerServlet.deployUploadWarExists", workspace); }

      if (host.findChild(name) != null && !isDeployed(name)) {
        return smClient.getString("htmlManagerServlet.deployUploadInServerXml", workspace);
      }

      if (isServiced(name)) {
        return smClient.getString("managerServlet.inService", name);
      } else {
        addServiced(name);
        try {
          File source = new File(workingDirectory, "target/jred-editor-template.war");
          Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
          // Perform new deployment
          check(name);
        } finally {
          removeServiced(name);
        }
      }

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();

      return smClient.getString("htmlManagerServlet.deployUploadFail", e.getMessage());
    } finally {
      if (tmpDir != null) {
        try {
          FileUtils.deleteDirectory(tmpDir.toFile());
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    return "OK";
  }

  protected String undeploy(ContextName cn, StringManager smClient) {
    logger.trace(">>> undeploy: {}", cn);

    final StringWriter stringWriter = new StringWriter();
    final PrintWriter printWriter = new PrintWriter(stringWriter);

    super.undeploy(printWriter, cn, smClient);

    return stringWriter.toString();
  }

  protected void export(HttpServletRequest req, HttpServletResponse res,
      ContextName cn, StringManager smClient) throws ServletException, IOException {
    logger.trace(">>> export:");

    String userDir = System.getProperty("user.dir");

    File buildFile = new File(getServletContext().getRealPath(Constants.EXPORT_WORKSPACE_BUILD_FILE));
    File buildDir = new File(ServletContext.TEMPDIR, UUID.randomUUID().toString());
    File webappsDir = new File(userDir, "webapps");

    Map<String, String> props = new HashMap<>();
    props.put("build.dir", buildDir.getAbsolutePath());
    props.put("webapps.dir", webappsDir.getAbsolutePath());
    props.put("context.name", cn.getName());
    props.put("context.extension", ".zip");

    byte[] data = null;

    try {
      AntExecutor.executeAntTask(buildFile, "export", props);
      data = Files.readAllBytes(new File(buildDir, cn.getName() + ".zip").toPath());
    } catch (Exception e) {
      logger.error("Export to flows failed", e);
    } finally {
//      try {
//        FileUtils.deleteDirectory(buildDir);
//      } catch (IOException e) {
//        logger.warn("Cleanup error.", e);
//      }
    }

    // ---
    res.setContentType(HttpServletUtil.APPLICATION_OCTET_STREAM);
    res.setHeader("Content-Disposition",
        String.format("attachment; filename=\"%s%s\"", cn.getBaseName(), ".zip"));
    res.setHeader("Content-Length", Integer.toString(data.length));
    res.getOutputStream().write(data);
    res.getOutputStream().flush();
    // ---
    return;
  }

  protected void list(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> list:");

    final JtonArray workspaces = list();

    HttpServletUtil.sendJSON(res, workspaces);
  }

  protected JtonArray list() throws UnsupportedEncodingException {
    logger.trace(">>> list:");

    final JtonArray workspaces = new JtonArray();

    for (Container child : host.findChildren()) {
      final String name = child.getName();
      final Context context = (Context) host.findChild(name);

      if (context != null) {
        final String displayName = context.getDisplayName();
        if (displayName == null || !displayName.startsWith("J-RED Editor")) {
          continue;
        }

        final JtonObject workspace = new JtonObject();
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

        final StringBuilder tmp = new StringBuilder();
        tmp.append("path=");
        tmp.append(URLEncoder.encode(displayPath, "UTF-8"));
        if (context.getWebappVersion().length() > 0) {
          tmp.append("&version=");
          tmp.append(URLEncoder.encode(context.getWebappVersion(), "UTF-8"));
        }

        workspace.set("pathVersion", tmp.toString());

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
    }

    return workspaces;
  }
}
