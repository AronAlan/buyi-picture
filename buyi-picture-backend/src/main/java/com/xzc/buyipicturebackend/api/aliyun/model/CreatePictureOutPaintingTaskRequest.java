package com.xzc.buyipicturebackend.api.aliyun.model;

import lombok.Data;

import java.io.Serializable;

/**
 * AI扩图请求类
 *
 * @author xuzhichao
 */
@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;

    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}
