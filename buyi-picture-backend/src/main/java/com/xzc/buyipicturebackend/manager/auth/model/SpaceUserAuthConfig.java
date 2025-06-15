package com.xzc.buyipicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 权限控制 模型
 * 接收配置文件的值
 *
 * @author: Samoyer
 * @date: 2025-06-13
 */
@Data
public class SpaceUserAuthConfig implements Serializable {
    /**
     * 权限列表
     */
    private List<SpaceUserPermission> permissions;

    /**
     * 角色列表
     */
    private List<SpaceUserRole> roles;
}
