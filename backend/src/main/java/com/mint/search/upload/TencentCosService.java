package com.mint.search.upload;

import com.mint.search.upload.mapper.UploadedAssetMapper;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Service
public class TencentCosService {
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final CosProperties properties;
    private final UploadedAssetMapper assetMapper;
    private final ImageProcessingService imageProcessingService;

    public TencentCosService(CosProperties properties, UploadedAssetMapper assetMapper,
                             ImageProcessingService imageProcessingService) {
        this.properties = properties;
        this.assetMapper = assetMapper;
        this.imageProcessingService = imageProcessingService;
    }

    public UploadedAsset uploadImage(Long userId, MultipartFile file, String tags) {
        validateImage(file);
        ensureConfigured();
        ProcessedImage image = imageProcessingService.toCompressedJpeg(file);
        String objectKey = objectKey(userId, image.fileName());
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(image.contentType());
        metadata.setContentLength(image.size());

        COSClient client = client();
        try {
            client.putObject(new PutObjectRequest(properties.getBucket(), objectKey,
                    new ByteArrayInputStream(image.bytes()), metadata));
        } catch (Exception exception) {
            throw new IllegalStateException("图片上传腾讯云失败：" + exception.getMessage(), exception);
        } finally {
            client.shutdown();
        }

        UploadedAsset asset = new UploadedAsset();
        asset.setUserId(userId);
        asset.setAssetType("image");
        asset.setFileName(image.fileName());
        asset.setObjectKey(objectKey);
        asset.setUrl(publicUrl(objectKey));
        asset.setContentType(image.contentType());
        asset.setFileSize(image.size());
        asset.setTags(normalizeTags(tags));
        asset.setBlocked(0);
        assetMapper.insert(asset);
        return asset;
    }

    public boolean deleteImage(Long userId, Long assetId) {
        UploadedAsset asset = assetMapper.selectById(assetId);
        if (asset == null || !asset.getUserId().equals(userId) || !"image".equals(asset.getAssetType())) {
            throw new IllegalArgumentException("图片不存在或无权操作");
        }
        ensureConfigured();
        COSClient client = client();
        try {
            client.deleteObject(properties.getBucket(), asset.getObjectKey());
        } catch (Exception exception) {
            throw new IllegalStateException("删除腾讯云图片失败：" + exception.getMessage(), exception);
        } finally {
            client.shutdown();
        }
        return assetMapper.deleteById(assetId) > 0;
    }

    public UploadedAsset updateImageTags(Long userId, Long assetId, String tags) {
        UploadedAsset asset = assetMapper.selectById(assetId);
        if (asset == null || !asset.getUserId().equals(userId) || !"image".equals(asset.getAssetType())) {
            throw new IllegalArgumentException("图片不存在或无权操作");
        }
        asset.setTags(normalizeTags(tags));
        assetMapper.updateById(asset);
        return assetMapper.selectById(assetId);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("仅支持 JPG、PNG、WEBP、GIF 图片");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("图片大小不能超过 10MB");
        }
    }

    private void ensureConfigured() {
        if (!StringUtils.hasText(properties.getBucket()) || !StringUtils.hasText(properties.getRegion())
                || !StringUtils.hasText(properties.getSecretId()) || !StringUtils.hasText(properties.getSecretKey())) {
            throw new IllegalStateException("腾讯云 COS 未配置完整，请设置 TENCENT_COS_SECRET_ID 和 TENCENT_COS_SECRET_KEY");
        }
    }

    private COSClient client() {
        COSCredentials credentials = new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey());
        return new COSClient(credentials, new ClientConfig(new Region(properties.getRegion())));
    }

    private String objectKey(Long userId, String fileName) {
        String safeName = StringUtils.hasText(fileName) ? fileName : UUID.randomUUID() + ".jpg";
        return "uploads/users/" + userId + "/" + LocalDate.now() + "/" + safeName;
    }

    private String normalizeTags(String tags) {
        if (!StringUtils.hasText(tags)) {
            return "";
        }
        return tags.trim().replaceAll("[，、\\s]+", ",");
    }

    private String publicUrl(String objectKey) {
        String domain = StringUtils.hasText(properties.getCustomDomain())
                ? properties.getCustomDomain()
                : "https://" + properties.getBucket() + ".cos." + properties.getRegion() + ".myqcloud.com";
        return domain.replaceAll("/+$", "") + "/" + objectKey;
    }
}
