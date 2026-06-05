package com.mint.search.upload;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("uploaded_asset")
public class UploadedAsset {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String assetType;
    private String fileName;
    private String objectKey;
    private String url;
    private String contentType;
    private Long fileSize;
    private String tags;
    private Integer blocked;
    @TableLogic
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
