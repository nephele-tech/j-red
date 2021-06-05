package com.nepheletech.jred.console.server.util;

import java.io.File;
import java.util.List;
import java.util.Properties;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenUtils {
  private static final Logger logger = LoggerFactory.getLogger(MavenUtils.class);
  
  public static InvocationResult invokeMaven(File workingDirectory, File pomFile, Properties properties, List<String> goals) throws MavenInvocationException {
    logger.trace(">>> invokeMaven: workingDirectory={}, pomFile={}, properties={}, goals={}", new Object[] { workingDirectory, pomFile, properties, goals });
    DefaultInvocationRequest defaultInvocationRequest = new DefaultInvocationRequest();
    defaultInvocationRequest.setBatchMode(true);
    defaultInvocationRequest.setPomFile(pomFile);
    defaultInvocationRequest.setProperties(properties);
    defaultInvocationRequest.setGoals(goals);
    DefaultInvoker defaultInvoker = new DefaultInvoker();
    defaultInvoker.setWorkingDirectory(workingDirectory);
    return defaultInvoker.execute((InvocationRequest)defaultInvocationRequest);
  }
}