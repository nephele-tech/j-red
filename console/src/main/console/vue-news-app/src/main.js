// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from './App'
import Vuetify from 'vuetify'
import 'vuetify/dist/vuetify.min.css'
// add this two import statements
import AOS from "aos"
import "aos/dist/aos.css"

Vue.use(Vuetify)

Vue.config.productionTip = false

/* eslint-disable no-new */
new Vue({
  el: '#app',
  created: function() {
    AOS.init({ disable: "phone" });
  },
  components: { App },
  template: '<App/>'
})
