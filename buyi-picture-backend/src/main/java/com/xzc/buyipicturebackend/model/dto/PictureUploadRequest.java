package com.xzc.buyipicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 图片上传请求
 *
 * @author xuzhichao
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改，上传按钮第二次及以后按，相当于上传错了）
     */
    private Long id;

    /**
     * 图片url地址
     */
    private String fileUrl;

    /**
     * 图片名称
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;

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
