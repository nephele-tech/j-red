package com.nepheletech.jred.console.server;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

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
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.FileItemFactory;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.res.StringManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.console.server.util.AntExecutor;
import com.nepheletech.jred.console.server.util.MavenUtils;
import com.nepheletech.jred.console.server.util.WorkspaceUtils;
import com.nepheletech.jred.console.server.util.ZipUtils;
import com.nepheletech.jton.JtonArray;
import com.nepheletech.jton.JtonElement;
import com.nepheletech.jton.JtonObject;
import com.nepheletech.servlet.utils.HttpServletUtil;

@WebServlet(urlPatterns = {"/manager/*"})
public class JREDManagerServlet extends ManagerServlet {
  private static final long serialVersionUID = 7595850776983747862L;
  
  private static final Logger logger = LoggerFactory.getLogger(JREDManagerServlet.class);
  
  public void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> doGet: req={}", req);
    String command = req.getPathInfo();
    if (((String)Optional.<String>ofNullable(command).orElse("")).isEmpty() || command
      .startsWith("/list")) {
      list(req, res);
    } else {
      doPost(req, res);
    } 
  }
  
  public void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.info(">>> doPost: req={}", req);
    String command = req.getPathInfo();
    String path = req.getParameter("path");
    ContextName cn = (path != null) ? new ContextName(path, req.getParameter("version")) : null;
    logger.info("--------------{}-------------{}", command, cn);
    String message = null;
    StringManager smClient = StringManager.getManager("org.apache.catalina.manager", req.getLocales());
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
      if (workspace == null || !workspace.matches("^[a-zA-Z0-9_\\-]+$")) {
        message = "FAIL - Invalid workspace name";
      } else {
        message = clone(cn, workspace, smClient);
      } 
    } else if ("/deploy".equals(command)) {
      String workspace = req.getParameter("workspace");
      if (workspace == null || !workspace.matches("^[a-zA-Z0-9_\\-]+$")) {
        message = "FAIL - Invalid workspace name";
      } else {
        message = deploy(workspace, smClient);
      } 
    } else if ("/undeploy".equals(command)) {
      message = undeploy(cn, smClient);
    } else {
      if ("/export".equals(command)) {
        export(req, res, cn, smClient);
        return;
      } 
      if ("/import".equals(command)) {
        logger.info("Importing: {}", path);
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        logger.info("{}", Boolean.valueOf(isMultipart));
        if (isMultipart) {
          try {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            factory.setSizeThreshold(0);
            ServletContext servletContext = getServletContext();
            File repository = (File)servletContext.getAttribute("javax.servlet.context.tempdir");
            factory.setRepository(repository);
            ServletFileUpload upload = new ServletFileUpload((FileItemFactory)factory);
            Map<String, List<FileItem>> items = upload.parseParameterMap(req);
            for (Map.Entry<String, List<FileItem>> e : items.entrySet()) {
              for (FileItem fileItem : e.getValue()) {
                logger.info("File item: {}", fileItem);
                message = doImport(cn, ((DiskFileItem)fileItem).getStoreLocation(), smClient);
              } 
            } 
          } catch (Exception e) {
            e.printStackTrace();
          } 
        } else {
          logger.warn("!!! Not a multipart request");
        } 
      } else {
        if ("/config".equals(command)) {
          String config = req.getParameter("config");
          if ("GET".equals(req.getMethod())) {
            HttpServletUtil.sendText(res, loadConfig(cn, config, smClient));
          } else {
            String content = HttpServletUtil.getBody(req);
            HttpServletUtil.sendText(res, applyConfig(cn, config, content, smClient));
          } 
          return;
        } 
        if ("/docker".equals(command)) {
          message = dockerPush(cn, req.getParameter("account"), smClient);
        } else {
          list(req, res);
          return;
        } 
      } 
    } 
    HttpServletUtil.sendJSON(res, (JtonElement)(new JtonObject())
        .set("message", message)
        .set("items", (JtonElement)list()));
  }
  
  protected void list(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    logger.trace(">>> list: req={}", req);
    HttpServletUtil.sendJSON(res, (JtonElement)list());
  }
  
  protected JtonArray list() throws UnsupportedEncodingException {
    logger.trace(">>> list:");
    JtonArray workspaces = new JtonArray();
    for (Container child : this.host.findChildren()) {
      String name = child.getName();
      Context context = (Context)this.host.findChild(name);
      if (context != null) {
        String displayName = context.getDisplayName();
        if (displayName != null && displayName.startsWith("J-RED Editor")) {
          JtonObject workspace = new JtonObject();
          String displayPath = context.getPath();
          if ("".equals(displayPath))
            displayPath = "/"; 
          workspace.set("name", name);
          workspace.set("version", displayName);
          workspace.set("displayPath", displayPath);
          workspace.set("webappVersion", context.getWebappVersion());
          workspace.set("isAvailable", Boolean.valueOf(context.getState().isAvailable()));
          StringBuilder tmp = new StringBuilder();
          tmp.append("path=");
          tmp.append(URLEncoder.encode(displayPath, "UTF-8"));
          if (context.getWebappVersion().length() > 0) {
            tmp.append("&version=");
            tmp.append(URLEncoder.encode(context.getWebappVersion(), "UTF-8"));
          } 
          workspace.set("pathVersion", tmp.toString());
          Manager manager = context.getManager();
          if (manager instanceof DistributedManager) {
            workspace.set("activeSessions", Integer.valueOf(((DistributedManager)manager).getActiveSessionsFull()));
          } else if (manager != null) {
            workspace.set("activeSessions", Integer.valueOf(manager.getActiveSessions()));
          } else {
            workspace.set("activeSessions", "0");
          } 
          try {
            workspace.set("isDeployed", Boolean.valueOf(isDeployed(name)));
          } catch (Exception e) {
            workspace.set("isDeployed", Boolean.valueOf(false));
          } 
          workspaces.push((JtonElement)workspace);
        } 
      } 
    } 
    workspaces.sort((a, b) -> a.asJtonObject().getAsString("name", "").compareTo(b.asJtonObject().getAsString("name", "")));
    return workspaces;
  }
  
  protected String start(ContextName cn, StringManager smClient) {
    logger.trace(">>> start: {}", cn);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    start(printWriter, cn, smClient);
    return stringWriter.toString();
  }
  
  protected String stop(ContextName cn, StringManager smClient) {
    logger.trace(">>> stop: {}", cn);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    stop(printWriter, cn, smClient);
    return stringWriter.toString();
  }
  
  protected String reload(ContextName cn, StringManager smClient) {
    logger.trace(">>> reload: {}", cn);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    reload(printWriter, cn, smClient);
    return stringWriter.toString();
  }
  
  protected String clone(ContextName cn, String workspace, StringManager smClient) {
    logger.info(">>> clone: context={}, cn={}, smClient={}", new Object[] { this.context, cn, smClient });
    Context context = (Context)this.host.findChild(cn.getName());
    Path tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory(null, (FileAttribute<?>[])new FileAttribute[0]);
      File workingDirectory = tmpDir.toFile();
      File templateDir = new File(getServletContext().getRealPath("/WEB-INF/template"));
      WorkspaceUtils.copyFiles(context, templateDir, workingDirectory);
      logger.info("----------------------------- workspace={}", workspace);
      File target = new File(this.host.getAppBase(), workspace + ".war");
      logger.info("----------------------------- target={}", target);
      ContextName newCn = new ContextName(workspace, true);
      String name = newCn.getName();
      if (this.host.findChild(name) != null && !isDeployed(name))
        return smClient.getString("htmlManagerServlet.deployUploadInServerXml", new Object[] { workspace }); 
      if (isServiced(name))
        return smClient.getString("managerServlet.inService", new Object[] { name }); 
      File pomFile = new File(workingDirectory, "pom.xml");
      InvocationResult result = MavenUtils.invokeMaven(workingDirectory, pomFile, null, 
          Arrays.asList(new String[] { "clean", "package" }));
      if (result.getExitCode() != 0) {
        logger.error("Build failed: {}", result.toString());
        if (result.getExecutionException() != null)
          throw new IllegalStateException("Build failed: " + result.getExitCode(), result.getExecutionException()); 
        throw new IllegalStateException("Build failed: " + result.getExitCode());
      } 
      addServiced(name);
      try {
        File source = new File(workingDirectory, "target/jred-editor-template.war");
        Files.copy(source.toPath(), target.toPath(), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
        check(name);
      } finally {
        removeServiced(name);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      return smClient.getString("htmlManagerServlet.deployUploadFail", new Object[] { e.getMessage() });
    } finally {
      if (tmpDir != null)
        try {
          FileUtils.deleteDirectory(tmpDir.toFile());
        } catch (IOException e) {
          e.printStackTrace();
        }  
    } 
    return "OK";
  }
  
  protected synchronized String deploy(String workspace, StringManager smClient) {
    logger.trace(">>> deploy: {}", workspace);
    Path tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory(null, (FileAttribute<?>[])new FileAttribute[0]);
      File workingDirectory = tmpDir.toFile();
      File templateDir = new File(getServletContext().getRealPath("/WEB-INF/template"));
      WorkspaceUtils.copyFiles(null, templateDir, workingDirectory);
      File target = new File(this.host.getAppBase(), workspace + ".war");
      if (target.exists())
        return smClient.getString("htmlManagerServlet.deployUploadWarExists", new Object[] { workspace }); 
      ContextName cn = new ContextName(workspace, true);
      String name = cn.getName();
      if (this.host.findChild(name) != null && !isDeployed(name))
        return smClient.getString("htmlManagerServlet.deployUploadInServerXml", new Object[] { workspace }); 
      if (isServiced(name))
        return smClient.getString("managerServlet.inService", new Object[] { name }); 
      File pomFile = new File(workingDirectory, "pom.xml");
      InvocationResult result = MavenUtils.invokeMaven(workingDirectory, pomFile, null, 
          Arrays.asList(new String[] { "clean", "package" }));
      if (result.getExitCode() != 0) {
        logger.error("Build failed: {}", result.toString());
        if (result.getExecutionException() != null)
          throw new IllegalStateException("Build failed: " + result.getExitCode(), result.getExecutionException()); 
        throw new IllegalStateException("Build failed: " + result.getExitCode());
      } 
      addServiced(name);
      try {
        File source = new File(workingDirectory, "target/jred-editor-template.war");
        Files.copy(source.toPath(), target.toPath(), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
        check(name);
      } finally {
        removeServiced(name);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      return smClient.getString("htmlManagerServlet.deployUploadFail", new Object[] { e.getMessage() });
    } finally {
      if (tmpDir != null)
        try {
          FileUtils.deleteDirectory(tmpDir.toFile());
        } catch (IOException e) {
          e.printStackTrace();
        }  
    } 
    return "OK - '" + workspace + "' deployed";
  }
  
  protected String undeploy(ContextName cn, StringManager smClient) {
    logger.trace(">>> undeploy: {}", cn);
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    undeploy(printWriter, cn, smClient);
    return stringWriter.toString();
  }
  
  protected void export(HttpServletRequest req, HttpServletResponse res, ContextName cn, StringManager smClient) throws ServletException, IOException {
    logger.trace(">>> export:");
    Path tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory(null, (FileAttribute<?>[])new FileAttribute[0]);
      File buildXml = new File(getServletContext().getRealPath("WEB-INF/ant-scripts/export-workspace-build.xml"));
      File templateDir = new File(getServletContext().getRealPath("/WEB-INF/template"));
      File buildDir = tmpDir.toFile();
      File webappsDir = new File(this.host.getAppBase());
      Map<String, String> props = new HashMap<>();
      props.put("build.dir", buildDir.getAbsolutePath());
      props.put("template.dir", templateDir.getAbsolutePath());
      props.put("webapps.dir", webappsDir.getAbsolutePath());
      props.put("context.name", cn.getName());
      props.put("context.extension", ".zip");
      AntExecutor.executeAntTask(buildXml, "export", props);
      byte[] data = Files.readAllBytes((new File(buildDir, cn.getName() + ".zip")).toPath());
      res.setContentType("application/octet-stream");
      res.setHeader("Content-Disposition", 
          String.format("attachment; filename=\"%s%s\"", new Object[] { cn.getBaseName(), ".zip" }));
      res.setHeader("Content-Length", Integer.toString(data.length));
      res.getOutputStream().write(data);
      res.getOutputStream().flush();
    } finally {
      if (tmpDir != null)
        try {
          FileUtils.deleteDirectory(tmpDir.toFile());
        } catch (IOException e) {
          e.printStackTrace();
        }  
    } 
  }
  
  protected String doImport(ContextName cn, File fileZip, StringManager smClient) {
    logger.info("doImport: cn={}, fileZip={}, smClient={}", new Object[] { cn, fileZip, smClient });
    Path tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory(null, (FileAttribute<?>[])new FileAttribute[0]);
      ZipUtils.unzip(fileZip, tmpDir.toFile());
      File workingDirectory = tmpDir.toFile();
      String workspace = cn.getBaseName();
      String name = cn.getName();
      File target = new File(this.host.getAppBase(), workspace + ".war");
      if (this.host.findChild(workspace) != null && !isDeployed(workspace))
        return smClient.getString("htmlManagerServlet.deployUploadInServerXml", new Object[] { workspace }); 
      if (isServiced(name))
        return smClient.getString("managerServlet.inService", new Object[] { name }); 
      File pomFile = new File(workingDirectory, "pom.xml");
      InvocationResult result = MavenUtils.invokeMaven(workingDirectory, pomFile, null, 
          Arrays.asList(new String[] { "clean", "package" }));
      if (result.getExitCode() != 0) {
        logger.error("Build failed: {}", result.toString());
        if (result.getExecutionException() != null)
          throw new IllegalStateException("Build failed: " + result.getExitCode(), result.getExecutionException()); 
        throw new IllegalStateException("Build failed: " + result.getExitCode());
      } 
      addServiced(name);
      try {
        File source = new File(workingDirectory, "target/jred-editor-template.war");
        Files.copy(source.toPath(), target.toPath(), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
        check(name);
      } finally {
        removeServiced(name);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      return smClient.getString("htmlManagerServlet.deployUploadFail", new Object[] { e.getMessage() });
    } finally {
      if (tmpDir != null)
        try {
          FileUtils.deleteDirectory(tmpDir.toFile());
        } catch (IOException e) {
          e.printStackTrace();
        }  
    } 
    return "OK - '" + cn.getBaseName() + "' deployed";
  }
  
  protected String loadConfig(ContextName cn, String config, StringManager smClient) throws IOException {
    logger.trace(">>> loadConfig: cn={}, config={}, smClient={}", new Object[] { cn, config, smClient });
    Context context = (cn != null) ? (Context)this.host.findChild(cn.getName()) : null;
    if ("webapp".equals(config)) {
      File configFile = new File(context.getRealPath("/WEB-INF/web.xml"));
      if (configFile.exists())
        return new String(Files.readAllBytes(configFile.toPath())); 
    } else if ("context".equals(config)) {
      File configFile = new File(context.getRealPath("/META-INF/context.xml"));
      if (configFile.exists())
        return new String(Files.readAllBytes(configFile.toPath())); 
    } else if ("maven".equals(config)) {
      File configFile = new File(context.getRealPath("/META-INF/maven/com.nepheletech/jred-editor-template/pom.xml"));
      if (configFile.exists())
        return new String(Files.readAllBytes(configFile.toPath())); 
    } else if ("m2-settings".equals(config)) {
      File configFile = new File(System.getProperty("user.home"), ".m2/settings.xml");
      if (configFile.exists())
        return new String(Files.readAllBytes(configFile.toPath())); 
    } 
    return config;
  }
  
  protected String applyConfig(ContextName cn, String config, String content, StringManager smClient) throws IOException {
    logger.info(">>> applyConfig: cn={}, content={}, smClient={}", new Object[] { cn, config, smClient });
    Context context = (cn != null) ? (Context)this.host.findChild(cn.getName()) : null;
    if ("webapp".equals(config)) {
      File configFile = new File(context.getRealPath("/WEB-INF/web.xml"));
      File backupFile = new File(context.getRealPath("/WEB-INF/web.xml~"));
      FileUtils.copyFile(configFile, backupFile);
      FileUtils.write(configFile, content);
      return reload(cn, smClient);
    } 
    if ("context".equals(config)) {
      File configFile = new File(context.getRealPath("/META-INF/context.xml"));
      File backupFile = new File(context.getRealPath("/META-INF/context.xml~"));
      FileUtils.copyFile(configFile, backupFile);
      FileUtils.write(configFile, content);
      String message = reload(cn, smClient);
      try {
        check(cn.getName());
      } catch (Exception e) {
        e.printStackTrace();
      } 
      return message;
    } 
    if ("maven".equals(config))
      return rebuild(context, cn, content, smClient); 
    if ("m2-settings".equals(config)) {
      File configFile = new File(System.getProperty("user.home"), ".m2/settings.xml");
      File backupFile = new File(System.getProperty("user.home"), ".m2/settings.xml~");
      FileUtils.copyFile(configFile, backupFile);
      FileUtils.write(configFile, content);
      return "OK";
    } 
    return config;
  }
  
  protected synchronized String rebuild(Context context, ContextName cn, String content, StringManager smClient) {
    logger.info(">>> rebuild: context={}, cn={}, smClient={}", new Object[] { context, cn, smClient });
    Path tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory(null, (FileAttribute<?>[])new FileAttribute[0]);
      File workingDirectory = tmpDir.toFile();
      File templateDir = new File(getServletContext().getRealPath("/WEB-INF/template"));
      WorkspaceUtils.copyFiles(context, templateDir, workingDirectory);
      File pomFile = new File(workingDirectory, "pom.xml");
      FileUtils.write(pomFile, content);
      String workspace = cn.getBaseName();
      logger.info("----------------------------- workspace={}", workspace);
      File target = new File(this.host.getAppBase(), workspace + ".war");
      logger.info("----------------------------- name={}", target);
      String name = cn.getName();
      if (this.host.findChild(name) != null && !isDeployed(name))
        return smClient.getString("htmlManagerServlet.deployUploadInServerXml", new Object[] { workspace }); 
      if (isServiced(name))
        return smClient.getString("managerServlet.inService", new Object[] { name }); 
      InvocationResult result = MavenUtils.invokeMaven(workingDirectory, pomFile, null, 
          Arrays.asList(new String[] { "clean", "package" }));
      if (result.getExitCode() != 0) {
        logger.error("Build failed: {}", result.toString());
        if (result.getExecutionException() != null)
          throw new IllegalStateException("Build failed: " + result.getExitCode(), result.getExecutionException()); 
        throw new IllegalStateException("Build failed: " + result.getExitCode());
      } 
      addServiced(name);
      try {
        File source = new File(workingDirectory, "target/jred-editor-template.war");
        Files.copy(source.toPath(), target.toPath(), new CopyOption[] { StandardCopyOption.REPLACE_EXISTING });
        check(name);
      } finally {
        removeServiced(name);
      } 
    } catch (Exception e) {
      e.printStackTrace();
      return smClient.getString("htmlManagerServlet.deployUploadFail", new Object[] { e.getMessage() });
    } finally {
      if (tmpDir != null)
        try {
          FileUtils.deleteDirectory(tmpDir.toFile());
        } catch (IOException e) {
          e.printStackTrace();
        }  
    } 
    return "OK";
  }
  
  private String dockerPush(ContextName cn, String account, StringManager smClient) {
    logger.info(">>> dockerPush: cn={}, smClient={}", cn, smClient);
    Path tmpDir = null;
    try {
      tmpDir = Files.createTempDirectory(null, (FileAttribute<?>[])new FileAttribute[0]);
      Context context = (Context)this.host.findChild(cn.getName());
      File workingDirectory = tmpDir.toFile();
      File templateDir = new File(getServletContext().getRealPath("/WEB-INF/template"));
      WorkspaceUtils.copyFiles(context, templateDir, workingDirectory);
      File pomFile = new File(workingDirectory, "pom.xml");
      if (pomFile.exists()) {
        logger.info("POM file: {}", pomFile);
      } else {
        logger.warn("POM file does not exists: {}", pomFile);
      } 
      Properties properties = new Properties();
      properties.setProperty("workspace.name", cn.getBaseName());
      if (account != null && !account.trim().isEmpty()) {
        properties.setProperty("docker.username", account);
        properties.setProperty("docker.password", "N3ph3l301.");
      } 
      InvocationResult result = MavenUtils.invokeMaven(workingDirectory, pomFile, properties, 
          Arrays.asList(new String[] { "clean", "package", "docker:build", "docker:push" }));
      if (result.getExitCode() != 0) {
        logger.error("Build failed: {}", result.toString());
        if (result.getExecutionException() != null)
          throw new IllegalStateException("Build failed: " + result.getExitCode(), result.getExecutionException()); 
        throw new IllegalStateException("Build failed: " + result.getExitCode());
      } 
      String workspace = cn.getName();
      String name = workspace.startsWith("/") ? workspace.substring(1) : workspace;
      name = URLEncoder.encode(name, "UTF-8");
    } catch (Exception e) {
      e.printStackTrace();
      return smClient.getString("htmlManagerServlet.deployUploadFail", new Object[] { e.getMessage() });
    } finally {
//      if (tmpDir != null)
//        try {
//          FileUtils.deleteDirectory(tmpDir.toFile());
//        } catch (IOException e) {
//          e.printStackTrace();
//        }  
    } 
    return "OK - '" + cn.getBaseName() + "' pushed";
  }
}
