package com.xzc.buyipicturebackend.controller;

import cn.hutool.core.util.ObjUtil;
import com.xzc.buyipicturebackend.common.BaseResponse;
import com.xzc.buyipicturebackend.common.ResultUtils;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.model.dto.DeleteRequest;
import com.xzc.buyipicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xzc.buyipicturebackend.model.dto.spaceuser.SpaceUserEditRequest;
import com.xzc.buyipicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xzc.buyipicturebackend.model.entity.SpaceUser;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.spaceuser.SpaceUserVo;
import com.xzc.buyipicturebackend.service.SpaceUserService;
import com.xzc.buyipicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * （团队）空间与用户关联 控制层
 *
 * @author xuzhichao
 * @since 2025-06-13
 */
@Slf4j
@RestController
@RequestMapping("/spaceUser")
public class SpaceUserController {

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private UserService userService;

    /**
     * 添加成员到团队空间（仅有管理成员权限的空间管理员）
     *
     * @param spaceUserAddRequest SpaceUserAddRequest
     * @return spaceUser的id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpaceUser(@RequestBody SpaceUserAddRequest spaceUserAddRequest) {
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        long id = spaceUserService.addSpaceUser(spaceUserAddRequest);
        return ResultUtils.success(id);
    }

    /**
     * 从团队空间中移除成员（仅有管理成员权限的空间管理员）
     *
     * @param deleteRequest DeleteRequest
     * @return 移除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        Long id = deleteRequest.getId();
        // 判断空间是否存在
        SpaceUser spaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        boolean result = spaceUserService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "移除成员失败，数据库错误");
        return ResultUtils.success(true);
    }

    /**
     * 查询空间成员关联信息（仅有管理成员权限的空间管理员）
     *
     * @param spaceUserQueryRequest SpaceUserQueryRequest
     * @return SpaceUser
     */
    @PostMapping("/get")
    public BaseResponse<SpaceUser> getSpaceById(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);

        // 查询数据库
        SpaceUser spaceUser = spaceUserService.getOne(spaceUserService.getQueryWrapper(spaceUserQueryRequest));
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(spaceUser);
    }

    /**
     * 查询空间成员关联信息列表（仅有管理成员权限的空间管理员）
     *
     * @param spaceUserQueryRequest SpaceUserQueryRequest
     * @return List<SpaceUserVo>
     */
    @PostMapping("/list")
    public BaseResponse<List<SpaceUserVo>> listSpaceUser(@RequestBody SpaceUserQueryRequest spaceUserQueryRequest) {
        ThrowUtils.throwIf(spaceUserQueryRequest == null, ErrorCode.PARAMS_ERROR);
        List<SpaceUser> spaceUserList = spaceUserService.list(
                spaceUserService.getQueryWrapper(spaceUserQueryRequest)
        );
        return ResultUtils.success(spaceUserService.getSpaceUserVoList(spaceUserList));
    }

    /**
     * 编辑成员信息（仅有管理成员权限的空间管理员）
     *
     * @param spaceUserEditRequest SpaceUserEditRequest
     * @return 编辑成功
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpaceUser(@RequestBody SpaceUserEditRequest spaceUserEditRequest) {
        ThrowUtils.throwIf(spaceUserEditRequest == null || spaceUserEditRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // 将实体类和 DTO 进行转换
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserEditRequest, spaceUser);
        // 数据校验
        spaceUserService.validSpaceUser(spaceUser, false);

        // 判断是否存在
        long id = spaceUserEditRequest.getId();
        SpaceUser oldSpaceUser = spaceUserService.getById(id);
        ThrowUtils.throwIf(oldSpaceUser == null, ErrorCode.NOT_FOUND_ERROR);

        // 操作数据库
        boolean result = spaceUserService.updateById(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑成员失败，数据库错误");
        return ResultUtils.success(true);
    }

    /**
     * 查询我所加入的团队空间列表（所有用户）
     *
     * @param request HttpServletRequest
     * @return SpaceVO
     */
    @GetMapping("/list/my")
    public BaseResponse<List<SpaceUserVo>> listMyTeamSpaces(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        SpaceUserQueryRequest spaceUserQueryRequest = new SpaceUserQueryRequest();
        spaceUserQueryRequest.setUserId(loginUser.getId());
        // 查询数据库
        List<SpaceUser> spaceUserList = spaceUserService.list(spaceUserService.getQueryWrapper(spaceUserQueryRequest));

        // 获取封装类
        return ResultUtils.success(spaceUserService.getSpaceUserVoList(spaceUserList));
    }
}
