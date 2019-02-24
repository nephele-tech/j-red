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
        value: 'isDeployed'
      }, {
        text: 'Sessions',
        value: 'activeSessions'
      }, {
        text: 'Commands'
      }],
      items: [],
      loading: true
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
    getItems: function(callback) { 
      axios.get('./manager/list')
        .then(function(response) {
          callback(response.data);
        })
        .catch(function(error) {
          console.error(error);
        })
        .then(function() {
            
        });
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