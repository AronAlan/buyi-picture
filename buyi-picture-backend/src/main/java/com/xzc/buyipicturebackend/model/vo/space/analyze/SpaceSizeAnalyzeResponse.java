package com.xzc.buyipicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间图片大小范围与对应数量分析 响应类
 *
 * @author xuzhichao
 */
@Data
@AllArgsConstructor
public class SpaceSizeAnalyzeResponse implements Serializable {

    /**
     * 图片大小范围
     */
    private String sizeRange;

    /**
     * 图片数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
