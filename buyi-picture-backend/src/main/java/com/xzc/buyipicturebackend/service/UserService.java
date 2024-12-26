package com.xzc.buyipicturebackend.service;

import com.xzc.buyipicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xzc.buyipicturebackend.model.vo.LoginUserVo;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xuzhichao
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2024-12-26 16:53:08
 */
public interface UserService extends IService<User> {
    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户密码加密
     *
     * @param userPassword 明文密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request      HttpServletRequest
     * @return 脱敏后的用户信息
     */
    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request HttpServletRequest
     * @return User
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @param user User
     * @return LoginUserVo
     */
    LoginUserVo getLoginUserVO(User user);

    /**
     * 用户注销
     *
     * @param request HttpServletRequest
     * @return 注销是否成功
     */
    boolean userLogout(HttpServletRequest request);


}
