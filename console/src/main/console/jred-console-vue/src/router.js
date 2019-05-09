import Vue from 'vue';
import VueRouter from 'vue-router';
import Workspaces from './views/Workspaces.vue';

Vue.use(VueRouter);

export default new VueRouter({
	mode: 'history',
	//base: process.env.BASE_URL,
	routes: [{
		path: '/workspaces',
		name: 'Workspaces',
		icon: 'dashboard',
		component: Workspaces
	}, {
		path: '*',
		redirect: '/workspaces'
	}]
})