package com.xzc.buyipicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzc.buyipicturebackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import com.xzc.buyipicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xzc.buyipicturebackend.api.aliyun.model.CreatePictureOutPaintingTaskRequest;
import com.xzc.buyipicturebackend.model.dto.*;
import com.xzc.buyipicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.PictureVo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * @author xuzhichao
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2024-12-31 16:05:11
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片（可重新上传）（重新上传时，暂为直接在云中多上传一张图片，旧图片保留了，暂未删除）
     * 用户和管理员皆可上传
     * 用户上传图片（上传并提交修改图片相关信息）时，审核状态改为待审核
     * 管理员上传图片时，自动过审
     *
     * @param inputSource          输入源（本地文件或url）
     * @param pictureUploadRequest 上传图片后需填写图片信息
     * @param loginUser            登录用户
     * @return PictureVO（脱敏）
     */
    PictureVo uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    /**
     * 下载图片
     *
     * @param filepath 图片路径
     * @param response HttpServletResponse
     * @throws IOException IOException
     */
    void downloadPicture(String filepath, HttpServletResponse response) throws IOException;

    /**
     * 构造图片查询请求QueryWrapper
     *
     * @param pictureQueryRequest PictureQueryRequest
     * @return QueryWrapper<Picture>
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单个图片的封装
     *
     * @param picture Picture
     * @param request HttpServletRequest
     * @return PictureVO
     */
    PictureVo getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     * Page<Picture> --> Page<PictureVO>
     *
     * @param picturePage Page<Picture>
     * @param request     HttpServletRequest
     * @return Page<PictureVO>
     */
    Page<PictureVo> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest request);

    /**
     * 图片数据（id,url,简介）校验
     * 用于更新和修改图片时进行判断
     *
     * @param picture 图片
     */
    void validPicture(Picture picture);

    /**
     * 图片审核（管理员）
     *
     * @param pictureReviewRequest PictureReviewRequest管理员进行审核发送到后端的请求
     * @param loginUser            登录用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    /**
     * 用户或管理员上传图片时，修改审核图片的审核状态
     * 填充审核参数（审核状态，审核员Id，审核信息，审核时间）
     *
     * @param picture   Picture
     * @param loginUser User
     * @param isEdit    是否为修改（供显示审核信息中修改和上传的不同）
     */
    void fillReviewParams(Picture picture, User loginUser, Boolean isEdit);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量抓取图片请求
     * @param loginUser                   登录用户
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    /**
     * 从缓存中读取图片VOs
     * 本地缓存-> redis缓存 -> 数据库
     *
     * @param pictureQueryRequest PictureQueryRequest
     * @param request             HttpServletRequest
     * @return Page<PictureVO>
     */
    Page<PictureVo> getDataFromCacheOrDb(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 删除包含本地缓存和redis缓存在内的所有缓存内容
     */
    void deleteAllCache();

    /**
     * 删除图片
     *
     * @param pictureId 图片id
     * @param loginUser User
     */
    void deletePicture(long pictureId, User loginUser);

    /**
     * 删除图片在cos中的存储文件
     * 异步执行
     *
     * @param oldPicture 图片
     */
    void deletePictureFile(Picture oldPicture);

    /**
     * 删除图片校验权限
     * 公共图库的图片，上传者和管理员能删除
     * 私有空间的图片仅拥有者能够删除，管理员也不能私自删除
     *
     * @param loginUser User
     * @param picture   Picture
     */
    void checkPictureAuth(User loginUser, Picture picture);

    /**
     * 编辑图片
     * 公共图库的图片管理员和本人可编辑
     * 私有图库的图片仅本人可编辑
     *
     * @param pictureEditRequest PictureEditRequest
     * @param loginUser          User
     */
    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    /**
     * 个人空间下根据主色调搜索图片
     *
     * @param spaceId   空间id
     * @param picColor  色调（十六进制）
     * @param loginUser 用户
     * @return List<PictureVo>
     */
    List<PictureVo> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    /**
     * 批量修改图片分类或标签或名称
     *
     * @param pictureEditByBatchRequest pictureEditByBatchRequest
     * @param loginUser                 用户
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建AI扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建任务请求
     * @param loginUer                            用户
     * @return 请求响应
     */
    CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUer);
}
