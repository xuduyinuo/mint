package com.mint.search.content;

import com.mint.search.common.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content")
public class ContentPreviewController {
    private final ContentPreviewService contentPreviewService;

    public ContentPreviewController(ContentPreviewService contentPreviewService) {
        this.contentPreviewService = contentPreviewService;
    }

    @PostMapping("/preview")
    public ApiResponse<?> preview(@RequestBody ContentPreviewRequest request) {
        return ApiResponse.ok(contentPreviewService.preview(request));
    }
}
