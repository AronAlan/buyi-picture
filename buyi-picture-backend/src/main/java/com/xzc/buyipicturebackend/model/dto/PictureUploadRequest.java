package com.xzc.buyipicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片上传请求
 *
 * @author xuzhichao
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
