<template>
  <v-app light>
  <SideMenu :drawer="drawer"  :api_key="api_key" @selectsource="setResource" ></SideMenu><!--add this component in the template -->
<v-toolbar fixed app light clipped-left color="primary" class="elevation-2">
    <v-toolbar-side-icon @click="drawer = !drawer"   class="white--text"></v-toolbar-side-icon>
    <v-toolbar-title class="white--text">News App</v-toolbar-title>
  </v-toolbar>
  <v-content>
    <v-container fluid>
      <MainContent :articles="articles"></MainContent>
    </v-container>
   </v-content>
   <v-footer class="secondary" app>
      <v-layout row wrap align-center>
        <v-flex xs12>
          <div class="white--text ml-3">
            Made with
            <v-icon class="red--text">favorite</v-icon>
            by <a class="white--text" href="https://vuetifyjs.com" target="_blank">Vuetify</a>
            and <a class="white--text" href="https://github.com/rachidsakara" target="_blank">Rachid Sakara</a>
          </div>
        </v-flex>
      </v-layout>
    </v-footer>
  </v-app>
</template>
<script>
import axios from 'axios'
import MainContent from './components/MainContent.vue'
import SideMenu from './components/SideMenu.vue' // import the SideMenu component
export default {
      components: {
        MainContent,
        SideMenu // Register the component
      },
    data () {
      return {
       drawer: false,
       api_key:'6bd6c9a75fdb47b888def4eaec38d6c2',
       articles: [],
       errors: []
      }
    },
    created () {
      axios.get('https://newsapi.org/v2/top-headlines?sources=techcrunch&apiKey='+this.api_key)
        .then(response => {
          this.articles = response.data.articles
          console.log('data:')
          console.log(response.data.articles) // This will give you access to the full object
        })
        .catch(e => {
          this.errors.push(e)
        })
     },
     //add the methodes events hadler with setResource() function
      methods: {
            setResource(source){             axios.get('https://newsapi.org/v2/top-headlines?sources='+source+'&apiKey='+this.api_key)
              .then(response => {
                this.articles = response.data.articles
                console.log(response.data)
              })
              .catch(e => {
                this.errors.push(e)
              })

            }
         }
  }
</script>
