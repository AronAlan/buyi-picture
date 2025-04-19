package com.xzc.buyipicturebackend.api.imagesearch.model;

import lombok.Data;

/**
 * 以图搜图结果类，接收API返回值
 * @author xuzhichao
 * @since 2025-03-13
 */
@Data
public class ImageSearchResult {
    /**
     * 图片地址
     */
    private String imgUrl;

    /**
     * 标题
     */
    private String title;

    /**
     * 图片key
     */
    private String imgkey;

    /**
     * HTTP
     */
    private String http;

    /**
     * HTTPS
     */
    private String https;
}
