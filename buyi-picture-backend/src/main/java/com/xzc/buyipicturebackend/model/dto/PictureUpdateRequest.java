package com.xzc.buyipicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片更新请求（管理员）
 *
 * @author xuzhichao
 */
@Data
public class PictureUpdateRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}