package com.xzc.buyipicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzc.buyipicturebackend.model.dto.SpaceAddRequest;
import com.xzc.buyipicturebackend.model.dto.SpaceQueryRequest;
import com.xzc.buyipicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.SpaceVo;

import javax.servlet.http.HttpServletRequest;

/**
 * @author xuzhichao
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-01-10 14:58:13
 */
public interface SpaceService extends IService<Space> {

    /**
     * 用户创建私有空间
     *
     * @param spaceAddRequest SpaceAddRequest
     * @param loginUser       User
     * @return 空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 添加或编辑时，校验空间
     *
     * @param space 空间
     * @param add   是否为创建新空间
     */
    void validSpace(Space space, boolean add);

    /**
     * 创建或更新空间时，根据空间级别自动填充限额数据
     *
     * @param space 空间
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 对象转封装类，关联查询用户信息
     *
     * @param space   Space
     * @param request HttpServletRequest
     * @return SpaceVo
     */
    SpaceVo getSpaceVo(Space space, HttpServletRequest request);

    /**
     * 分页Space对象转分页SpaceVo对象
     *
     * @param spacePage Page<Space>
     * @param request   HttpServletRequest
     * @return Page<SpaceVo>
     */
    Page<SpaceVo> getSpaceVoPage(Page<Space> spacePage, HttpServletRequest request);

    /**
     * 构造空间查询请求QueryWrapper
     *
     * @param spaceQueryRequest PictureQueryRequest
     * @return QueryWrapper<Space>
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 校验空间权限，仅管理员和本人有权限，否则抛异常
     *
     * @param loginUser 用户
     * @param space     空间
     */
    void checkSpaceAuth(User loginUser, Space space);
}
