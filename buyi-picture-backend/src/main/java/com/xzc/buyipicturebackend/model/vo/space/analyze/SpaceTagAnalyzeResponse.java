package com.xzc.buyipicturebackend.model.vo.space.analyze;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间图片标签分析 响应类
 *
 * @author xuzhichao
 */
@Data
@AllArgsConstructor
public class SpaceTagAnalyzeResponse implements Serializable {

    /**
     * 标签名称
     */
    private String tag;

    /**
     * 关联的图片数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
