package com.nepheletech.jred.console.client.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class ActionResponse extends JavaScriptObject {
  
  protected ActionResponse() {}
  
  public final native String getMessage() /*-{
    return this.message;
  }-*/;
  
  public final native JsArray<Workspace> getItems() /*-{
    return this.items;
  }-*/;
}
