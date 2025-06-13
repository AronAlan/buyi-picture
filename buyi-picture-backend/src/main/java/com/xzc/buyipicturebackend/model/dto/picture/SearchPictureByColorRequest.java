package com.xzc.buyipicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 以主色调搜图请求
 *
 * @author xuzhichao
 */
@Data
public class SearchPictureByColorRequest implements Serializable {

    /**
     * 图片主色调（十六进制）
     */
    private String picColor;

    /**
     * 空间 id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
