package com.xzc.buyipicturebackend.model.dto.picture;

import lombok.Data;

/**
 * 图片上传并解析后返回的结果
 *
 * @author xuzhichao
 */
@Data
public class UploadPictureResult {

    /**
     * 图片原图地址
     */
    private String url;

    /**
     * 图片压缩为webp地址
     */
    private String webpUrl;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private int picWidth;

    /**
     * 图片高度
     */
    private int picHeight;

    /**
     * 图片宽高比
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;


}
