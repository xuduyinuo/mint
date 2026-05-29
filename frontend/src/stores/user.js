import { defineStore } from 'pinia'
import http from '../api/http'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('mint_token') || '',
    user: JSON.parse(localStorage.getItem('mint_user') || 'null')
  }),
  getters: {
    loggedIn: (state) => Boolean(state.token),
    isAdmin: (state) => state.user?.role === 'ADMIN'
  },
  actions: {
    async login(form) {
      const data = await http.post('/api/auth/login', form)
      this.setAuth(data)
    },
    async register(form) {
      const data = await http.post('/api/auth/register', form)
      this.setAuth(data)
    },
    setAuth(data) {
      this.token = data.token
      this.user = data
      localStorage.setItem('mint_token', data.token)
      localStorage.setItem('mint_user', JSON.stringify(data))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('mint_token')
      localStorage.removeItem('mint_user')
    }
  }
})
