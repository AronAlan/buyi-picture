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
public class SpaceUserAnalyzeResponse implements Serializable {

    /**
     * 时间区间
     */
    private String period;

    /**
     * 上传数量
     */
    private Long count;

    private static final long serialVersionUID = 1L;
}
