package com.nepheletech.jred.console.client.services;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.user.client.Window;
import com.nepheletech.jred.console.client.model.Workspace;

public class WorkspacesService {
  private static final String BASE_URL = GWT.getHostPageBaseURL();

  private static final String MANAGER_SERVLET_LIST = BASE_URL + "manager/list";

  private static final String MANAGER_SERVLET_STOP = BASE_URL + "manager/stop";

  private static final String MANAGER_SERVLET_START = BASE_URL + "manager/start";

  private static final String MANAGER_SERVLET_RELOAD = BASE_URL + "manager/reload";

  private static final String MANAGER_SERVLET_CLONE = BASE_URL + "manager/clone";

  private static final String MANAGER_SERVLET_UNDEPLOY = BASE_URL + "manager/undeploy";

  private static final String MANAGER_SERVLET_DEPLOY = BASE_URL + "manager/deploy";

  private static final String MANAGER_SERVLET_EXPORT = BASE_URL + "manager/export";

  private static final String MANAGER_SERVLET_IMPORT = BASE_URL + "manager/import";

  private static final String MANAGER_SERVLET_CONFIG = BASE_URL + "manager/config";

  private static final String MANAGER_SERVLET_DOCKER = BASE_URL + "manager/docker";

  private static WorkspacesService workspacesService = null;

  public static WorkspacesService get() {
    if (workspacesService == null)
      workspacesService = new WorkspacesService();
    return workspacesService;
  }

  public void list(RequestCallback callback) {
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, MANAGER_SERVLET_LIST);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void create(String workspace, RequestCallback callback) {
    String url = MANAGER_SERVLET_DEPLOY + "?workspace=" + workspace;
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void stop(Workspace workspace, RequestCallback callback) {
    String url = MANAGER_SERVLET_STOP + "?path=" + workspace.getName();
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void start(Workspace workspace, RequestCallback callback) {
    String url = MANAGER_SERVLET_START + "?path=" + workspace.getName();
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void reload(Workspace workspace, RequestCallback callback) {
    String url = MANAGER_SERVLET_RELOAD + "?path=" + workspace.getName();
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void cloneWorkspace(Workspace workspace, String name, RequestCallback callback) {
    String url = MANAGER_SERVLET_CLONE + "?path=" + workspace.getName() + "&workspace=" + name;
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void undeploy(Workspace workspace, RequestCallback callback) {
    String url = MANAGER_SERVLET_UNDEPLOY + "?path=" + workspace.getName();
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void export(Workspace workspace) {
    String url = MANAGER_SERVLET_EXPORT + "?path=" + workspace.getName();
    Window.Location.replace(url);
  }

  public void loadConfig(String config, RequestCallback callback) {
    String url = MANAGER_SERVLET_CONFIG + "?config=" + config;
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void loadConfig(Workspace workspace, String config, RequestCallback callback) {
    String url = MANAGER_SERVLET_CONFIG + "?path=" + workspace.getName() + "&config=" + config;
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void doImport(Workspace workspace, InputElement fileInputElem) {
    String url = MANAGER_SERVLET_IMPORT + "?path=" + workspace.getName();
    doImportImpl(url, fileInputElem);
  }

  private final native void doImportImpl(String url, InputElement fileInputElem) /*-{
		var e = fileInputElem.files;
		var f = new FormData;
		$wnd.jQuery.each(e, function(a, b) {
			f.append(a, b)
		});
		$wnd.console.log(f);
		$wnd.jQuery.ajax({
			headers : {
				'Accept' : 'application/json'
			},
			type : 'POST',
			data : f,
			url : url,
			dataType : 'json',
			processData : false,
			contentType : false,
			success : function(a) {
				$wnd.console.log(a);
				setTimeout(function() {
					$wnd.alert(a.message)
				}, 0)
			},
			error : function(a) {
				$wnd.console.error(a)
			}
		})
  }-*/;

  public void applyConfig(String config, String content, RequestCallback callback) {
    String url = MANAGER_SERVLET_CONFIG + "?config=" + config;
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setHeader("Content-Type", "text/plain");
    requestBuilder.setRequestData(content);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void applyConfig(Workspace workspace, String config, String content, RequestCallback callback) {
    String url = MANAGER_SERVLET_CONFIG + "?path=" + workspace.getName() + "&config=" + config;
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setHeader("Content-Type", "text/plain");
    requestBuilder.setRequestData(content);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }

  public void dockerPush(Workspace workspace, String account, String password, RequestCallback callback) {
    String url = MANAGER_SERVLET_DOCKER + "?path=" + workspace.getName() + "&account=" + account;
    RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
    requestBuilder.setHeader("Content-Type", "text/plain");
    requestBuilder.setRequestData(password);
    requestBuilder.setCallback(callback);
    try {
      requestBuilder.send();
    } catch (RequestException e) {
      callback.onError(null, (Throwable) e);
    }
  }
}
