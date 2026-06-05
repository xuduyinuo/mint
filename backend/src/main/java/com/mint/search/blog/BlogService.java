package com.mint.search.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.blog.dto.BlogPostRequest;
import com.mint.search.blog.mapper.BlogPostMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;

@Service
public class BlogService {
    private final BlogPostMapper blogMapper;

    public BlogService(BlogPostMapper blogMapper) {
        this.blogMapper = blogMapper;
    }

    public List<BlogPost> published() {
        return blogMapper.selectList(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getStatus, "PUBLISHED")
                .eq(BlogPost::getBlocked, 0)
                .orderByDesc(BlogPost::getUpdateTime));
    }

    public List<BlogPost> mine(Long userId) {
        return blogMapper.selectList(new LambdaQueryWrapper<BlogPost>()
                .eq(BlogPost::getUserId, userId)
                .orderByDesc(BlogPost::getUpdateTime));
    }

    public BlogPost create(Long userId, BlogPostRequest request) {
        BlogPost post = new BlogPost();
        post.setUserId(userId);
        post.setBlocked(0);
        apply(post, request);
        blogMapper.insert(post);
        return post;
    }

    public BlogPost update(Long userId, Long id, BlogPostRequest request) {
        BlogPost post = mustOwn(userId, id);
        if (Integer.valueOf(1).equals(post.getBlocked())) {
            throw new IllegalArgumentException("博客已被管理员封禁，无法编辑");
        }
        apply(post, request);
        blogMapper.updateById(post);
        return blogMapper.selectById(id);
    }

    public boolean delete(Long userId, Long id) {
        mustOwn(userId, id);
        return blogMapper.deleteById(id) > 0;
    }

    private void apply(BlogPost post, BlogPostRequest request) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new IllegalArgumentException("博客标题不能为空");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("博客正文不能为空");
        }
        String status = StringUtils.hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase() : "DRAFT";
        if (!Set.of("DRAFT", "PUBLISHED").contains(status)) {
            throw new IllegalArgumentException("博客状态只能是 DRAFT 或 PUBLISHED");
        }
        post.setTitle(request.getTitle().trim());
        post.setSummary(StringUtils.hasText(request.getSummary()) ? request.getSummary().trim() : "");
        post.setCoverUrl(StringUtils.hasText(request.getCoverUrl()) ? request.getCoverUrl().trim() : "");
        post.setTags(StringUtils.hasText(request.getTags()) ? request.getTags().trim() : "");
        post.setContent(request.getContent());
        post.setStatus(status);
    }

    private BlogPost mustOwn(Long userId, Long id) {
        BlogPost post = blogMapper.selectById(id);
        if (post == null || !post.getUserId().equals(userId)) {
            throw new IllegalArgumentException("博客不存在或无权操作");
        }
        return post;
    }
}
