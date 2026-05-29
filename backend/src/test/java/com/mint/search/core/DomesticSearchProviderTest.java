package com.mint.search.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mint.search.search.service.DomesticSearchProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DomesticSearchProviderTest {
    private final DomesticSearchProvider provider = new DomesticSearchProvider(new ObjectMapper());

    @Test
    void parsesPexelsImageResults() throws Exception {
        String json = """
                {"photos":[{"id":101,"url":"https://www.pexels.com/photo/smartphone-101/","photographer":"Alice","alt":"Smartphone on desk","src":{"medium":"https://images.pexels.com/photos/101/medium.jpeg","original":"https://images.pexels.com/photos/101/original.jpeg"}}]}
                """;

        var items = provider.parsePexelsImages(json, "手机");

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getType()).isEqualTo("image");
        assertThat(items.getFirst().getSourceName()).isEqualTo("Pexels");
        assertThat(items.getFirst().getTitle()).isEqualTo("Smartphone on desk");
        assertThat(items.getFirst().getUrl()).isEqualTo("https://www.pexels.com/photo/smartphone-101/");
        assertThat(items.getFirst().getThumbnailUrl()).isEqualTo("https://images.pexels.com/photos/101/medium.jpeg");
        assertThat(items.getFirst().getSummary()).isEqualTo("Pexels 图片，摄影师：Alice，关键词：手机");
    }

    @Test
    void parsesTencentNewsResults() throws Exception {
        String json = """
                {"secList":[{"newsList":[{"id":"N1","title":"AI 新闻","abstract":"新闻摘要","url":"https://view.inews.qq.com/a/N1","time":"2026-05-28 09:00:00","uinnick":"腾讯新闻","thumbnails_qqnews":["https://inews.gtimg.com/a.jpg"]}]}]}
                """;

        var items = provider.parseTencentNews(json, "AI");

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getType()).isEqualTo("news");
        assertThat(items.getFirst().getSourceName()).isEqualTo("腾讯新闻");
        assertThat(items.getFirst().getThumbnailUrl()).isEqualTo("https://inews.gtimg.com/a.jpg");
    }

    @Test
    void parsesGoogleNewsRssResults() {
        String xml = """
                <rss><channel>
                  <item>
                    <title>AI 新闻 - 示例媒体</title>
                    <link>https://news.google.com/rss/articles/abc</link>
                    <description>&lt;a href="https://example.com/news"&gt;AI 新闻&lt;/a&gt;&amp;nbsp;&amp;nbsp;&lt;font color="#6f6f6f"&gt;示例媒体&lt;/font&gt;</description>
                    <pubDate>Fri, 29 May 2026 02:45:00 GMT</pubDate>
                    <source url="https://example.com">示例媒体</source>
                  </item>
                </channel></rss>
                """;

        var items = provider.parseGoogleNewsRss(xml, "AI");

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getType()).isEqualTo("news");
        assertThat(items.getFirst().getSourceName()).isEqualTo("示例媒体");
        assertThat(items.getFirst().getTitle()).isEqualTo("AI 新闻 - 示例媒体");
        assertThat(items.getFirst().getUrl()).isEqualTo("https://news.google.com/rss/articles/abc");
        assertThat(items.getFirst().getSummary()).contains("AI 新闻");
    }

    @Test
    void parsesBilibiliVideoResults() throws Exception {
        String json = """
                {"data":{"result":[{"bvid":"BV1xx","title":"AI <em class=\\"keyword\\">视频</em>","description":"视频摘要","arcurl":"https://www.bilibili.com/video/BV1xx","pic":"//i0.hdslb.com/bfs/archive/a.jpg","author":"UP主","pubdate":1779940000}]}}
                """;

        var items = provider.parseBilibiliVideos(json, "AI");

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getType()).isEqualTo("video");
        assertThat(items.getFirst().getSourceName()).isEqualTo("UP主");
        assertThat(items.getFirst().getTitle()).isEqualTo("AI 视频");
        assertThat(items.getFirst().getThumbnailUrl())
                .isEqualTo("/api/media/thumbnail?url=https%3A%2F%2Fi0.hdslb.com%2Fbfs%2Farchive%2Fa.jpg");
    }

    @Test
    void buildsSerpApiYoutubeSearchUri() {
        var uri = provider.buildSerpApiYoutubeSearchUri("AI 视频", "serp-key");

        assertThat(uri.toString())
                .startsWith("https://serpapi.com/search?")
                .contains("engine=youtube")
                .contains("search_query=AI%20%E8%A7%86%E9%A2%91")
                .contains("api_key=serp-key");
    }

    @Test
    void parsesSerpApiYoutubeVideoResults() throws Exception {
        String json = """
                {"video_results":[{"title":"AI video","link":"https://www.youtube.com/watch?v=abc","snippet":"Video summary","thumbnail":{"static":"https://i.ytimg.com/vi/abc/hqdefault.jpg"},"channel":{"name":"OpenAI"},"published_date":"2 days ago"}]}
                """;

        var items = provider.parseSerpApiYoutubeVideos(json, "AI");

        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getType()).isEqualTo("video");
        assertThat(items.getFirst().getSourceName()).isEqualTo("OpenAI");
        assertThat(items.getFirst().getTitle()).isEqualTo("AI video");
        assertThat(items.getFirst().getUrl()).isEqualTo("https://www.youtube.com/watch?v=abc");
        assertThat(items.getFirst().getThumbnailUrl()).isEqualTo("https://i.ytimg.com/vi/abc/hqdefault.jpg");
        assertThat(items.getFirst().getSummary()).isEqualTo("Video summary");
    }
}
