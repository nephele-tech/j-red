package com.nepheletech.jred.console.client.model;

import com.google.gwt.core.client.JavaScriptObject;

public class Workspace extends JavaScriptObject {
  
  protected Workspace() {}
  
  public final native String getName() /*-{
    return this.name;
  }-*/;
  
  public final native String getVersion() /*-{
    return this.version;
  }-*/;
  
  public final native String getWebAppVersion() /*-{
    return this.webAppVersion;
  }-*/;
  
  public final native boolean isAvailable() /*-{
    return this.isAvailable;
  }-*/;
  
  public final native String getPathVersion() /*-{
    return this.pathVersion;
  }-*/;
  
  public final native int getActiveSessions() /*-{
    return this.activeSessions;
  }-*/;
  
  public final native boolean isDeployed() /*-{
    return this.isDeployed;
  }-*/;
}