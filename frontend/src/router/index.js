import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import AuthView from '../views/AuthView.vue'
import AdminView from '../views/AdminView.vue'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/auth', component: AuthView },
    { path: '/admin', component: AdminView, meta: { admin: true } }
  ]
})

router.beforeEach((to) => {
  const user = useUserStore()
  if (to.meta.admin && !user.isAdmin) {
    return '/'
  }
})

export default router
