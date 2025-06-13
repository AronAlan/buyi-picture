package com.xzc.buyipicturebackend.model.dto.space;

import com.xzc.buyipicturebackend.model.dto.PageRequest;
import lombok.EqualsAndHashCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 空间查询请求
 *
 * @author xuzhichao
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class SpaceQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间名称
     */
    private String spaceName;

    /**
     * 空间类型：0-私有 1-团队
     */
    private Integer spaceType;

    /**
     * 空间级别：0-普通版 1-专业版 2-旗舰版
     */
    private Integer spaceLevel;

    private static final long serialVersionUID = 1L;
}
