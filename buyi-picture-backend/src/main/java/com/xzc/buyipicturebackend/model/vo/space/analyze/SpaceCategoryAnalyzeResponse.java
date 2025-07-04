package com.xzc.buyipicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间图片分类分析 响应类
 *
 * @author xuzhichao
 */
@Data
@AllArgsConstructor
public class SpaceCategoryAnalyzeResponse implements Serializable {

    /**
     * 图片分类
     */
    private String category;

    /**
     * 图片数量
     */
    private Long count;

    /**
     * 分类图片总大小
     */
    private Long totalSize;

    private static final long serialVersionUID = 1L;
}
