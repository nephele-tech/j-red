<template>
  <v-app light>
    <AppNavigation></AppNavigation>
    <v-content transition="slide-x-transition">
      <router-view></router-view>
    </v-content>
    <v-footer class="pa-3">
      <v-spacer></v-spacer>
      <div>&copy;{{ new Date().getFullYear() }} <a href="http://www.nepheletech.com" target="nephele-tech-window">NepheleTech Limited</a></div>
    </v-footer>
  </v-app>
</template>

<script>
import AppNavigation from '@/components/AppNavigation'

export default {
  name: 'App',
  components: {
    AppNavigation,
  }
}
</script>

<style>
</style>
