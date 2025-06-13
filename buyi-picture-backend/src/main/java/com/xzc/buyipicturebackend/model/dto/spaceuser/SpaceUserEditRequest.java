package com.xzc.buyipicturebackend.model.dto.spaceuser;

import lombok.Data;

import java.io.Serializable;

/**
 * 编辑空间成员请求（空间管理员使用）
 *
 * @author: Samoyer
 * @date: 2025-06-12
 */
@Data
public class SpaceUserEditRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    private static final long serialVersionUID = 1L;
}
