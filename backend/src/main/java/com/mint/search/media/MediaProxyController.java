package com.mint.search.media;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/media")
public class MediaProxyController {
    private static final Set<String> ALLOWED_HOSTS = Set.of(
            "i0.hdslb.com",
            "i1.hdslb.com",
            "i2.hdslb.com",
            "archive.biliimg.com"
    );

    private final RestClient restClient = RestClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
            .defaultHeader(HttpHeaders.REFERER, "https://www.bilibili.com/")
            .build();

    @GetMapping("/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@RequestParam String url) {
        if (!StringUtils.hasText(url)) {
            return ResponseEntity.badRequest().build();
        }
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !ALLOWED_HOSTS.contains(uri.getHost())) {
            return ResponseEntity.badRequest().build();
        }
        ResponseEntity<byte[]> response = restClient.get()
                .uri(uri)
                .retrieve()
                .toEntity(byte[].class);
        MediaType contentType = response.getHeaders().getContentType();
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .contentType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM : contentType)
                .body(response.getBody());
    }
}
