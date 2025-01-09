package com.xzc.buyipicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzc.buyipicturebackend.model.dto.PictureQueryRequest;
import com.xzc.buyipicturebackend.model.dto.PictureReviewRequest;
import com.xzc.buyipicturebackend.model.dto.PictureUploadByBatchRequest;
import com.xzc.buyipicturebackend.model.dto.PictureUploadRequest;
import com.xzc.buyipicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
    PictureVO uploadPicture(Object inputSource,
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
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 分页获取图片封装
     * Page<Picture> --> Page<PictureVO>
     *
     * @param picturePage Page<Picture>
     * @param request     HttpServletRequest
     * @return Page<PictureVO>
     */
    Page<PictureVO> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest request);

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
    Page<PictureVO> getDataFromCacheOrDb(PictureQueryRequest pictureQueryRequest, HttpServletRequest request);

    /**
     * 删除包含本地缓存和redis缓存在内的所有缓存内容
     */
    void deleteAllCache();

    /**
     * 删除图片在cos中的存储文件
     * 异步执行
     *
     * @param oldPicture 图片
     */
    void deletePictureFile(Picture oldPicture);
}
