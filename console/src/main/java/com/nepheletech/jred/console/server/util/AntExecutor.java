package com.nepheletech.jred.console.server.util;

import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;

public class AntExecutor {
  private static final Logger logger = Logger.getLogger(AntExecutor.class.getName());
  
  public static boolean executeAntTask(File buildFile) {
    return executeAntTask(buildFile, null, null, getConsoleLogger());
  }
  
  public static boolean executeAntTask(File buildFile, String target, Map<String, String> props) {
    return executeAntTask(buildFile, target, props, getConsoleLogger());
  }
  
  public static boolean executeAntTask(File buildFile, String target, Map<String, String> props, BuildLogger logger) {
    BuildLogger consoleLogger = (logger != null) ? logger : getConsoleLogger();
    Project project = new Project();
    project.setUserProperty("ant.file", buildFile.getAbsolutePath());
    project.addBuildListener((BuildListener)consoleLogger);
    if (props != null)
      for (Map.Entry<String, String> entry : props.entrySet())
        project.setProperty(entry.getKey(), entry.getValue());  
    boolean success = false;
    try {
      project.fireBuildStarted();
      project.init();
      ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
      project.addReference("ant.projectHelper", projectHelper);
      projectHelper.parse(project, buildFile);
      String targetToExecute = (target != null && target.trim().length() > 0) ? target.trim() : project.getDefaultTarget();
      project.executeTarget(targetToExecute);
      project.fireBuildFinished(null);
      success = true;
    } catch (BuildException buildException) {
      project.fireBuildFinished((Throwable)buildException);
      throw new RuntimeException("!!! Unable to execute build file !!!", buildException);
    } 
    return success;
  }
  
  private static BuildLogger getConsoleLogger() {
    DefaultLogger logger = new DefaultLogger();
    logger.setErrorPrintStream(System.err);
    logger.setOutputPrintStream(System.out);
    logger.setMessageOutputLevel(2);
    return (BuildLogger)logger;
  }
  
  public static BuildLogger createBuildLogger(PrintStream outputStream, PrintStream errorStream) {
    DefaultLogger logger = new DefaultLogger() {
        protected void log(String message) {
          AntExecutor.logger.info(message);
        }
      };
    logger.setErrorPrintStream(errorStream);
    logger.setOutputPrintStream(outputStream);
    logger.setMessageOutputLevel(2);
    return (BuildLogger)logger;
  }
}