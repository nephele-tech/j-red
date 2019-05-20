/*
 * Copyright NepheleTech, http://www.nephelerech.com
 *
 * This file is part of J-RED project.
 *
 * J-RED is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * J-RED is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with J-RED.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.nepheletech.jred.editor;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepheletech.jred.runtime.FlowsRuntime;
import com.nepheletech.jred.runtime.storage.Storage;
import com.nepheletech.jton.jsonpath.NepheleJsonPath;
import com.nepheletech.jred.editor.nodes.NodeRegistry;
import com.nepheletech.jred.runtime.DefaultFlowsRuntime;
import com.nepheletech.jred.runtime.storage.LocalFileSystemStorage;
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
