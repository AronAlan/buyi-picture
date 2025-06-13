package com.xzc.buyipicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.mapper.SpaceUserMapper;
import com.xzc.buyipicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.xzc.buyipicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.xzc.buyipicturebackend.model.entity.Space;
import com.xzc.buyipicturebackend.model.entity.SpaceUser;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.enums.SpaceRoleEnum;
import com.xzc.buyipicturebackend.model.vo.space.SpaceVo;
import com.xzc.buyipicturebackend.model.vo.spaceuser.SpaceUserVo;
import com.xzc.buyipicturebackend.model.vo.user.UserVo;
import com.xzc.buyipicturebackend.service.SpaceService;
import com.xzc.buyipicturebackend.service.SpaceUserService;
import com.xzc.buyipicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author XZC
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service实现
 * @createDate 2025-06-12 21:04:13
 */
@Service
public class SpaceUserServiceImpl extends ServiceImpl<SpaceUserMapper, SpaceUser>
        implements SpaceUserService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 添加空间用户
     *
     * @param spaceUserAddRequest SpaceUserAddRequest
     * @return spaceUser.getId
     */
    @Override
    public long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest) {
        //参数校验
        ThrowUtils.throwIf(spaceUserAddRequest == null, ErrorCode.PARAMS_ERROR);
        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserAddRequest, spaceUser);
        validSpaceUser(spaceUser, true);

        //前端未传，则默认普通浏览者
        if (spaceUser.getSpaceRole() == null) {
            spaceUser.setSpaceRole(SpaceRoleEnum.VIEWER.getValue());
        }

        //插入
        boolean result = this.save(spaceUser);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "新增空间用户失败");
        return spaceUser.getId();
    }

    /**
     * 新增或编辑时校验对象
     *
     * @param spaceUser SpaceUser
     * @param add       是否新建
     */
    @Override
    public void validSpaceUser(SpaceUser spaceUser, boolean add) {
        ThrowUtils.throwIf(spaceUser == null, ErrorCode.PARAMS_ERROR);
        //新增
        Long spaceId = spaceUser.getSpaceId();
        Long userId = spaceUser.getUserId();
        if (add) {
            ThrowUtils.throwIf(ObjUtil.hasEmpty(spaceId, userId), ErrorCode.PARAMS_ERROR);
            ThrowUtils.throwIf(userService.getById(userId) == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在");
            ThrowUtils.throwIf(spaceService.getById(spaceId) == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            //若用户已经加入该空间，则失败
            SpaceUser existUser = this.getOne(
                    new QueryWrapper<SpaceUser>()
                            .eq("spaceId", spaceId)
                            .eq("userId", userId)
            );
            ThrowUtils.throwIf(existUser != null, ErrorCode.OPERATION_ERROR, "用户已经加入该空间");

        }

        //校验空间角色
        String spaceRole = spaceUser.getSpaceRole();
        SpaceRoleEnum spaceRoleEnum = SpaceRoleEnum.getEnumByValue(spaceRole);
        ThrowUtils.throwIf(spaceRoleEnum == null, ErrorCode.PARAMS_ERROR, "空间角色不存在");
    }

    /**
     * 转换查询QueryWrapper
     *
     * @param spaceUserQueryRequest SpaceUserQueryRequest
     * @return QueryWrapper<SpaceUser>
     */
    @Override
    public QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest) {
        QueryWrapper<SpaceUser> queryWrapper = new QueryWrapper<>();
        if (spaceUserQueryRequest == null) {
            return queryWrapper;
        }

        Long id = spaceUserQueryRequest.getId();
        Long spaceId = spaceUserQueryRequest.getSpaceId();
        Long userId = spaceUserQueryRequest.getUserId();
        String spaceRole = spaceUserQueryRequest.getSpaceRole();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(spaceId != null, "spaceId", spaceId);
        queryWrapper.eq(userId != null, "userId", userId);
        queryWrapper.eq(StrUtil.isNotBlank(spaceRole), "spaceRole", spaceRole);

        return queryWrapper;
    }

    /**
     * 获取SpaceUserVo封装结果
     *
     * @param spaceUser SpaceUser
     * @param request   HttpServletRequest
     * @return SpaceUserVo
     */
    @Override
    public SpaceUserVo getSpaceUserVo(SpaceUser spaceUser, HttpServletRequest request) {
        SpaceUserVo spaceUserVo = SpaceUserVo.objToVo(spaceUser);
        //关联查询用户信息(这里查询的可能是成员)
        Long userId = spaceUser.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVo userVo = userService.getUserVo(user);
            spaceUserVo.setUser(userVo);
        }
        //关联查询空间信息（getSpaceVo中查询的user是空间创建者）
        Long spaceId = spaceUser.getSpaceId();
        if (spaceId != null && spaceId > 0) {
            Space space = spaceService.getById(spaceId);
            SpaceVo spaceVo = spaceService.getSpaceVo(space, request);
            spaceUserVo.setSpace(spaceVo);
        }

        return spaceUserVo;
    }

    /**
     * 获取SpaceUserVo封装结果列表
     *
     * @param spaceUserList List<SpaceUser>
     * @return List<SpaceUserVo>
     */
    @Override
    public List<SpaceUserVo> getSpaceUserVoList(List<SpaceUser> spaceUserList) {
        if (CollUtil.isEmpty(spaceUserList)) {
            return Collections.emptyList();
        }

        //1.需要查询的用户ID和空间ID
        Set<Long> userIdSet = spaceUserList.stream().map(SpaceUser::getUserId).collect(Collectors.toSet());
        Set<Long> spaceIdSet = spaceUserList.stream().map(SpaceUser::getSpaceId).collect(Collectors.toSet());
        //2.批量查询用户和空间
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        Map<Long, List<Space>> spaceIdSpaceListMap = spaceService.listByIds(spaceIdSet).stream().collect(Collectors.groupingBy(Space::getId));

        //3.填充vo的用户和空间信息
        //对象列表->VO列表
        List<SpaceUserVo> spaceUserVoList = spaceUserList.stream().map(SpaceUserVo::objToVo).collect(Collectors.toList());
        spaceUserVoList.forEach(spaceUserVO -> {
            Long spaceId = spaceUserVO.getSpaceId();
            Long userId = spaceUserVO.getUserId();

            Space space = null;
            if (spaceIdSpaceListMap.containsKey(spaceId)) {
                space = spaceIdSpaceListMap.get(spaceId).get(0);
            }
            spaceUserVO.setSpace(SpaceVo.objToVo(space));
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceUserVO.setUser(UserVo.objToVo(user));
        });

        return spaceUserVoList;
    }


}




