package com.xzc.buyipicturebackend.manager.auth.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 权限
 *
 * @author: Samoyer
 * @date: 2025-06-13
 */
@Data
public class SpaceUserPermission implements Serializable {

    /**
     * 权限键
     */
    private String key;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    private String description;

    private static final long serialVersionUID = 1L;

}