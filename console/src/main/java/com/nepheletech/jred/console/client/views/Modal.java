package com.nepheletech.jred.console.client.views;

import com.google.gwt.core.client.JavaScriptObject;

public class Modal extends JavaScriptObject {
  
  protected Modal() {}
  
  public final native void show(boolean backdrop, boolean keyboard, boolean focus) /*-{
    this.show();
  }-*/;
  
  public final native void hide() /*-{
    this.hide();
  }-*/;
}