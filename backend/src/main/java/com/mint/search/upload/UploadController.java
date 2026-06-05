package com.mint.search.upload;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mint.search.common.ApiResponse;
import com.mint.search.security.SecurityUser;
import com.mint.search.upload.mapper.UploadedAssetMapper;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private final TencentCosService cosService;
    private final UploadedAssetMapper assetMapper;

    public UploadController(TencentCosService cosService, UploadedAssetMapper assetMapper) {
        this.cosService = cosService;
        this.assetMapper = assetMapper;
    }

    @PostMapping("/images")
    public ApiResponse<?> uploadImage(@RequestParam("file") MultipartFile file,
                                      @RequestParam(value = "tags", required = false) String tags,
                                      @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(cosService.uploadImage(user.id(), file, tags));
    }

    @GetMapping("/images")
    public ApiResponse<?> myImages(@AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(assetMapper.selectList(new LambdaQueryWrapper<UploadedAsset>()
                .eq(UploadedAsset::getUserId, user.id())
                .eq(UploadedAsset::getAssetType, "image")
                .orderByDesc(UploadedAsset::getCreateTime)));
    }

    @DeleteMapping("/images/{id}")
    public ApiResponse<?> deleteImage(@PathVariable Long id,
                                      @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(cosService.deleteImage(user.id(), id));
    }

    @PutMapping("/images/{id}/tags")
    public ApiResponse<?> updateImageTags(@PathVariable Long id,
                                          @RequestBody ImageTagRequest request,
                                          @AuthenticationPrincipal SecurityUser user) {
        return ApiResponse.ok(cosService.updateImageTags(user.id(), id, request.getTags()));
    }
}
