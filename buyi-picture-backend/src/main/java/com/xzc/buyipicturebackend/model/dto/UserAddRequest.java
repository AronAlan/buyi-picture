package com.xzc.buyipicturebackend.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加用户 请求DTO
 * @author xuzhichao
 * @since 2024-12-26
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
