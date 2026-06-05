<template>
  <main class="admin-layout">
    <aside>
      <div class="brand">Mint Admin</div>
      <el-menu v-model="active" @select="active = $event">
        <el-menu-item index="stats">统计概览</el-menu-item>
        <el-menu-item index="users">用户画像</el-menu-item>
        <el-menu-item index="sources">搜索源</el-menu-item>
        <el-menu-item index="content">原创内容</el-menu-item>
        <el-menu-item index="hot">热点推荐</el-menu-item>
        <el-menu-item index="ranking">排序策略</el-menu-item>
        <el-menu-item index="logs">搜索日志</el-menu-item>
      </el-menu>
      <el-button text @click="$router.push('/')">返回首页</el-button>
    </aside>

    <section class="admin-main">
      <header>
        <div>
          <h1>{{ title }}</h1>
          <p>{{ subtitle }}</p>
        </div>
        <div class="admin-actions">
          <el-button :loading="loading" @click="load">刷新</el-button>
          <el-button v-if="canCreate" type="primary" @click="openCreate">新增</el-button>
        </div>
      </header>

      <div v-if="active === 'stats'" class="stat-grid">
        <div v-for="item in statItems" :key="item.key" class="stat-card">
          <span>{{ item.label }}</span>
          <b>{{ item.value }}</b>
        </div>
      </div>

      <div v-else-if="active === 'content'" v-loading="loading" class="admin-content-panel">
        <el-tabs v-model="contentTab">
          <el-tab-pane :label="`博客文章（${contentData.blogs.length}）`" name="blogs">
            <el-table :data="contentData.blogs" stripe height="calc(100vh - 250px)">
              <el-table-column prop="id" label="ID" width="80" />
              <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
              <el-table-column prop="status" label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.blocked === 1 ? 'danger' : row.status === 'PUBLISHED' ? 'success' : 'info'">
                    {{ row.blocked === 1 ? '已封禁' : row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="nickname" label="作者" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ authorName(row) }}</template>
              </el-table-column>
              <el-table-column prop="summary" label="摘要" min-width="260" show-overflow-tooltip />
              <el-table-column prop="tags" label="标签" min-width="160" show-overflow-tooltip />
              <el-table-column prop="updateTime" label="更新时间" width="180" />
              <el-table-column label="操作" width="220" fixed="right" align="center">
                <template #default="{ row }">
                  <div class="admin-row-actions">
                    <el-button link type="primary" @click="previewAdminBlog(row)">查看</el-button>
                    <el-button link type="warning" @click="toggleContentBlocked('blogs', row)">
                      {{ row.blocked === 1 ? '解封' : '封禁' }}
                    </el-button>
                    <el-button link type="danger" @click="removeContent('blogs', row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
          <el-tab-pane :label="`图片资源（${contentData.images.length}）`" name="images">
            <el-table :data="contentData.images" stripe height="calc(100vh - 250px)">
              <el-table-column label="预览" width="112">
                <template #default="{ row }">
                  <img :src="row.url" :alt="row.fileName" class="admin-image-thumb" @click="previewAdminImage(row)" />
                </template>
              </el-table-column>
              <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
              <el-table-column prop="nickname" label="上传者" min-width="140" show-overflow-tooltip>
                <template #default="{ row }">{{ authorName(row) }}</template>
              </el-table-column>
              <el-table-column prop="blocked" label="状态" width="110">
                <template #default="{ row }">
                  <el-tag :type="row.blocked === 1 ? 'danger' : 'success'">
                    {{ row.blocked === 1 ? '已封禁' : '正常' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="tags" label="标签" min-width="160" show-overflow-tooltip />
              <el-table-column prop="contentType" label="格式" width="130" />
              <el-table-column prop="fileSize" label="大小" width="120">
                <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column prop="createTime" label="上传时间" width="180" />
              <el-table-column label="操作" width="220" fixed="right" align="center">
                <template #default="{ row }">
                  <div class="admin-row-actions">
                    <el-button link type="primary" @click="previewAdminImage(row)">查看</el-button>
                    <el-button link type="warning" @click="toggleContentBlocked('images', row)">
                      {{ row.blocked === 1 ? '解封' : '封禁' }}
                    </el-button>
                    <el-button link type="danger" @click="removeContent('images', row)">删除</el-button>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </div>

      <el-table v-else v-loading="loading" :data="tableData" stripe height="calc(100vh - 190px)">
        <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :min-width="col.width || 130" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="col.kind === 'status'" :type="row[col.prop] === 1 ? 'success' : 'info'">
              {{ row[col.prop] === 1 ? '启用' : '停用' }}
            </el-tag>
            <el-tag v-else-if="col.kind === 'role'" :type="row[col.prop] === 'ADMIN' ? 'danger' : 'primary'">
              {{ row[col.prop] }}
            </el-tag>
            <span v-else>{{ formatCell(row[col.prop]) }}</span>
          </template>
        </el-table-column>

        <el-table-column v-if="hasActions" label="操作" width="180" fixed="right" align="center">
          <template #default="{ row }">
            <div class="admin-row-actions">
              <el-button v-if="active !== 'logs'" link type="primary" @click="openEdit(row)">编辑</el-button>
              <el-button v-if="canToggle" link type="info" @click="toggleEnabled(row)">
                {{ row.enabled === 1 || row.status === 1 ? '停用' : '启用' }}
              </el-button>
              <el-button v-if="canDelete" link type="danger" @click="remove(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="active === 'logs'" class="admin-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :current-page="logPage"
          :page-size="logPageSize"
          :page-sizes="logPageSizes"
          :total="logTotal"
          @current-change="handleLogPageChange"
          @size-change="handleLogPageSizeChange"
        />
      </div>
    </section>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px">
      <el-form :model="form" label-width="92px">
        <template v-if="active === 'users'">
          <el-form-item label="账号">
            <el-input v-model="form.username" disabled />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="form.role">
              <el-option label="管理员" value="ADMIN" />
              <el-option label="普通用户" value="USER" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </template>

        <template v-else-if="active === 'sources'">
          <el-form-item label="提供方" required>
            <el-select v-model="form.provider" @change="applySourcePreset">
              <el-option v-for="preset in sourcePresets" :key="preset.provider" :label="preset.label" :value="preset.provider" />
            </el-select>
          </el-form-item>
          <el-form-item label="来源名称" required>
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="类型" required>
            <el-select v-model="form.type">
              <el-option label="新闻" value="news" />
              <el-option label="图片" value="image" />
              <el-option label="视频" value="video" />
              <el-option label="文档" value="document" />
            </el-select>
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="权重">
            <el-input-number v-model="form.weight" :min="0" :max="10" :step="0.1" />
          </el-form-item>
          <el-form-item label="权威分">
            <el-input-number v-model="form.authorityScore" :min="0" :max="1" :step="0.05" />
          </el-form-item>
          <el-form-item label="端点">
            <el-input v-model="form.endpoint" />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="form.apiKey" type="password" show-password placeholder="留空则使用环境变量或无密钥来源" />
          </el-form-item>
          <el-form-item label="说明">
            <el-input v-model="form.notes" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="配置 JSON">
            <el-input v-model="form.configJson" type="textarea" :rows="4" />
          </el-form-item>
        </template>

        <template v-else-if="active === 'hot'">
          <el-form-item label="类型" required>
            <el-select v-model="form.type">
              <el-option label="新闻" value="news" />
              <el-option label="图片" value="image" />
              <el-option label="视频" value="video" />
            </el-select>
          </el-form-item>
          <el-form-item label="标题" required>
            <el-input v-model="form.title" />
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="form.summary" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="链接">
            <el-input v-model="form.url" />
          </el-form-item>
          <el-form-item label="缩略图">
            <el-input v-model="form.thumbnailUrl" />
          </el-form-item>
          <el-form-item label="热度">
            <el-input-number v-model="form.heatScore" :min="0" :max="100" :step="1" />
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </template>

        <template v-else-if="active === 'ranking'">
          <el-form-item label="名称" required>
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="相关性">
            <el-input-number v-model="form.relevanceWeight" :min="0" :max="1" :step="0.05" />
          </el-form-item>
          <el-form-item label="时效性">
            <el-input-number v-model="form.freshnessWeight" :min="0" :max="1" :step="0.05" />
          </el-form-item>
          <el-form-item label="权威性">
            <el-input-number v-model="form.authorityWeight" :min="0" :max="1" :step="0.05" />
          </el-form-item>
          <el-form-item label="偏好">
            <el-input-number v-model="form.preferenceWeight" :min="0" :max="1" :step="0.05" />
          </el-form-item>
          <el-form-item label="权重合计">
            <el-tag :type="rankingTotalOk ? 'success' : 'danger'">{{ rankingTotal.toFixed(2) }}</el-tag>
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </template>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="adminPreviewVisible" :title="adminPreviewName" class="image-preview-dialog" width="fit-content">
      <img v-if="adminPreviewUrl" :src="adminPreviewUrl" :alt="adminPreviewName" class="preview-large-image" />
    </el-dialog>

    <el-dialog v-model="adminBlogPreviewVisible" width="min(860px, 94vw)" class="admin-blog-preview-dialog">
      <template #header>
        <div class="admin-blog-preview-header">
          <el-tag :type="adminBlogPreview?.blocked === 1 ? 'danger' : adminBlogPreview?.status === 'PUBLISHED' ? 'success' : 'info'">
            {{ adminBlogPreview?.blocked === 1 ? '已封禁' : adminBlogPreview?.status === 'PUBLISHED' ? '已发布' : '草稿' }}
          </el-tag>
          <h2>{{ adminBlogPreview?.title }}</h2>
          <p>{{ adminBlogPreview?.summary || '暂无摘要' }}</p>
        </div>
      </template>
      <MdPreview
        v-if="adminBlogPreview"
        :model-value="adminBlogPreview.content || ''"
        editor-id="admin-blog-preview"
        preview-theme="github"
        code-theme="atom"
        class="mint-md-preview"
      />
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import http from '../api/http'

const active = ref('stats')
const loading = ref(false)
const saving = ref(false)
const stats = ref({})
const tableData = ref([])
const contentTab = ref('blogs')
const contentData = ref({ blogs: [], images: [] })
const logPage = ref(1)
const logPageSize = ref(20)
const logTotal = ref(0)
const logPageSizes = [10, 20, 50, 100]
const dialogVisible = ref(false)
const adminPreviewVisible = ref(false)
const adminPreviewUrl = ref('')
const adminPreviewName = ref('')
const adminBlogPreviewVisible = ref(false)
const adminBlogPreview = ref(null)
const editingId = ref(null)
const form = ref({})

const meta = {
  stats: { title: '统计概览', subtitle: '系统数据、内容和行为规模概览。' },
  users: { title: '用户与画像', subtitle: '维护账号状态、角色，并查看画像与行为数量。' },
  sources: { title: '搜索源配置', subtitle: '配置搜索来源、权重、权威分和启停状态。' },
  content: { title: '原创内容管理', subtitle: '管理员集中查看并管理平台原创博客和图片资源。' },
  hot: { title: '热点推荐', subtitle: '维护未登录与冷启动场景下展示的热点内容。' },
  ranking: { title: '排序策略', subtitle: '调整搜索排序的相关性、时效性、权威性和偏好权重。' },
  logs: { title: '搜索日志', subtitle: '查看搜索关键词、结果数量、耗时和来源分布。' }
}

const sourcePresets = [
  {
    provider: 'tencent_news',
    label: '腾讯新闻',
    name: '腾讯新闻',
    type: 'news',
    endpoint: 'https://i.news.qq.com/gw/pc_search/result',
    requiresApiKey: false,
    notes: '新闻搜索主来源，今日热点会优先读取腾讯新闻热点榜。'
  },
  {
    provider: 'google_news_rss',
    label: 'Google News RSS',
    name: 'Google News RSS',
    type: 'news',
    endpoint: 'https://news.google.com/rss/search',
    requiresApiKey: false,
    notes: '新闻搜索备用来源，可和腾讯新闻同时启用补充结果。'
  },
  {
    provider: 'pexels',
    label: 'Pexels 图片',
    name: 'Pexels',
    type: 'image',
    endpoint: 'https://api.pexels.com/v1/search',
    requiresApiKey: true,
    apiKeyFrom: 'PEXELS_API_KEY',
    notes: '图片搜索来源，可在此填写 apiKey 覆盖环境变量。'
  },
  {
    provider: 'bilibili',
    label: 'Bilibili 视频',
    name: 'Bilibili',
    type: 'video',
    endpoint: 'https://api.bilibili.com/x/web-interface/search/type',
    requiresApiKey: false,
    notes: '视频搜索主来源，会过滤付费课程等非普通视频结果。'
  },
  {
    provider: 'serpapi_youtube',
    label: 'SerpAPI YouTube',
    name: 'SerpAPI YouTube',
    type: 'video',
    endpoint: 'https://serpapi.com/search?engine=youtube',
    requiresApiKey: true,
    apiKeyFrom: 'SERPAPI_API_KEY',
    notes: '可选视频补充来源，填写 SerpAPI key 后启用。'
  }
]

const title = computed(() => meta[active.value].title)
const subtitle = computed(() => meta[active.value].subtitle)
const canCreate = computed(() => ['sources', 'hot', 'ranking'].includes(active.value))
const canDelete = computed(() => ['sources', 'hot', 'ranking', 'logs'].includes(active.value))
const canToggle = computed(() => ['users', 'sources', 'hot', 'ranking'].includes(active.value))
const hasActions = computed(() => ['users', 'sources', 'hot', 'ranking', 'logs'].includes(active.value))
const dialogTitle = computed(() => `${editingId.value ? '编辑' : '新增'}${title.value}`)

const columns = computed(() => ({
  users: [
    { prop: 'id', label: 'ID', width: 80 },
    { prop: 'username', label: '账号' },
    { prop: 'nickname', label: '昵称' },
    { prop: 'role', label: '角色', kind: 'role' },
    { prop: 'status', label: '状态', kind: 'status' },
    { prop: 'interestTags', label: '兴趣标签', width: 180 },
    { prop: 'preferredTypes', label: '偏好类型' },
    { prop: 'behaviorCount', label: '行为数' },
    { prop: 'createTime', label: '创建时间', width: 180 }
  ],
  sources: [
    { prop: 'id', label: 'ID', width: 80 },
    { prop: 'name', label: '来源' },
    { prop: 'providerLabel', label: '提供方', width: 150 },
    { prop: 'type', label: '类型' },
    { prop: 'enabled', label: '启用', kind: 'status' },
    { prop: 'weight', label: '权重' },
    { prop: 'authorityScore', label: '权威分' },
    { prop: 'endpoint', label: '端点', width: 260 },
    { prop: 'requiresApiKeyLabel', label: '密钥', width: 110 },
    { prop: 'updateTime', label: '更新时间', width: 180 }
  ],
  hot: [
    { prop: 'id', label: 'ID', width: 80 },
    { prop: 'type', label: '类型' },
    { prop: 'title', label: '标题', width: 220 },
    { prop: 'summary', label: '摘要', width: 260 },
    { prop: 'url', label: '链接', width: 240 },
    { prop: 'heatScore', label: '热度' },
    { prop: 'enabled', label: '启用', kind: 'status' },
    { prop: 'updateTime', label: '更新时间', width: 180 }
  ],
  ranking: [
    { prop: 'id', label: 'ID', width: 80 },
    { prop: 'name', label: '名称' },
    { prop: 'relevanceWeight', label: '相关性' },
    { prop: 'freshnessWeight', label: '时效性' },
    { prop: 'authorityWeight', label: '权威性' },
    { prop: 'preferenceWeight', label: '偏好' },
    { prop: 'enabled', label: '启用', kind: 'status' },
    { prop: 'updateTime', label: '更新时间', width: 180 }
  ],
  logs: [
    { prop: 'id', label: 'ID', width: 80 },
    { prop: 'userId', label: '用户' },
    { prop: 'keyword', label: '关键词', width: 180 },
    { prop: 'type', label: '类型' },
    { prop: 'resultCount', label: '结果数' },
    { prop: 'durationMs', label: '耗时ms' },
    { prop: 'sourceDistribution', label: '来源分布', width: 240 },
    { prop: 'createTime', label: '创建时间', width: 180 }
  ]
}[active.value] || []))

const statItems = computed(() => [
  { key: 'userCount', label: '用户数', value: stats.value.userCount ?? 0 },
  { key: 'sourceCount', label: '搜索源', value: stats.value.sourceCount ?? 0 },
  { key: 'enabledSources', label: '启用来源', value: stats.value.enabledSources ?? 0 },
  { key: 'hotCount', label: '热点内容', value: stats.value.hotCount ?? 0 },
  { key: 'blogCount', label: '原创博客', value: stats.value.blogCount ?? 0 },
  { key: 'imageCount', label: '原创图片', value: stats.value.imageCount ?? 0 },
  { key: 'enabledHot', label: '启用热点', value: stats.value.enabledHot ?? 0 },
  { key: 'rankingCount', label: '排序策略', value: stats.value.rankingCount ?? 0 },
  { key: 'searchCount', label: '搜索次数', value: stats.value.searchCount ?? 0 },
  { key: 'behaviorCount', label: '行为事件', value: stats.value.behaviorCount ?? 0 }
])

const rankingTotal = computed(() => ['relevanceWeight', 'freshnessWeight', 'authorityWeight', 'preferenceWeight']
  .reduce((total, key) => total + Number(form.value[key] || 0), 0))
const rankingTotalOk = computed(() => Math.abs(rankingTotal.value - 1) <= 0.01)

onMounted(load)
watch(active, () => {
  dialogVisible.value = false
  if (active.value === 'logs') {
    logPage.value = 1
  }
  load()
})

async function load() {
  loading.value = true
  try {
    if (active.value === 'stats') {
      stats.value = await http.get('/api/admin/stats')
    } else if (active.value === 'logs') {
      const data = await http.get('/api/admin/logs', {
        params: { page: logPage.value, size: logPageSize.value }
      })
      tableData.value = data.records || []
      logTotal.value = data.total || 0
      logPage.value = Number(data.page || logPage.value)
      logPageSize.value = Number(data.size || logPageSize.value)
    } else if (active.value === 'content') {
      const data = await http.get('/api/admin/content')
      contentData.value = {
        blogs: data.blogs || [],
        images: data.images || []
      }
    } else {
      const data = await http.get(`/api/admin/${active.value}`)
      tableData.value = active.value === 'sources' ? data.map(enrichSource) : data
    }
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = null
  form.value = defaults(active.value)
  if (active.value === 'sources') {
    syncSourceConfigJson()
  }
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = { ...row }
  if (active.value === 'sources') {
    form.value = { ...row, ...parseSourceConfig(row.configJson) }
    syncSourceConfigJson()
  }
  dialogVisible.value = true
}

async function save() {
  if (active.value === 'ranking' && !rankingTotalOk.value) {
    ElMessage.error('排序权重合计需要等于 1')
    return
  }
  saving.value = true
  try {
    const payload = payloadForActive()
    if (editingId.value) {
      await http.put(`/api/admin/${active.value}/${editingId.value}`, payload)
    } else {
      await http.post(`/api/admin/${active.value}`, payload)
    }
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function toggleEnabled(row) {
  const field = active.value === 'users' ? 'status' : 'enabled'
  const nextValue = row[field] === 1 ? 0 : 1
  await http.put(`/api/admin/${active.value}/${row.id}`, payloadForRow({ ...row, [field]: nextValue }))
  ElMessage.success(nextValue === 1 ? '已启用' : '已停用')
  await load()
}

async function remove(row) {
  await ElMessageBox.confirm('确认删除这条记录？', '提示')
  await http.delete(`/api/admin/${active.value}/${row.id}`)
  ElMessage.success('已删除')
  if (active.value === 'logs' && tableData.value.length === 1 && logPage.value > 1) {
    logPage.value -= 1
  }
  await load()
}

async function removeContent(type, row) {
  const label = type === 'blogs' ? '博客文章' : '图片资源'
  await ElMessageBox.confirm(`确认删除${label}「${row.title || row.fileName}」？`, '提示')
  await http.delete(`/api/admin/content/${type}/${row.id}`)
  ElMessage.success('已删除')
  await load()
}

function previewAdminImage(row) {
  adminPreviewUrl.value = row.url
  adminPreviewName.value = row.fileName
  adminPreviewVisible.value = true
}

function previewAdminBlog(row) {
  adminBlogPreview.value = row
  adminBlogPreviewVisible.value = true
}

async function toggleContentBlocked(type, row) {
  const blocked = row.blocked !== 1
  const label = type === 'blogs' ? '博客文章' : '图片资源'
  await ElMessageBox.confirm(`确认${blocked ? '封禁' : '解封'}${label}「${row.title || row.fileName}」？`, '提示')
  await http.put(`/api/admin/content/${type}/${row.id}/blocked`, null, {
    params: { blocked }
  })
  ElMessage.success(blocked ? '已封禁' : '已解封')
  await load()
}

function handleLogPageChange(page) {
  logPage.value = page
  load()
}

function handleLogPageSizeChange(size) {
  logPageSize.value = size
  logPage.value = 1
  load()
}

function defaults(type) {
  return {
    sources: {
      ...sourceDefaults('tencent_news'),
      enabled: 1,
      weight: 1,
      authorityScore: 0.7
    },
    hot: {
      type: 'news',
      title: '',
      summary: '',
      url: '',
      thumbnailUrl: '',
      heatScore: 60,
      enabled: 1
    },
    ranking: {
      name: '自定义排序策略',
      relevanceWeight: 0.45,
      freshnessWeight: 0.2,
      authorityWeight: 0.25,
      preferenceWeight: 0.1,
      enabled: 1
    }
  }[type] || {}
}

function applySourcePreset(provider) {
  const preset = sourceDefaults(provider)
  form.value = {
    ...form.value,
    provider,
    name: preset.name,
    type: preset.type,
    endpoint: preset.endpoint,
    requiresApiKey: preset.requiresApiKey,
    apiKeyFrom: preset.apiKeyFrom,
    notes: preset.notes,
    apiKey: form.value.apiKey || ''
  }
  syncSourceConfigJson()
}

function syncSourceConfigJson() {
  const config = {
    provider: form.value.provider,
    endpoint: form.value.endpoint,
    requiresApiKey: Boolean(form.value.requiresApiKey),
    notes: form.value.notes || ''
  }
  if (form.value.apiKeyFrom) {
    config.apiKeyFrom = form.value.apiKeyFrom
  }
  if (form.value.apiKey) {
    config.apiKey = form.value.apiKey
  }
  form.value.configJson = JSON.stringify(config, null, 2)
}

function enrichSource(row) {
  const config = parseSourceConfig(row.configJson)
  const preset = sourcePresets.find((item) => item.provider === config.provider)
  return {
    ...row,
    ...config,
    providerLabel: preset?.label || config.provider || '-',
    endpoint: config.endpoint || '-',
    requiresApiKeyLabel: config.requiresApiKey ? (config.apiKey || config.apiKeyFrom ? '已配置' : '需要') : '无需'
  }
}

function parseSourceConfig(raw) {
  try {
    const config = raw ? JSON.parse(raw) : {}
    return {
      provider: config.provider || 'tencent_news',
      endpoint: config.endpoint || '',
      requiresApiKey: Boolean(config.requiresApiKey),
      apiKeyFrom: config.apiKeyFrom || '',
      apiKey: config.apiKey || '',
      notes: config.notes || ''
    }
  } catch {
    return sourceDefaults('tencent_news')
  }
}

function sourceDefaults(provider) {
  const preset = sourcePresets.find((item) => item.provider === provider) || sourcePresets[0]
  return {
    provider: preset.provider,
    name: preset.name,
    type: preset.type,
    endpoint: preset.endpoint,
    requiresApiKey: preset.requiresApiKey,
    apiKeyFrom: preset.apiKeyFrom || '',
    apiKey: '',
    notes: preset.notes
  }
}

function payloadForActive() {
  if (active.value === 'sources') {
    syncSourceConfigJson()
  }
  return payloadForRow(form.value)
}

function payloadForRow(row) {
  if (active.value === 'sources') {
    const { id, name, type, enabled, weight, authorityScore, configJson } = row
    return { id, name, type, enabled, weight, authorityScore, configJson }
  }
  return { ...row }
}

function formatCell(value) {
  if (value === null || value === undefined || value === '') {
    return '-'
  }
  return value
}

function authorName(row) {
  return row.nickname || row.username || `用户 ${row.userId}`
}

function formatFileSize(value) {
  const size = Number(value || 0)
  if (size < 1024) {
    return `${size} B`
  }
  if (size < 1024 * 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${(size / 1024 / 1024).toFixed(1)} MB`
}
</script>
