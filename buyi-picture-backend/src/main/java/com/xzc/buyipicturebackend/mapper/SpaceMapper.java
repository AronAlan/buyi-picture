package com.xzc.buyipicturebackend.mapper;

import com.xzc.buyipicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xzc.buyipicturebackend.model.entity.SpaceUser;
import org.apache.ibatis.annotations.Insert;

/**
 * @author xuzhichao
 * @description 针对表【space(空间)】的数据库操作Mapper
 * @createDate 2025-01-10 14:58:13
 * @Entity com.xzc.buyipicturebackend.model.entity.Space
 */
public interface SpaceMapper extends BaseMapper<Space> {

    /**
     * 插入成员与团队空间的关联记录
     * SpaceService中创建团队空间时需插入创建者与空间的关联，防止与SpaceUserService循环依赖
     *
     * @param spaceUser SpaceUser
     * @return boolean
     */
    boolean insertSpaceUser(SpaceUser spaceUser);
}




