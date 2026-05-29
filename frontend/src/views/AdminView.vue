<template>
  <main class="admin-layout">
    <aside>
      <div class="brand">Mint Admin</div>
      <el-menu v-model="active" @select="active = $event">
        <el-menu-item index="stats">统计概览</el-menu-item>
        <el-menu-item index="users">用户画像</el-menu-item>
        <el-menu-item index="sources">搜索源</el-menu-item>
        <el-menu-item index="hot">热点推荐</el-menu-item>
        <el-menu-item index="logs">搜索日志</el-menu-item>
      </el-menu>
      <el-button text @click="$router.push('/')">返回首页</el-button>
    </aside>
    <section class="admin-main">
      <header>
        <h1>{{ title }}</h1>
        <el-button v-if="active === 'sources'" type="primary" @click="createSource">新增搜索源</el-button>
        <el-button v-if="active === 'hot'" type="primary" @click="createHot">新增热点</el-button>
      </header>

      <div v-if="active === 'stats'" class="stat-grid">
        <div v-for="(value, key) in stats" :key="key" class="stat-card">
          <span>{{ statLabel(key) }}</span>
          <b>{{ value }}</b>
        </div>
      </div>

      <el-table v-else :data="tableData" stripe height="calc(100vh - 170px)">
        <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" min-width="130" show-overflow-tooltip />
        <el-table-column v-if="active === 'sources' || active === 'hot'" label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" text @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'

const active = ref('stats')
const stats = ref({})
const tableData = ref([])

const title = computed(() => ({
  stats: '统计概览',
  users: '用户与画像',
  sources: '搜索源配置',
  hot: '热点推荐',
  logs: '搜索日志'
}[active.value]))

const columns = computed(() => ({
  users: [
    { prop: 'id', label: 'ID' }, { prop: 'username', label: '账号' }, { prop: 'nickname', label: '昵称' }, { prop: 'role', label: '角色' }, { prop: 'status', label: '状态' }
  ],
  sources: [
    { prop: 'id', label: 'ID' }, { prop: 'name', label: '来源' }, { prop: 'type', label: '类型' }, { prop: 'enabled', label: '启用' }, { prop: 'weight', label: '权重' }, { prop: 'authorityScore', label: '权威分' }
  ],
  hot: [
    { prop: 'id', label: 'ID' }, { prop: 'type', label: '类型' }, { prop: 'title', label: '标题' }, { prop: 'heatScore', label: '热度' }, { prop: 'enabled', label: '启用' }
  ],
  logs: [
    { prop: 'id', label: 'ID' }, { prop: 'userId', label: '用户' }, { prop: 'keyword', label: '关键词' }, { prop: 'type', label: '类型' }, { prop: 'resultCount', label: '结果数' }, { prop: 'durationMs', label: '耗时ms' }
  ]
}[active.value] || []))

onMounted(load)
watch(active, load)

async function load() {
  if (active.value === 'stats') {
    stats.value = await http.get('/api/admin/stats')
  } else {
    tableData.value = await http.get(`/api/admin/${active.value}`)
  }
}

async function createSource() {
  await http.post('/api/admin/sources', {
    name: `自定义来源 ${Date.now().toString().slice(-4)}`,
    type: 'news',
    enabled: 1,
    weight: 1,
    authorityScore: 0.7,
    configJson: '{}'
  })
  ElMessage.success('已新增搜索源')
  load()
}

async function createHot() {
  await http.post('/api/admin/hot', {
    type: 'news',
    title: `运营热点 ${Date.now().toString().slice(-4)}`,
    summary: '管理员维护的未登录热点推荐内容。',
    url: 'https://example.com',
    heatScore: 70,
    enabled: 1
  })
  ElMessage.success('已新增热点')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm('确认逻辑删除这条记录？', '提示')
  await http.delete(`/api/admin/${active.value}/${row.id}`)
  ElMessage.success('已删除')
  load()
}

function statLabel(key) {
  return {
    userCount: '用户数',
    sourceCount: '搜索源',
    hotCount: '热点内容',
    searchCount: '搜索次数',
    behaviorCount: '行为事件',
    enabledSources: '启用来源'
  }[key] || key
}
</script>
