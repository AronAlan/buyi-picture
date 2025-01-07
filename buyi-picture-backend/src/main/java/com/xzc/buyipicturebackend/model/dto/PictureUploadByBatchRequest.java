package com.xzc.buyipicturebackend.model.dto;

import lombok.Data;

/**
 * 批量抓取图片请求
 *
 * @author xuzhichao
 */
@Data
public class PictureUploadByBatchRequest {

    /**
     * 搜索词
     */
    private String searchText;

    /**
     * 抓取数量
     */
    private Integer count = 10;

    /**
     * 名称前缀
     */
    private String namePrefix;

}
