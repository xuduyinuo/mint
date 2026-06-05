<template>
  <main class="creator-shell image-library-shell">
    <header class="topbar creator-topbar">
      <div class="brand" @click="$router.push('/creator')">Mint Creator</div>
      <nav class="nav-actions">
        <el-button text @click="$router.push('/creator/blogs')">博客创作</el-button>
        <el-button text @click="$router.push('/creator')">创作中心</el-button>
        <el-button text @click="$router.push('/')">返回搜索</el-button>
      </nav>
    </header>

    <section class="image-library-layout">
      <div class="image-hero">
        <div class="image-library-header">
          <span>图片媒体</span>
          <h1>上传与管理图片资产</h1>
          <p>图片会上传到腾讯云 COS，可作为独立图片内容，也可复制链接插入博客。</p>
        </div>
        <button type="button" class="upload-hero-button" @click="openUploadDialog">
          <span class="upload-hero-icon"><UploadFilled /></span>
          <b>上传图片</b>
          <small>添加标签后上传</small>
        </button>
      </div>

      <div class="asset-toolbar">
        <b>图片资产</b>
        <el-button :icon="Refresh" @click="loadAssets">刷新</el-button>
      </div>

      <div class="asset-masonry" v-loading="assetLoading">
        <article v-for="asset in assets" :key="asset.id" class="asset-card">
          <img :src="asset.url" :alt="asset.fileName" @click="previewAsset(asset)" />
          <div class="asset-overlay">
            <strong>{{ asset.fileName }}</strong>
            <span>{{ formatDate(asset.createTime) }}</span>
            <div v-if="tagList(asset.tags).length" class="asset-tags">
              <em v-for="tag in tagList(asset.tags)" :key="tag">{{ tag }}</em>
            </div>
            <div class="asset-card-actions">
              <el-button link type="primary" @click="copyUrl(asset.url)">复制链接</el-button>
              <el-button link @click="previewAsset(asset)">查看</el-button>
              <el-button link @click="openTagEditor(asset)">标签</el-button>
              <el-button link type="danger" @click="deleteAsset(asset)">删除</el-button>
            </div>
          </div>
        </article>
      </div>

      <el-dialog v-model="previewVisible" :title="previewAssetName" class="image-preview-dialog" width="fit-content">
        <img v-if="previewUrl" :src="previewUrl" :alt="previewAssetName" class="preview-large-image" />
      </el-dialog>

      <el-dialog v-model="uploadVisible" class="image-upload-dialog" width="min(760px, 94vw)" destroy-on-close>
        <template #header>
          <div class="upload-dialog-title">
            <h2>分享一张图</h2>
            <p>先添加标签，再拖拽或选择图片上传。标签会参与搜索匹配。</p>
          </div>
        </template>

        <el-upload
          drag
          multiple
          :show-file-list="false"
          :http-request="uploadImage"
          accept="image/png,image/jpeg,image/webp,image/gif"
          class="dialog-image-uploader"
        >
          <div class="dialog-upload-drop">
            <div class="dialog-upload-icon"><UploadFilled /></div>
            <b>把图丢进来</b>
            <el-button type="primary" round>选择图片 / GIF</el-button>
            <span>JPG / PNG / GIF / WebP，单张小于 10MB，上传后统一压缩为 JPG</span>
          </div>
        </el-upload>

        <div class="tag-editor">
          <label>标签 <span>最多 5 个</span></label>
          <el-input
            v-model="tagInput"
            placeholder="输入后按 Enter 新增..."
            @keyup.enter="addTag(tagInput)"
          />
          <div v-if="uploadTags.length" class="selected-tags">
            <el-tag v-for="tag in uploadTags" :key="tag" closable @close="removeTag(tag)">{{ tag }}</el-tag>
          </div>
          <div class="quick-tags">
            <span>常用：</span>
            <button v-for="tag in quickTags" :key="tag" type="button" @click="addTag(tag)">+ {{ tag }}</button>
          </div>
        </div>
      </el-dialog>

      <el-dialog v-model="tagEditVisible" title="编辑图片标签" width="min(520px, 92vw)" destroy-on-close>
        <div class="tag-editor compact-tag-editor">
          <label>标签 <span>最多 5 个，用于搜索匹配</span></label>
          <el-input
            v-model="editTagInput"
            placeholder="输入后按 Enter 新增..."
            @keyup.enter="addEditTag(editTagInput)"
          />
          <div v-if="editTags.length" class="selected-tags">
            <el-tag v-for="tag in editTags" :key="tag" closable @close="removeEditTag(tag)">{{ tag }}</el-tag>
          </div>
          <div class="quick-tags">
            <span>常用：</span>
            <button v-for="tag in quickTags" :key="tag" type="button" @click="addEditTag(tag)">+ {{ tag }}</button>
          </div>
        </div>
        <template #footer>
          <el-button @click="tagEditVisible = false">取消</el-button>
          <el-button type="primary" @click="saveAssetTags">保存标签</el-button>
        </template>
      </el-dialog>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, UploadFilled } from '@element-plus/icons-vue'
