package com.nepheletech.jred.console.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.ButtonElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.dom.client.TextAreaElement;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.nepheletech.jred.console.client.model.ActionResponse;
import com.nepheletech.jred.console.client.model.Workspace;
import com.nepheletech.jred.console.client.services.WorkspacesService;
import com.nepheletech.jred.console.client.views.Modal;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main implements EntryPoint {
  private static Logger logger = Logger.getLogger(Main.class.getName());

  private Workspace workspace = null;

  private Modal configModal = null;

  private Modal importWorkspaceModal = null;

  private Modal dockerModal = null;

  private Modal m2SettingsModal = null;

  public void onModuleLoad() {
    addEventListener("#createNewWorkspaceBtn", "click", new EventListener() {
      public void onBrowserEvent(Event event) {
        String workspace = Window.prompt("Workspace name", "workspace-name");
        if (workspace != null) {
          final ButtonElement btnElem = (ButtonElement) Document.get().getElementById("createNewWorkspaceBtn").cast();
          btnElem.setDisabled(true);
          final Element iconElem = Document.get().getElementById("createNewWorkspaceBtnIcon");
          Main.startSpinning(iconElem);
          WorkspacesService.get().create(workspace, new RequestCallback() {
            public void onResponseReceived(Request request, Response response) {
              ActionResponse actionResponse = (ActionResponse) JsonUtils.safeEval(response.getText()).cast();
              Main.this.updateTable(actionResponse.getItems());
              btnElem.setDisabled(false);
              Main.stopSpinning(iconElem);
              Window.alert(actionResponse.getMessage());
            }

            public void onError(Request request, Throwable exception) {
              Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
              btnElem.setDisabled(false);
              Main.stopSpinning(iconElem);
            }
          });
        }
      }
    });
    addEventListener("#refreshBtn", "click", new EventListener() {
      public void onBrowserEvent(Event event) {
        Main.this.deferRefresh();
      }
    });
    addEventListener("#configFormSelect", "change", new EventListener() {
      public void onBrowserEvent(Event event) {
        Main.this.loadConfig();
      }
    });
    addEventListener("#configFormBtn", "click", new EventListener() {
      public void onBrowserEvent(Event event) {
        SelectElement configFormSelect = (SelectElement) Document.get().getElementById("configFormSelect").cast();
        TextAreaElement configFormContentInput = (TextAreaElement) Document.get().getElementById("configFormContentInput").cast();
        WorkspacesService.get().applyConfig(Main.this.workspace, configFormSelect.getValue(), configFormContentInput
            .getValue(), new RequestCallback() {
              public void onResponseReceived(Request request, Response response) {
                Window.alert(response.getText());
                Main.this.deferRefresh();
              }

              public void onError(Request request, Throwable exception) {
                Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
              }
            });
        Main.this.configModal.hide();
      }
    });
    addEventListener("#importFlowsBtn", "click", new EventListener() {
      public void onBrowserEvent(Event event) {
        WorkspacesService.get().doImport(Main.this.workspace,
            (InputElement) Document.get().getElementById("importFlowsModalFile").cast());
        Main.this.importWorkspaceModal.hide();
      }
    });
    addEventListener("#dockerModalBtn", "click", new EventListener() {
      public void onBrowserEvent(Event event) {
        InputElement accountInput = (InputElement) Document.get().getElementById("dockerFormAccountInput").cast();
        InputElement passwordInput = (InputElement) Document.get().getElementById("dockerFormPasswordInput").cast();
        WorkspacesService.get().dockerPush(Main.this.workspace, accountInput
            .getValue(), passwordInput.getValue(), new RequestCallback() {
              public void onResponseReceived(Request request, Response response) {
                ActionResponse actionResponse = (ActionResponse) JsonUtils.safeEval(response.getText()).cast();
                Main.this.updateTable(actionResponse.getItems());
                Window.alert(actionResponse.getMessage());
              }

              public void onError(Request request, Throwable exception) {
                Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
              }
            });
        Main.this.dockerModal.hide();
      }
    });
    addEventListener("#maven-settings-xml-action", "click", new EventListener() {
      public void onBrowserEvent(Event event) {
        if (Main.this.m2SettingsModal == null) {
          Main.this.m2SettingsModal = Main.showModal(Document.get().getElementById("m2SettingsModal"), true, false, true);
        } else {
          Main.this.m2SettingsModal.show(true, false, true);
        }
        WorkspacesService.get().loadConfig("m2-settings", new RequestCallback() {
          public void onResponseReceived(Request request, Response response) {
            TextAreaElement textArea = (TextAreaElement) Document.get().getElementById("m2SettingsFormContentInput").cast();
            textArea.setValue(response.getText());
          }

          public void onError(Request request, Throwable exception) {
            Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
          }
        });
      }
    });
    addEventListener("#m2SettingsFormBtn", "click", new EventListener() {
      public void onBrowserEvent(Event event) {
        TextAreaElement textArea = (TextAreaElement) Document.get().getElementById("m2SettingsFormContentInput").cast();
        WorkspacesService.get().applyConfig("m2-settings", textArea.getValue(), new RequestCallback() {
          public void onResponseReceived(Request request, Response response) {
            Main.this.deferRefresh();
          }

          public void onError(Request request, Throwable exception) {
            Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
          }
        });
        Optional.<Modal>ofNullable(Main.this.m2SettingsModal).ifPresent(Modal::hide);
      }
    });
    deferRefresh();
  }

  protected void deferRefresh() {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      public void execute() {
        Main.this.refresh();
      }
    });
  }

  protected void refresh() {
    final Element spin = Document.get().getElementById("refreshBtnIcon");
    spin.addClassName("fa-spin");
    WorkspacesService.get().list(new RequestCallback() {
      public void onResponseReceived(Request request, Response response) {
        Main.logger.info(response.getText());
        Main.stopSpinning(spin);
        @SuppressWarnings("unchecked")
        JsArray<Workspace> data = (JsArray<Workspace>) JsonUtils.safeEval(response.getText()).cast();
        Main.this.updateTable(data);
      }

      public void onError(Request request, Throwable exception) {
        Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
        Main.stopSpinning(spin);
      }
    });
  }

  private static void startSpinning(final Element elem) {
    (new Timer() {
      public void run() {
        elem.addClassName("fa-spin");
      }
    }).schedule(1000);
  }

  private static void stopSpinning(final Element elem) {
    (new Timer() {
      public void run() {
        elem.removeClassName("fa-spin");
      }
    }).schedule(1000);
  }

  protected void updateTable(final JsArray<Workspace> data) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0, n = data.length(); i < n; i++) {
      Workspace workspace = (Workspace) data.get(i);
      sb.append("<tr>");
      if (workspace.isAvailable()) {
        sb.append("<th scope=\"row\" class=\"text-nowrap\">")
            .append("<img src=\"" + workspace.getName() + "/favicon.ico\" style=\"width:1.5rem;margin-right:.7rem;\">")
            .append("<a href=\"").append(workspace.getName()).append("\" class=\"link-secondary\" target=\"").append(workspace.getName()).append("\">")
            .append(workspace.getName())
            .append("</a>")
            .append("</th>");
      } else {
        sb.append("<th scope=\"row\" class=\"text-nowrap\">")
            .append("<img src=\"" + GWT.getHostPageBaseURL() + "/favicon.ico\" style=\"width:1.5rem;margin-right:.7rem;\">")
            .append("<span class=\"text-muted\">" + workspace.getName() + "</span>")
            .append("</th>");
      }
      sb.append("<td scope=\"row\" class=\"text-center text-nowrap\">");
      if (!workspace.isAvailable()) {
        sb.append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"start\" data-row=\"" + i + "\">")
            .append("<i class=\"fas fa-play\"></i>")
            .append("</a> ")
            .append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"reload\" data-row=\"" + i + "\">")
            .append("<i class=\"fas fa-redo\"></i>")
            .append("</a> ");
      } else {
        sb.append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"stop\" data-row=\"" + i + "\">")
            .append("<i class=\"fas fa-stop\"></i>")
            .append("</a> ")
            .append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"reload\" data-row=\"" + i + "\">")
            .append("<i class=\"fas fa-redo\"></i>")
            .append("</a> ");
      }
      sb.append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"clone\" data-row=\"" + i + "\">")
          .append("<i class=\"fas fa-clone\"></i>")
          .append("</a> ")

          .append("&nbsp;")

          .append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"export\" data-row=\"" + i + "\">")
          .append("<i class=\"fas fa-download\"></i>")
          .append("</a> ")

          .append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"import\" data-row=\"" + i + "\">")
          .append("<i class=\"fas fa-upload\"></i>")
          .append("</a> ")

          .append("&nbsp;")

          .append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"docker\" data-row=\"" + i + "\">")
          .append("<i class=\"fab fa-docker\"></i>")
          .append("</a> ")

          .append("<a class=\"btn btn-secondary btn-sm\" href=\"#\" role=\"button\" data-action=\"config\" data-row=\"" + i + "\">")
          .append("<i class=\"fas fa-cog\"></i>")
          .append("</a> ")

          .append("&nbsp;")

          .append("<a class=\"btn btn-danger btn-sm\" href=\"#\" role=\"button\" data-action=\"undeploy\" data-row=\"" + i + "\")>")
          .append("<i class=\"fas fa-trash-alt\"></i>")
          .append("</a>")

          .append("</td>")
          .append("</tr>");
    }
    Element tbody = Document.get().getElementById("workspacesTBody");
    tbody.setInnerHTML(sb.toString());
    addEventListener("#workspacesTBody a.btn[data-action]", "click", new EventListener() {
      public void onBrowserEvent(Event event) {
        Element elem = Element.as((JavaScriptObject) event.getCurrentEventTarget());
        int row = Integer.parseInt(elem.getAttribute("data-row"));
        String action = elem.getAttribute("data-action");
        if ("stop".equals(action)) {
          Main.this.stop((Workspace) data.get(row));
        } else if ("start".equals(action)) {
          Main.this.start((Workspace) data.get(row));
        } else if ("reload".equals(action)) {
          Main.this.reload((Workspace) data.get(row));
        } else if ("clone".equals(action)) {
          Main.this.cloneWorkspace((Workspace) data.get(row));
        } else if ("undeploy".equals(action)) {
          Main.this.undeploy((Workspace) data.get(row));
        } else if ("export".equals(action)) {
          Main.this.export((Workspace) data.get(row));
        } else if ("import".equals(action)) {
          Main.this.importWorkspace((Workspace) data.get(row));
        } else if ("config".equals(action)) {
          Main.this.loadConfig((Workspace) data.get(row));
        } else if ("docker".equals(action)) {
          Main.this.dockerConfig((Workspace) data.get(row));
        }
      }
    });
  }

  protected void stop(Workspace workspace) {
    WorkspacesService.get().stop(workspace, new RequestCallback() {
      public void onResponseReceived(Request request, Response response) {
        ActionResponse actionResponse = (ActionResponse) JsonUtils.safeEval(response.getText()).cast();
        Main.this.updateTable(actionResponse.getItems());
        Window.alert(actionResponse.getMessage());
      }

      public void onError(Request request, Throwable exception) {
        Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
      }
    });
  }

  protected void start(Workspace workspace) {
    WorkspacesService.get().start(workspace, new RequestCallback() {
      public void onResponseReceived(Request request, Response response) {
        ActionResponse actionResponse = (ActionResponse) JsonUtils.safeEval(response.getText()).cast();
        Main.this.updateTable(actionResponse.getItems());
        Window.alert(actionResponse.getMessage());
      }

      public void onError(Request request, Throwable exception) {
        Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
      }
    });
  }

  protected void reload(Workspace workspace) {
    WorkspacesService.get().reload(workspace, new RequestCallback() {
      public void onResponseReceived(Request request, Response response) {
        ActionResponse actionResponse = (ActionResponse) JsonUtils.safeEval(response.getText()).cast();
        Main.this.updateTable(actionResponse.getItems());
        Window.alert(actionResponse.getMessage());
      }

      public void onError(Request request, Throwable exception) {
        Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
      }
    });
  }

  protected void cloneWorkspace(Workspace workspace) {
    String workspaceName = Window.prompt("Workspace name", "workspace-name");
    if (workspaceName != null)
      WorkspacesService.get().cloneWorkspace(workspace, workspaceName, new RequestCallback() {
        public void onResponseReceived(Request request, Response response) {
          ActionResponse actionResponse = (ActionResponse) JsonUtils.safeEval(response.getText()).cast();
          Main.this.updateTable(actionResponse.getItems());
          Window.alert(actionResponse.getMessage());
        }

        public void onError(Request request, Throwable exception) {
          Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
        }
      });
  }

  protected void undeploy(Workspace workspace) {
    if (!Window.confirm("Undeploy " + workspace.getName() + "?"))
      return;
    WorkspacesService.get().undeploy(workspace, new RequestCallback() {
      public void onResponseReceived(Request request, Response response) {
        ActionResponse actionResponse = (ActionResponse) JsonUtils.safeEval(response.getText()).cast();
        Main.this.updateTable(actionResponse.getItems());
        Window.alert(actionResponse.getMessage());
      }

      public void onError(Request request, Throwable exception) {
        Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
      }
    });
  }

  protected void export(Workspace workspace) {
    WorkspacesService.get().export(workspace);
  }

  protected void importWorkspace(Workspace workspace) {
    this.workspace = workspace;
    if (this.importWorkspaceModal == null) {
      this.importWorkspaceModal = showModal(Document.get().getElementById("importFlowsModal"), true, false, true);
    } else {
      this.importWorkspaceModal.show(true, false, true);
    }
  }

  protected void loadConfig(Workspace workspace) {
    this.workspace = workspace;
    if (this.configModal == null) {
      this.configModal = showModal(Document.get().getElementById("configModal"), true, false, true);
    } else {
      this.configModal.show(true, false, true);
    }
    loadConfig();
  }

  protected void loadConfig() {
    SelectElement select = (SelectElement) Document.get().getElementById("configFormSelect").cast();
    WorkspacesService.get().loadConfig(this.workspace, select.getValue(), new RequestCallback() {
      public void onResponseReceived(Request request, Response response) {
        TextAreaElement textArea = (TextAreaElement) Document.get().getElementById("configFormContentInput").cast();
        textArea.setValue(response.getText());
      }

      public void onError(Request request, Throwable exception) {
        Main.logger.log(Level.SEVERE, String.valueOf(request), exception);
      }
    });
  }

  protected void dockerConfig(Workspace workspace) {
    this.workspace = workspace;
    if (this.dockerModal == null) {
      this.dockerModal = showModal(Document.get().getElementById("dockerModal"), true, false, true);
    } else {
      this.dockerModal.show(true, false, true);
    }
  }

  public static native void addEventListener(String selector, String events, EventListener listener) /*-{
		$wnd
				.jQuery(selector)
				.on(
						events,
						function(event) {
							listener.@com.google.gwt.user.client.EventListener::onBrowserEvent(Lcom/google/gwt/user/client/Event;)(event)
						});
  }-*/;

  public static native Modal showModal(Element e, boolean backdrop, boolean keyboard, boolean focus) /*-{
    var modal=new $wnd.bootstrap.Modal(e,{backdrop:backdrop,keyboard:keyboard,focus:focus});
    modal.show();
    return modal
  }-*/;
}
