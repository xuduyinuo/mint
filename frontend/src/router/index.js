import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: () => import('../views/HomeView.vue') },
    { path: '/auth', component: () => import('../views/AuthView.vue') },
    { path: '/creator', component: () => import('../views/CreatorView.vue'), meta: { auth: true } },
    { path: '/creator/blogs', component: () => import('../views/BlogEditorView.vue'), meta: { auth: true } },
    { path: '/creator/images', component: () => import('../views/ImageLibraryView.vue'), meta: { auth: true } },
    { path: '/admin', component: () => import('../views/AdminView.vue'), meta: { admin: true } }
  ]
})

router.beforeEach((to) => {
  const user = useUserStore()
  if (to.meta.auth && !user.loggedIn) {
    return '/auth'
  }
  if (to.meta.admin && !user.isAdmin) {
    return '/'
  }
})

export default router
