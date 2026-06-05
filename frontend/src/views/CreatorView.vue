<template>
  <main class="creator-shell creator-home">
    <header class="topbar creator-topbar">
      <div class="brand" @click="$router.push('/')">Mint Creator</div>
      <nav class="nav-actions">
        <el-button text @click="$router.push('/')">返回搜索</el-button>
      </nav>
    </header>

    <section class="creator-home-main">
      <div class="creator-home-title">
        <span>内容聚合平台</span>
        <h1>选择一种媒体类型开始创作</h1>
        <p>Mint 可以聚合搜索外部媒体内容，也可以沉淀自己的原创内容。博客和图片作为两个独立媒体类型管理，后续可以继续扩展视频、音频或专题。</p>
      </div>

      <div class="creator-type-grid">
        <button type="button" class="creator-type-card blog-type" @click="$router.push('/creator/blogs')">
          <div class="type-icon"><EditPen /></div>
          <div>
            <strong>博客创作</strong>
            <span>进入 Markdown 写作工作台，撰写、保存草稿或发布图文博客。</span>
          </div>
        </button>

        <button type="button" class="creator-type-card image-type" @click="$router.push('/creator/images')">
          <div class="type-icon"><Picture /></div>
          <div>
            <strong>图片上传</strong>
            <span>上传图片到腾讯云 COS，并管理可被博客引用的图片资产。</span>
          </div>
        </button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { EditPen, Picture } from '@element-plus/icons-vue'
</script>
