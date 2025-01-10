package com.xzc.buyipicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzc.buyipicturebackend.annotation.AuthCheck;
import com.xzc.buyipicturebackend.common.BaseResponse;
import com.xzc.buyipicturebackend.common.ResultUtils;
import com.xzc.buyipicturebackend.constant.UserConstant;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.model.dto.*;
import com.xzc.buyipicturebackend.model.entity.Space;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.enums.SpaceLevelEnum;
import com.xzc.buyipicturebackend.model.vo.SpaceLevel;
import com.xzc.buyipicturebackend.model.vo.SpaceVo;
import com.xzc.buyipicturebackend.service.PictureService;
import com.xzc.buyipicturebackend.service.SpaceService;
import com.xzc.buyipicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 空间 控制层
 *
 * @author xuzhichao
 * @since 2025-01-10
 */
@Slf4j
@RestController
@RequestMapping("/space")
public class SpaceController {

    @Resource
    private SpaceService spaceService;

    @Resource
    private UserService userService;

    /**
     * 添加创建私有图库空间
     *
     * @param spaceAddRequest SpaceAddRequest
     * @param request         HttpServletRequest
     * @return 图库空间id
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long newId = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(newId);
    }

    /**
     * 删除私有空间
     *
     * @param deleteRequest DeleteRequest
     * @param request       HttpServletRequest
     * @return 删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest
            , HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();
        // 判断空间是否存在
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或者管理员可删除
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 操作数据库
        boolean result = spaceService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除空间失败，数据库错误");
        return ResultUtils.success(true);
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 分页获取空间列表（封装类）（用户）
     * 用户进入“我的空间”时查询自己的私有空间，如果列表为0则去创建。最多1个
     *
     * @param spaceQueryRequest SpaceQueryRequest
     * @param request           HttpServletRequest
     * @return Page<SpaceVO>（脱敏）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVo>> listSpaceVoByPage(@RequestBody SpaceQueryRequest spaceQueryRequest
            , HttpServletRequest request) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVoPage(spacePage, request));
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     *
     * @param id 空间id
     * @return Space
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Space> getSpaceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（封装类）（用户）
     *
     * @param id      空间id
     * @param request HttpServletRequest
     * @return SpaceVO
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVo> getSpaceVoById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        // 非本人查询
        if (!userService.getLoginUser(request).getId().equals(space.getUserId())){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR,"您无权限查看他人的私有图库空间");
        }
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVo(space, request));
    }

    /**
     * 更新空间（仅管理员可用）
     *
     * @param spaceUpdateRequest SpaceUpdateRequest
     * @return 更新成功
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);
        // 自动填充数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 数据校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间（给用户使用）
     * 目前只支持修改空间名称
     *
     * @param spaceEditRequest SpaceEditRequest
     * @param request          HttpServletRequest
     * @return 编辑成功
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);
        // 自动填充限额数据
        spaceService.fillSpaceBySpaceLevel(space);
        // 设置编辑时间
        space.setEditTime(new Date());
        // 数据校验
        spaceService.validSpace(space, false);
        User loginUser = userService.getLoginUser(request);
        // 判断是否存在
        long id = spaceEditRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
        // 仅本人或管理员可编辑
        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 获取空间级别列表，便于前端展示
     *
     * @return List<SpaceLevel>
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        // 获取所有枚举
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }


}