import http from '../api/http'

const assets = ref([])
const assetLoading = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const previewAssetName = ref('')
const uploadVisible = ref(false)
const tagInput = ref('')
const uploadTags = ref([])
const tagEditVisible = ref(false)
const editingAsset = ref(null)
const editTagInput = ref('')
const editTags = ref([])
const quickTags = ['风景', '人物', '学习', '生活', '创作', '日常']

onMounted(loadAssets)

async function uploadImage({ file }) {
  const body = new FormData()
  body.append('file', file)
  body.append('tags', uploadTags.value.join(','))
  try {
    const asset = await http.post('/api/uploads/images', body, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 60000
    })
    assets.value.unshift(asset)
    ElMessage.success('图片已上传到腾讯云')
    uploadVisible.value = false
    resetUploadTags()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openUploadDialog() {
  uploadVisible.value = true
}

function addTag(value) {
  const tag = String(value || '').trim()
  if (!tag) {
    return
  }
  if (uploadTags.value.length >= 5) {
    ElMessage.warning('最多添加 5 个标签')
    return
  }
  if (!uploadTags.value.includes(tag)) {
    uploadTags.value.push(tag)
  }
  tagInput.value = ''
}

function removeTag(tag) {
  uploadTags.value = uploadTags.value.filter((item) => item !== tag)
}

function resetUploadTags() {
  tagInput.value = ''
  uploadTags.value = []
}

function tagList(tags) {
  return String(tags || '')
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean)
}

function openTagEditor(asset) {
  editingAsset.value = asset
  editTags.value = tagList(asset.tags).slice(0, 5)
  editTagInput.value = ''
  tagEditVisible.value = true
}

function addEditTag(value) {
  const tag = String(value || '').trim()
  if (!tag) {
    return
  }
  if (editTags.value.length >= 5) {
    ElMessage.warning('最多添加 5 个标签')
    return
  }
  if (!editTags.value.includes(tag)) {
    editTags.value.push(tag)
  }
  editTagInput.value = ''
}

function removeEditTag(tag) {
  editTags.value = editTags.value.filter((item) => item !== tag)
}

async function saveAssetTags() {
  if (!editingAsset.value) {
    return
  }
  const updated = await http.put(`/api/uploads/images/${editingAsset.value.id}/tags`, {
    tags: editTags.value.join(',')
  })
  assets.value = assets.value.map((item) => (item.id === updated.id ? updated : item))
  tagEditVisible.value = false
  ElMessage.success('标签已更新')
}

async function loadAssets() {
  assetLoading.value = true
  try {
    assets.value = await http.get('/api/uploads/images')
  } finally {
    assetLoading.value = false
  }
}

async function copyUrl(url) {
  try {
    await navigator.clipboard.writeText(url)
  } catch {
    const textarea = document.createElement('textarea')
    textarea.value = url
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
  }
  ElMessage.success('链接已复制')
}

function previewAsset(asset) {
  previewUrl.value = asset.url
  previewAssetName.value = asset.fileName
  previewVisible.value = true
}

async function deleteAsset(asset) {
  await ElMessageBox.confirm(`确认删除图片「${asset.fileName}」？`, '删除图片')
  await http.delete(`/api/uploads/images/${asset.id}`)
  assets.value = assets.value.filter((item) => item.id !== asset.id)
  ElMessage.success('图片已删除')
}

function formatDate(value) {
  return value ? new Date(value).toLocaleString('zh-CN') : ''
}
</script>
