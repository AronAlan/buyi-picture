package com.xzc.buyipicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzc.buyipicturebackend.annotation.AuthCheck;
import com.xzc.buyipicturebackend.common.BaseResponse;
import com.xzc.buyipicturebackend.common.ResultUtils;
import com.xzc.buyipicturebackend.constant.UserConstant;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.model.dto.*;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.LoginUserVo;
import com.xzc.buyipicturebackend.model.vo.UserVo;
import com.xzc.buyipicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户 控制层
 *
 * @author xuzhichao
 * @since 2024-12-26
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     *
     * @param userRegisterRequest UserRegisterRequest
     * @return 用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        // 返回用户id
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest UserLoginRequest
     * @param request HttpServletRequest
     * @return LoginUserVo
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVo loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }


    /**
     * 获取当前登录用户(脱敏)
     *
     * @param request HttpServletRequest
     * @return LoginUserVo
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVo> getLoginUser(HttpServletRequest request) {
        User user = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(user));
    }

    /**
     * 用户注销
     *
     * @param request HttpServletRequest
     * @return 注销成功
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户（管理员）
     *
     * @param userAddRequest UserAddRequest
     * @return 用户id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user=new User();
        BeanUtils.copyProperties(userAddRequest,user);
        //默认密码12345678
        final String DEFAULT_PASSWORD="12345678";
        user.setUserPassword(userService.getEncryptPassword(DEFAULT_PASSWORD));
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"创建失败，数据库出错");
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据id获取用户（管理员）
     *
     * @param id 用户id
     * @return User（未脱敏）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR);
        User user=userService.getById(id);
        ThrowUtils.throwIf(user==null,ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取脱敏后的用户
     *
     * @param id 用户id
     * @return UserVo（脱敏）
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVo> getUserVoById(long id) {
        ThrowUtils.throwIf(id<=0, ErrorCode.PARAMS_ERROR);
        User user=userService.getById(id);
        UserVo userVo = userService.getUserVo(user);
        return ResultUtils.success(userVo);
    }

    /**
     * 删除用户（管理员）
     *
     * @param deleteRequest DeleteRequest(用户id)
     * @return 删除成功
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest==null||deleteRequest.getId()<=0, ErrorCode.PARAMS_ERROR);
        boolean result = userService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"删除失败，数据库出错");
        return ResultUtils.success(true);
    }

    /**
     * 更新用户（管理员）
     *
     * @param userUpdateRequest UserUpdateRequest
     * @return 更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest==null||userUpdateRequest.getId()<=0,
                ErrorCode.PARAMS_ERROR);
        User user=new User();
        BeanUtils.copyProperties(userUpdateRequest,user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"更新失败，数据库出错");
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户封装列表（管理员）
     *
     * @param userQueryRequest UserQueryRequest(用户id)
     * @return 用户封装列表
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVo>> listUserVoByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest==null, ErrorCode.PARAMS_ERROR);
        int current = userQueryRequest.getCurrent();
        int pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVo> userVoPage=new Page<>(current,pageSize,userPage.getTotal());
        List<UserVo> userVoList = userService.getUserVoList(userPage.getRecords());
        userVoPage.setRecords(userVoList);
        return ResultUtils.success(userVoPage);
    }

}
