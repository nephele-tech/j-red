package com.nepheletech.jred.editor;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jred.runtime.storage.Storage;
import com.nepheletech.jred.editor.nodes.NodeRegistry;
import com.nepheletech.jred.runtime.DefaultFlowsRuntime;
import com.nepheletech.jred.runtime.storage.LocalFileSystemStorage;
import com.nepheletech.json.jsonpath.NepheleJsonPath;
import com.nepheletech.messagebus.MessageBus;

@WebListener
public class ContextListener implements ServletContextListener {
  private static final Logger logger = LoggerFactory.getLogger(ContextListener.class);

  static {
    NepheleJsonPath.configure();
  }

  public void contextInitialized(ServletContextEvent sce) {
    final ServletContext servletContext = sce.getServletContext();
    logger.info(">>> Context initialized... {}", servletContext.getContextPath());

    servletContext.setAttribute(NodeRegistry.class.getName(), new NodeRegistry());

    final String baseDir = servletContext.getRealPath("WEB-INF/flows");
    final Storage storage = new LocalFileSystemStorage(baseDir);
    final FlowsRuntime flowsRuntime = new DefaultFlowsRuntime(storage);
    servletContext.setAttribute(FlowsRuntime.class.getName(), flowsRuntime);
    flowsRuntime.startFlows();
  }

  public void contextDestroyed(ServletContextEvent sce) {
    final ServletContext servletContext = sce.getServletContext();
    logger.info(">>> Context destroyed... {}", servletContext.getContextPath());

    final FlowsRuntime flowsRuntime = (FlowsRuntime) servletContext.getAttribute(FlowsRuntime.class.getName());
    if (flowsRuntime != null) {
      flowsRuntime.stopFlows();
    }

    // Reset message bus...
    MessageBus.unsubscribeAll();
  }
}
