package com.nepheletech.jred.runtime.nodes.api.servlet;

import javax.servlet.annotation.WebServlet;

import org.apache.camel.component.servlet.CamelHttpTransportServlet;

@WebServlet(name = "Http2InNodeServlet", asyncSupported = true, urlPatterns = { "/http2-in/*" })
public class Http2InNodeServlet extends CamelHttpTransportServlet {
  private static final long serialVersionUID = -3426828832336477805L;

}
