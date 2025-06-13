package com.xzc.buyipicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 查询空间成员的请求
 *
 * @author: Samoyer
 * @date: 2025-06-12
 */
@Data
public class SpaceUserQueryRequest implements Serializable {
    /**
     * ID
     */
    private Long id;

    /**
     * 空间 ID
     */
    private Long spaceId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
