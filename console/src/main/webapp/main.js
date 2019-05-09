Vue.use(VueRouter)

let workspaces = {
  template:`#workspaces`,
  data() {
    return {
      breadcrumbs: [{
        text: 'J-RED Console',
        disabled: false,
        href: '#/home' 
      }, {
        text: 'Workspaces',
        disabled: true,
        href: '#/workspaces'
      }],
      search: '',
      headers: [{
        text: 'Path',
        align: 'left',
        sortable: true,
        value: 'displayPath'
      }, {
        text: 'Version',
        align: 'left',
        sortable: true,
        value: 'version'
      }, { 
        text: 'Running',
        align: 'center',
        sortable: false,
        value: 'isDeployed'
      }, {
        text: 'Sessions',
        align: 'right',
        sortable: true,
        value: 'activeSessions'
      }, {
        text: 'Actions',
        align: 'center',
        sortable: false
      }],
      items: [],
      loading: true,
      dialog: false
    };
  },
  mounted() {
    var _this = this;
    this.getItems(function(items){
      _this.items = items;
      _this.loading = false;
    });
  },
  methods: {
    refresh: function() {
      var _this = this;
      this.getItems(function(items){
        _this.items = items;
        _this.loading = false;
      });
    },
    getItems: function(callback) { 
      axios.get('./manager/list')
        .then(function(response) {
          console.log(response.data);
          callback(response.data);
        })
        .catch(function(error) {
          console.error(error);
        })
        .then(function() {
            
        });
    },
    doStart: function(item) {
      var _this = this;
      
      axios.post('./manager/start?'+item.pathVersion)
        .then(function(response) {
          alert(response.data.message);

          _this.items = response.data.items;
          _this.loading = false;
        })
        .catch(function(error) {
          console.error(error);
        })
        .then(function() {

        });
    },
    doStop: function(item) {
      var _this = this;
      
      axios.post('./manager/stop?'+item.pathVersion)
        .then(function(response) {
          alert(response.data.message);

          _this.items = response.data.items;
          _this.loading = false;
        })
        .catch(function(error) {
          console.error(error);
        })
        .then(function() {

        });
    },
    doExport: function(item) {
      window.open('./manager/export?'+item.pathVersion, '_blank');
    },
    doImport: function(item) {
      his.dialog = true;
    },
    doReload: function(item) {
      var _this = this;

      axios.post('./manager/reload?'+item.pathVersion)
        .then(function(response) {
          alert(response.data.message);

          _this.items = response.data.items;
          _this.loading = false;
        })
        .catch(function(error) {
          console.error(error);
        })
        .then(function() {

        });
    },
    doClone: function(item) {
	  var _this = this;
	    
	  var workspace = prompt("Workspace name:", "/workspace")
	  if (workspace) {
	    // TODO validate workspace name
	    
	    axios.post('./manager/clone?'+item.pathVersion+'&workspace='+encodeURIComponent(workspace))
	      .then(function(response) {
	        alert(response.data.message);
	      })
	      .catch(function(error) {
	        console.error(error);
	      })
	      .then(function() {
	        _this.getItems(function(items){
	          _this.items = items;
	          _this.loading = false;
	        });
	      });
	  }
    },
    doSettings: function(item) {
    	
    },
    doUndeploy: function(item) {
      var _this = this;

      axios.post('./manager/undeploy?'+item.pathVersion)
        .then(function(response) {
          alert(response.data.message);
        })
        .catch(function(error) {
          console.error(error);
        })
        .then(function() {
          _this.getItems(function(items){
            _this.items = items;
            _this.loading = false;
          });
        });
    },
    doDeploy: function() {
      var _this = this;
      
      var workspace = prompt("Workspace name:", "/workspace")
      if (workspace) {
        // TODO validate workspace name
        
        axios.post('./manager/deploy?workspace='+encodeURIComponent(workspace))
          .then(function(response) {
            alert(response.data.message);
          })
          .catch(function(error) {
            console.error(error);
          })
          .then(function() {
            _this.getItems(function(items){
              _this.items = items;
              _this.loading = false;
            });
          });
      }
    }
  }
}

let component2 = {
  template:`<div class="title">Page 2</div>`
}
let component3 = {
  template:`<div class="title"><iframe style="position:absolute; top:0; left:0; width:100%; height:100%; border: none;" src="http://www.apache.org"></iframe></div>`
}

let router = new VueRouter({
  routes: [
    {
      path: '/workspaces',
      name: 'Workspaces',
      icon: 'dashboard',
      component: workspaces,
    }/*,
    {
      path: '/page2',
      name: 'Page 2',
      icon: 'home',
      component: component2,
    },
    {
      path: '/page3',
      name: 'Page Three',
      icon: 'event',
      component: component3,
    }*/,
    { path: '*', redirect: '/workspaces' }
  ]
})

new Vue({
  el: '#app',
  router,
  data() {
    return {
      routes: router.options.routes,
      drawer: false
    }
  }
})