package com.xzc.buyipicturebackend.model.vo.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 空间级别
 *
 * @author xuzhichao
 * @since 2025-01-10
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}
