<template>
  <main class="auth-page">
    <section class="auth-panel">
      <div>
        <h1>进入 Mint Search</h1>
        <p>登录后会记录搜索、点击等行为，生成兴趣画像并驱动个性化推荐。</p>
      </div>
      <el-tabs v-model="mode">
        <el-tab-pane label="登录" name="login">
          <el-form :model="form" label-position="top">
            <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="form.password" show-password /></el-form-item>
            <el-button type="primary" class="full" @click="submit">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="form" label-position="top">
            <el-form-item label="账号"><el-input v-model="form.username" /></el-form-item>
            <el-form-item label="昵称"><el-input v-model="form.nickname" /></el-form-item>
            <el-form-item label="密码"><el-input v-model="form.password" show-password /></el-form-item>
            <el-button type="primary" class="full" @click="submit">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
      <el-button text @click="$router.push('/')">返回搜索首页</el-button>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const user = useUserStore()
const mode = ref('login')
const form = reactive({ username: 'user', password: 'user123456', nickname: '' })

async function submit() {
  try {
    if (mode.value === 'login') {
      await user.login(form)
    } else {
      await user.register(form)
    }
    ElMessage.success('已登录')
    router.push(user.isAdmin ? '/admin' : '/')
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>
