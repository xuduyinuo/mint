<template>
  <main class="creator-shell writer-shell">
    <header class="topbar creator-topbar">
      <div class="brand" @click="$router.push('/creator')">Mint Creator</div>
      <nav class="nav-actions">
        <el-button text @click="$router.push('/creator/images')">图片资产</el-button>
        <el-button text @click="$router.push('/creator')">创作中心</el-button>
        <el-button text @click="$router.push('/')">返回搜索</el-button>
      </nav>
    </header>

    <section class="writer-layout">
      <aside class="writer-sidebar">
        <div class="writer-sidebar-header">
          <div>
            <h2>我的博客</h2>
            <p>草稿与已发布内容</p>
          </div>
          <el-button :icon="Plus" circle @click="resetForm" />
        </div>

        <div class="blog-list" v-loading="blogLoading">
          <button
            v-for="post in posts"
            :key="post.id"
            type="button"
            class="blog-item blog-select-item"
            :class="{ active: selectedPost?.id === post.id || form.id === post.id }"
            @click="viewPost(post)"
          >
            <b>{{ post.title || '未命名博客' }}</b>
            <span>{{ post.status === 'PUBLISHED' ? '已发布' : '草稿' }} · {{ formatDate(post.updateTime) }}</span>
          </button>
        </div>
      </aside>

      <section v-if="mode === 'preview' && selectedPost" class="writer-workbench writer-preview">
        <div class="writer-preview-header">
          <div>
            <el-tag :type="selectedPost.status === 'PUBLISHED' ? 'success' : 'info'">
              {{ selectedPost.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
            <h1>{{ selectedPost.title || '未命名博客' }}</h1>
            <p>{{ selectedPost.summary || '暂无摘要' }}</p>
            <div class="writer-preview-meta">
              <span>更新于 {{ formatDate(selectedPost.updateTime) }}</span>
              <span v-if="selectedPost.tags">标签：{{ selectedPost.tags }}</span>
            </div>
          </div>
          <div class="writer-preview-actions">
            <el-button @click="resetForm">新建</el-button>
            <el-button type="primary" @click="startEdit(selectedPost)">编辑</el-button>
            <el-button type="danger" plain @click="deletePost(selectedPost)">删除</el-button>
          </div>
        </div>

        <img v-if="selectedPost.coverUrl" :src="selectedPost.coverUrl" :alt="selectedPost.title" class="writer-preview-cover" />

        <MdPreview
          :model-value="selectedPost.content"
          editor-id="mint-blog-preview"
          preview-theme="github"
          code-theme="atom"
          class="mint-md-preview"
        />
      </section>

      <section v-else class="writer-workbench">
        <div class="writer-meta">
          <el-input v-model="form.title" maxlength="180" show-word-limit placeholder="输入博客标题" class="title-input" />
          <el-input v-model="form.summary" maxlength="512" show-word-limit placeholder="摘要会展示在搜索结果和内容列表中" />
          <div class="cover-row">
            <el-input v-model="form.coverUrl" placeholder="封面图 URL，可从图片资产复制或由编辑器上传图片生成" />
            <el-button @click="$router.push('/creator/images')">管理图片</el-button>
          </div>
        </div>

        <MdEditor
          v-model="form.content"
          class="mint-md-editor"
          editor-id="mint-blog-editor"
          language="zh-CN"
          preview-theme="github"
          code-theme="atom"
          :toolbars="toolbars"
          :footers="footers"
          :on-upload-img="handleEditorImageUpload"
          placeholder="在这里使用 Markdown 编写博客内容。可粘贴图片或点击工具栏图片按钮上传到腾讯云 COS。"
        />

        <div class="writer-actions">
          <el-button @click="resetForm">新建</el-button>
          <el-button :disabled="!form.id" type="danger" plain @click="deleteCurrentPost">删除</el-button>
          <el-button :disabled="!form.id" @click="cancelEdit">取消编辑</el-button>
          <el-button :loading="saving" @click="save('DRAFT')">保存草稿</el-button>
          <el-button type="primary" :loading="saving" @click="save('PUBLISHED')">发布博客</el-button>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { MdEditor, MdPreview } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import http from '../api/http'

const defaultForm = () => ({
  id: null,
  title: '',
  summary: '',
  coverUrl: '',
  content: ''
})

const toolbars = [
  'bold',
  'underline',
  'italic',
  '-',
  'title',
  'strikeThrough',
  'sub',
  'sup',
  'quote',
  'unorderedList',
  'orderedList',
  '-',
  'codeRow',
  'code',
  'link',
  'image',
  'table',
  '-',
  'revoke',
  'next',
  'save',
  '=',
  'preview',
  'htmlPreview',
  'catalog',
  'fullscreen'
]
const footers = ['markdownTotal', '=', 'scrollSwitch']

const form = ref(defaultForm())
const posts = ref([])
const selectedPost = ref(null)
const mode = ref('edit')
const saving = ref(false)
const blogLoading = ref(false)

onMounted(loadPosts)

async function loadPosts() {
  blogLoading.value = true
  try {
    posts.value = await http.get('/api/blogs/mine')
    if (selectedPost.value?.id) {
      selectedPost.value = posts.value.find((post) => post.id === selectedPost.value.id) || null
    }
  } finally {
    blogLoading.value = false
  }
}

async function handleEditorImageUpload(files, callback) {
  try {
    const uploaded = await Promise.all(files.map(uploadImageFile))
    if (!form.value.coverUrl && uploaded[0]?.url) {
      form.value.coverUrl = uploaded[0].url
    }
    callback(uploaded.map((asset) => ({ url: asset.url, alt: asset.fileName, title: asset.fileName })))
    ElMessage.success('图片已上传到腾讯云')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function uploadImageFile(file) {
  const body = new FormData()
  body.append('file', file)
  return http.post('/api/uploads/images', body, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}

async function save(status) {
  if (!form.value.title.trim()) {
    ElMessage.warning('请先填写博客标题')
    return
  }
  if (!form.value.content.trim()) {
    ElMessage.warning('请先填写博客正文')
    return
  }
  saving.value = true
  try {
    const payload = { ...form.value, status }
    if (form.value.id) {
      const updated = await http.put(`/api/blogs/${form.value.id}`, payload)
      selectedPost.value = updated
    } else {
      const created = await http.post('/api/blogs', payload)
      form.value.id = created.id
      selectedPost.value = created
    }
    ElMessage.success(status === 'PUBLISHED' ? '博客已发布' : '草稿已保存')
    await loadPosts()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    saving.value = false
  }
}

function viewPost(post) {
  selectedPost.value = post
  mode.value = 'preview'
}

function startEdit(post) {
  selectedPost.value = post
  form.value = { ...post }
  mode.value = 'edit'
}

function cancelEdit() {
  if (selectedPost.value) {
    mode.value = 'preview'
  } else {
    resetForm()
  }
}

async function deleteCurrentPost() {
  if (!form.value.id) {
    return
  }
  await deletePost(form.value)
}

async function deletePost(post) {
  await ElMessageBox.confirm(`确认删除博客「${post.title || '未命名博客'}」？`, '提示')
  await http.delete(`/api/blogs/${post.id}`)
  selectedPost.value = null
  resetForm()
  ElMessage.success('已删除')
  await loadPosts()
}

function resetForm() {
  form.value = defaultForm()
  selectedPost.value = null
  mode.value = 'edit'
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : ''
}
</script>
