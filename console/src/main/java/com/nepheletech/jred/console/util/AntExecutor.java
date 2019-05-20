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
package com.nepheletech.jred.console.util;

import java.io.File;
import java.io.PrintStream;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.tools.ant.BuildException;
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
    BuildLogger consoleLogger = logger != null ? logger : getConsoleLogger();

    // Prepare Ant project
    Project project = new Project();
    project.setUserProperty("ant.file", buildFile.getAbsolutePath());
    project.addBuildListener(consoleLogger);

    // Set properties
    if (props != null) {
      for (Map.Entry<String, String> entry : props.entrySet()) {
        project.setProperty(entry.getKey(), entry.getValue());
      }
    }

    boolean success = false;

    // Capture event for Ant script build start / stop / failure
    try {
      project.fireBuildStarted();
      project.init();
      ProjectHelper projectHelper = ProjectHelper.getProjectHelper();
      project.addReference("ant.projectHelper", projectHelper);
      projectHelper.parse(project, buildFile);

      // If no target specified then default target will be executed.
      String targetToExecute = (target != null && target.trim().length() > 0) ? target.trim()
          : project.getDefaultTarget();
      project.executeTarget(targetToExecute);
      project.fireBuildFinished(null);
      success = true;
    } catch (BuildException buildException) {
      project.fireBuildFinished(buildException);
      throw new RuntimeException("!!! Unable to execute build file !!!", buildException);
    }

    return success;
  }

  /**
   * Logger to log output generated while executing and script console.
   * 
   * @return
   */
  private static BuildLogger getConsoleLogger() {
    DefaultLogger logger = new DefaultLogger();
    logger.setErrorPrintStream(System.err);
    logger.setOutputPrintStream(System.out);
    logger.setMessageOutputLevel(Project.MSG_INFO);
    return logger;
  }

  public static BuildLogger createBuildLogger(PrintStream outputStream, PrintStream errorStream) {
    DefaultLogger logger = new DefaultLogger() {
      protected void log(java.lang.String message) {
        AntExecutor.logger.info(message);
      }
    };
    logger.setErrorPrintStream(errorStream);
    logger.setOutputPrintStream(outputStream);
    logger.setMessageOutputLevel(Project.MSG_INFO);
    return logger;
  }
}
