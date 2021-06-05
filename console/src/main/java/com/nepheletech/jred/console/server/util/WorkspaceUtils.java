package com.nepheletech.jred.console.server.util;

import java.io.File;
import java.io.IOException;
import org.apache.catalina.Context;
import org.apache.commons.io.FileUtils;

public class WorkspaceUtils {
  public static void copyFiles(Context context, File templateDir, File workingDirectory) throws IOException {
    FileUtils.copyDirectory(templateDir, workingDirectory);
    if (context != null) {
      File srcPomXml = new File(context.getRealPath("/META-INF/maven/com.nepheletech/jred-editor-template/pom.xml"));
      if (srcPomXml.exists()) {
        File dstPomXml = new File(workingDirectory, "pom.xml");
        FileUtils.copyFile(srcPomXml, dstPomXml);
      } 
      File webXml = new File(context.getRealPath("/WEB-INF/web.xml"));
      if (webXml.exists()) {
        File _webXml = new File(workingDirectory, "/src/main/webapp/WEB-INF/web.xml");
        FileUtils.copyFile(webXml, _webXml);
      } 
      File contextXml = new File(context.getRealPath("/META-INF/context.xml"));
      if (contextXml.exists()) {
        File _contextXml = new File(workingDirectory, "/src/main/webapp/META-INF/context.xml");
        FileUtils.copyFile(contextXml, _contextXml);
      } 
      File flowsDir = new File(context.getRealPath("/WEB-INF/flows"));
      if (flowsDir.exists()) {
        File newFlowsDir = new File(workingDirectory, "/src/main/webapp/WEB-INF/flows");
        if (!newFlowsDir.exists())
          newFlowsDir.mkdirs(); 
        FileUtils.copyDirectory(flowsDir, newFlowsDir);
      } 
    } 
  }
}