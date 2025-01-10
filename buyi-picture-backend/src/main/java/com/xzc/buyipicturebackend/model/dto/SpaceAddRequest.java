package com.xzc.buyipicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间添加请求
 *
 * @author xuzhichao
 */
@Data
public class SpaceAddRequest implements Serializable {

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    private static final long serialVersionUID = 1L;
}
