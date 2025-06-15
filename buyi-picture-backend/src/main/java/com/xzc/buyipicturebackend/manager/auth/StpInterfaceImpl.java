package com.xzc.buyipicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.xzc.buyipicturebackend.constant.UserConstant;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.manager.auth.model.SpaceUserAuthContext;
import com.xzc.buyipicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xzc.buyipicturebackend.model.entity.Picture;
import com.xzc.buyipicturebackend.model.entity.Space;
import com.xzc.buyipicturebackend.model.entity.SpaceUser;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.enums.SpaceRoleEnum;
import com.xzc.buyipicturebackend.model.enums.SpaceTypeEnum;
import com.xzc.buyipicturebackend.service.PictureService;
import com.xzc.buyipicturebackend.service.SpaceService;
import com.xzc.buyipicturebackend.service.SpaceUserService;
import com.xzc.buyipicturebackend.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 自定义权限加载接口实现类
 * 根据登录用户id获取到用户已有的角色和权限列表，从而进行权限比对
 *
 * @author: Samoyer
 * @date: 2025-06-15
 */
public class StpInterfaceImpl implements StpInterface {

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @Resource
    private SpaceUserService spaceUserService;

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    /**
     * 返回一个账号所拥有的权限码集合
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        //1.仅对类型为space进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<String>();
        }

        //2.使用管理员权限来表示校验通过
        List<String> ADMIN_PERMISSIONS = spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());

        //3.获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        //如果所有字段均为空，则表示查询的是公共图库，通过(返回管理员权限)
        if (isAllFieldsNull(authContext)) {
            return ADMIN_PERMISSIONS;
        }

        //4.判断是否登录
        //获取登录时存入在session中的userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        //未登录抛异常
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR, "请先登录");
        //取userId备用
        Long userId = loginUser.getId();

        //5.非以上情况，优先从上下文中获取SpaceUser对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        //如果存在，则直接根据角色获取权限列表
        if (spaceUser != null) {
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }

        //6.非以上情况，先看spaceUserId，如果有则表明是团队空间，则查询SpaceUser对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            ThrowUtils.throwIf(spaceUser == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间成员信息");
            //查询当前登录用户（spaceUser）是否属于该团队空间，上一步只查了表中是否有当前用户作为成员的数据记录
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    //所需要操作的空间
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    //当前登录用户的userId
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            //若不属于该团队空间，返回空权限列表
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }

            //非以上情况，则登录用户属于该团队空间，则查询并返回相应的权限
            return spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole());
        }

        //7.非以上情况，则通过spaceId或pictureId查询
        Long spaceId = authContext.getSpaceId();
        //无spaceId
        if (spaceId == null) {
            //无spaceId，则看pictureId
            Long pictureId = authContext.getPictureId();
            //pictureId也没有，则默认通过校验
            if (pictureId == null) {
                return ADMIN_PERMISSIONS;
            }
            //有pictureId，查出picture，再根据图片的spaceId继续查询
            Picture picture = pictureService.lambdaQuery()
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .eq(Picture::getId, pictureId)
                    .one();
            ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            //查到了picture，再根据图片的spaceId继续查询
            spaceId = picture.getSpaceId();
            //spaceId为空，则为公共图库，仅本人或管理员可操作
            if (spaceId == null) {
                if (picture.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                    return ADMIN_PERMISSIONS;
                } else {
                    //不是自己的图片，返回仅可查看的权限列表
                    return List.of(SpaceUserPermissionConstant.PICTURE_VIEW);
                }
            }
        }
        //有spaceId
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "未找到空间");
        //判断空间类型
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            //私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(userId) || userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            //团队空间,查询出SpaceUser,获取其权限
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, userId)
                    .one();
            //登录用户不属于该空间
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            //属于，返回对应权限
            return spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole());
        }
    }

    /**
     * 暂不使用。返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return new ArrayList<String>();
    }

    /**
     * 从请求中获取上下文对象
     *
     * @return 表示用户在特定空间内的授权上下文，包括关联的图片、空间和用户信息
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authContext;

        //兼容post和get
        if (ContentType.JSON.getValue().equals(contentType)) {
            //post，从body中获取参数
            String body = ServletUtil.getBody(request);
            authContext = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            //get，从请求参数中获取
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authContext = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }

        //根据请求路径区分id字段的含义
        Long id = authContext.getId();
        if (ObjUtil.isNotNull(id)) {
            // "api/picture/..."，取出picture
            String requestUri = request.getRequestURI();
            String partUri = requestUri.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authContext.setPictureId(id);
                    break;
                case "space":
                    authContext.setSpaceId(id);
                    break;
                case "spaceUser":
                    authContext.setSpaceUserId(id);
                    break;
                default:
            }
        }

        return authContext;
    }

    /**
     * 判断对象内的字段是否全部为空
     *
     * @param object 对象
     * @return 是否全部为空
     */
    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true;
        }
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                .map(field -> ReflectUtil.getFieldValue(object, field))
                .allMatch(ObjUtil::isEmpty);
    }
}
