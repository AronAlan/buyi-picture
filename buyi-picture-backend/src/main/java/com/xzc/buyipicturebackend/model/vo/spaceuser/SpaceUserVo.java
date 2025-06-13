package com.xzc.buyipicturebackend.model.vo.spaceuser;

import com.xzc.buyipicturebackend.model.entity.SpaceUser;
import com.xzc.buyipicturebackend.model.vo.space.SpaceVo;
import com.xzc.buyipicturebackend.model.vo.user.UserVo;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.Date;

/**
 * 空间成员的VO包装类
 *
 * @author: Samoyer
 * @date: 2025-06-12
 */
@Data
public class SpaceUserVo implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 空间角色：viewer/editor/admin
     */
    private String spaceRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 用户信息
     */
    private UserVo user;

    /**
     * 空间信息
     */
    private SpaceVo space;

    private static final long serialVersionUID = 1L;

    /**
     * 对象转VO类
     *
     * @param spaceUser
     * @return
     */
    public static SpaceUserVo objToVo(SpaceUser spaceUser) {
        if (spaceUser == null) {
            return null;
        }

        SpaceUserVo spaceUserVo = new SpaceUserVo();
        BeanUtils.copyProperties(spaceUser, spaceUserVo);
        return spaceUserVo;
    }

    /**
     * VO类转对象
     *
     * @param spaceUserVO
     * @return
     */
    public static SpaceUser voToObj(SpaceUserVo spaceUserVO) {
        if (spaceUserVO == null) {
            return null;
        }

        SpaceUser spaceUser = new SpaceUser();
        BeanUtils.copyProperties(spaceUserVO, spaceUser);
        return spaceUser;
    }
}
