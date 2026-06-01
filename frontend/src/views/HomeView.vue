<template>
  <main class="app-shell">
    <header class="topbar">
      <div class="brand" @click="reset">Mint Search</div>
      <nav class="nav-actions">
        <el-button v-if="user.isAdmin" text @click="$router.push('/admin')">管理后台</el-button>
        <template v-if="user.loggedIn">
          <span class="user-chip">{{ user.user?.nickname }}</span>
          <el-button text @click="user.logout(); loadRecommendations()">退出</el-button>
        </template>
        <el-button v-else type="primary" plain @click="$router.push('/auth')">登录 / 注册</el-button>
      </nav>
    </header>

    <section class="search-zone">
      <h1>一次搜索，聚合新闻、图片、视频</h1>
      <div class="search-row">
        <el-input
          v-model="keyword"
          size="large"
          clearable
          placeholder="输入关键词或问题，例如：AI 搜索平台"
          @clear="reset"
          @keyup.enter="runSearch"
        />
        <el-button type="primary" size="large" :icon="Search" @click="runSearch">搜索</el-button>
      </div>
      <el-segmented v-model="activeType" :options="typeOptions" @change="handleTypeChange" />
    </section>

    <section v-if="searchResult" class="result-meta">
      <div>
        <b>{{ searchResult.keyword }}</b>
        <span>第 {{ searchResult.page }} 页，每页 {{ searchResult.size }} 条</span>
      </div>
      <span>{{ searchResult.rankingExplain }}</span>
    </section>

    <section class="content-grid" v-loading="loading">
      <el-empty
        v-if="!loading && visibleItems.length === 0"
        class="empty-state"
        description="暂无真实来源内容，请尝试更换关键词或稍后重试"
      />
      <article
        v-for="item in visibleItems"
        :key="item.externalId"
        class="result-card"
        @click="trackClick(item)"
      >
        <div v-if="item.thumbnailUrl" class="thumb" :style="{ backgroundImage: `url(${item.thumbnailUrl})` }">
          <span>{{ typeLabel(item.type) }}</span>
        </div>
        <div v-else class="text-thumb">
          <span>{{ typeLabel(item.type) }}</span>
        </div>
        <div class="card-body">
          <div class="card-title">{{ item.title }}</div>
          <footer>
            <span>{{ item.sourceName }}</span>
            <span>{{ formatDate(item.publishedAt) }}</span>
          </footer>
        </div>
      </article>
    </section>

    <section v-if="searchResult && visibleItems.length > 0" class="pagination-bar">
      <el-pagination
        background
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="pageSizeOptions"
        :total="searchResult.total"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handlePageSizeChange"
      />
    </section>

    <el-dialog v-model="previewVisible" width="min(760px, 94vw)" class="preview-dialog" destroy-on-close>
      <template #header>
        <div class="preview-header">
          <span>{{ preview?.title }}</span>
          <el-tag>{{ typeLabel(preview?.type) }}</el-tag>
        </div>
      </template>
      <div v-loading="previewLoading" class="preview-body">
        <div v-if="preview?.thumbnailUrl" class="preview-cover" :style="{ backgroundImage: `url(${preview.thumbnailUrl})` }"></div>
        <pre>{{ preview?.content }}</pre>
      </div>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button v-if="preview?.originalUrl" type="primary" @click="openOriginal">打开原文</el-button>
      </template>
    </el-dialog>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useUserStore } from '../stores/user'

const user = useUserStore()
const keyword = ref('')
const activeType = ref('all')
const loading = ref(false)
const previewLoading = ref(false)
const previewVisible = ref(false)
const preview = ref(null)
const searchResult = ref(null)
const recommendations = ref([])
const currentPage = ref(1)
const pageSize = ref(12)
const pageSizeOptions = [12, 16]

const typeOptions = [
  { label: '综合', value: 'all' },
  { label: '新闻', value: 'news' },
  { label: '图片', value: 'image' },
  { label: '视频', value: 'video' }
]

const bilibiliThumbnailHosts = new Set(['i0.hdslb.com', 'i1.hdslb.com', 'i2.hdslb.com'])

const visibleItems = computed(() => normalizeItems(searchResult.value?.records || recommendations.value))

onMounted(loadRecommendations)

async function loadRecommendations() {
  loading.value = true
  try {
    recommendations.value = await http.get('/api/recommendations', { params: { type: activeType.value } })
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function runSearch() {
  if (!keyword.value.trim()) {
    searchResult.value = null
    currentPage.value = 1
    await loadRecommendations()
    return
  }
  currentPage.value = 1
  await executeSearch()
}

async function executeSearch() {
  loading.value = true
  try {
    searchResult.value = await http.get('/api/search', {
      params: { q: keyword.value, type: activeType.value, page: currentPage.value, size: pageSize.value }
    })
    if (user.loggedIn) {
      await http.post('/api/behavior', { eventType: 'SEARCH', keyword: keyword.value, itemType: activeType.value })
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function handleTypeChange() {
  if (searchResult.value) {
    currentPage.value = 1
    runSearch()
  } else {
    loadRecommendations()
  }
}

function handlePageChange(page) {
  currentPage.value = page
  executeSearch()
}

function handlePageSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  executeSearch()
}

async function trackClick(item) {
  previewVisible.value = true
  previewLoading.value = true
  preview.value = {
    title: item.title,
    type: item.type,
    sourceName: item.sourceName,
    originalUrl: item.url,
    thumbnailUrl: item.thumbnailUrl,
    content: item.summary || '正在获取内容...'
  }
  if (user.loggedIn) {
    await http.post('/api/behavior', {
      eventType: 'CLICK',
      keyword: keyword.value,
      itemId: item.externalId,
      itemType: item.type,
      tags: item.tags,
      durationSeconds: 1
    }).catch(() => {})
  }
  try {
    preview.value = normalizePreview(await http.post('/api/content/preview', { item }))
  } catch (error) {
    ElMessage.warning('内容源暂不可用，已展示本地摘要')
  } finally {
    previewLoading.value = false
  }
}

function openOriginal() {
  if (preview.value?.originalUrl) {
    window.open(preview.value.originalUrl, '_blank')
  }
}

function reset() {
  keyword.value = ''
  searchResult.value = null
  currentPage.value = 1
  loadRecommendations()
}

function typeLabel(type) {
  return typeOptions.find((item) => item.value === type)?.label || '内容'
}

function formatDate(value) {
  if (!value) {
    return '暂无日期'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '暂无日期'
  }
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  })
}

function normalizeItems(items = []) {
  return items.map((item) => ({
    ...item,
    thumbnailUrl: normalizeThumbnailUrl(item.thumbnailUrl)
  }))
}

function normalizePreview(item) {
  return item ? { ...item, thumbnailUrl: normalizeThumbnailUrl(item.thumbnailUrl) } : item
}

function normalizeThumbnailUrl(url) {
  if (!url) {
    return ''
  }
  const normalized = url.startsWith('//') ? `https:${url}` : url
  if (normalized.startsWith('/api/media/thumbnail')) {
    return normalized
  }
  try {
    const parsed = new URL(normalized, window.location.origin)
    if (bilibiliThumbnailHosts.has(parsed.hostname)) {
      return `/api/media/thumbnail?url=${encodeURIComponent(parsed.href)}`
    }
  } catch (error) {
    return normalized
  }
  return normalized
}
</script>
