package com.xzc.buyipicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xzc.buyipicturebackend.model.dto.space.SpaceAddRequest;
import com.xzc.buyipicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xzc.buyipicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xzc.buyipicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xzc.buyipicturebackend.model.vo.spaceuser.SpaceUserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author XZC
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-06-12 21:04:13
 */
public interface SpaceUserService extends IService<SpaceUser> {
    /**
     * 添加空间用户
     *
     * @param spaceAddRequest SpaceUserAddRequest
     * @return spaceUser.getId
     */
    long addSpaceUser(SpaceUserAddRequest spaceAddRequest);

    /**
     * 新增或编辑时校验对象
     *
     * @param spaceUser SpaceUser
     * @param add       是否新建
     */
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    /**
     * 转换查询QueryWrapper
     *
     * @param spaceUserQueryRequest SpaceUserQueryRequest
     * @return QueryWrapper<SpaceUser>
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 获取SpaceUserVo封装结果
     *
     * @param spaceUser SpaceUser
     * @param request   HttpServletRequest
     * @return SpaceUserVo
     */
    SpaceUserVo getSpaceUserVo(SpaceUser spaceUser, HttpServletRequest request);

    /**
     * 获取SpaceUserVo封装结果列表
     *
     * @param spaceUserList List<SpaceUser>
     * @return List<SpaceUserVo>
     */
    List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUserList);
}
