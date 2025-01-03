package com.xzc.buyipicturebackend.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzc.buyipicturebackend.annotation.AuthCheck;
import com.xzc.buyipicturebackend.common.BaseResponse;
import com.xzc.buyipicturebackend.common.ResultUtils;
import com.xzc.buyipicturebackend.constant.UserConstant;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.model.dto.*;
import com.xzc.buyipicturebackend.model.entity.Picture;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.enums.PictureReviewStatusEnum;
import com.xzc.buyipicturebackend.model.vo.PictureTagCategory;
import com.xzc.buyipicturebackend.model.vo.PictureVO;
import com.xzc.buyipicturebackend.service.PictureService;
import com.xzc.buyipicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 图片 控制层
 *
 * @author xuzhichao
 * @since 2024-12-31
 */
@Slf4j
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Resource
    private UserService userService;

    @Resource
    private PictureService pictureService;

    /**
     * 上传图片（可重新上传）（重新上传时，暂为直接在云中多上传一张图片，旧图片保留了，暂未删除）
     * 用户和管理员皆可上传
     * 用户上传图片（上传并提交修改图片相关信息）时，审核状态改为待审核
     * 管理员上传图片时，自动过审
     *
     * @param multipartFile        图片文件
     * @param pictureUploadRequest 上传图片后需填写图片信息
     * @param request              HttpServletRequest
     * @return PictureVO（脱敏）
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        // 新增或更新图片
        User loginUser = userService.getLoginUser(request);
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 图片下载
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @GetMapping("/download")
    public void downloadPicture(String filepath, HttpServletResponse response) throws IOException {
        //下载图片
        pictureService.downloadPicture(filepath, response);
    }

    /**
     * 删除图片（图片上传者为本人或管理员可删除）
     *
     * @param deleteRequest DeleteRequest
     * @param request       HttpServletRequest
     * @return Boolean
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        Long id = deleteRequest.getId();

        // 判断图片是否存在
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 仅本人或管理员可删除
        if (!picture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        //TODO 后续改为在用户的“我的”里，集中删除
        // 删除
        boolean result = pictureService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "删除失败，数据库错误");
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     *
     * @param pictureUpdateRequest PictureUpdateRequest
     * @return Boolean
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);

        // DTO转换实体类
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

        // 校验图片id,url,简介
        pictureService.validPicture(picture);

        // 判断图片是否存在
        Picture oldPicture = pictureService.getById(picture.getId());
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        // 补充审核参数（管理员更新图片，自动过审）
        User loginUser = userService.getLoginUser(request);
        pictureService.fillReviewParams(picture, loginUser, true);

        // 更新
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新失败，数据库错误");

        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     *
     * @param id      图片id
     * @param request HttpServletRequest
     * @return Picture图片（未脱敏）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     *
     * @param id      图片id
     * @param request HttpServletRequest
     * @return PictureVO（脱敏）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVoById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture, request));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     *
     * @param pictureQueryRequest PictureQueryRequest
     * @return Page<Picture>（未脱敏）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）（用户）
     *
     * @param pictureQueryRequest PictureQueryRequest
     * @param request             HttpServletRequest
     * @return Page<PictureVO>（脱敏）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVoByPage(@RequestBody PictureQueryRequest pictureQueryRequest
            , HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVoPage(picturePage, request));
    }

    /**
     * 编辑图片（给用户使用，管理员也会在管理页面使用）
     * 上传时可填写图片相关信息
     *
     * @param pictureEditRequest PictureEditRequest
     * @param request            HttpServletRequest
     * @return Boolean
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest
            , HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // DTO转换实体类
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        picture.setEditTime(new Date());

        // 数据校验
        pictureService.validPicture(picture);

        // 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);

        //TODO 后续改为在用户的“我的”里，集中编辑
        // 仅本人或管理员可编辑
        User loginUser = userService.getLoginUser(request);
        if (!oldPicture.getUserId().equals(loginUser.getId()) || !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 补充审核参数
        pictureService.fillReviewParams(picture, loginUser, true);

        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "编辑失败，数据库错误");

        return ResultUtils.success(true);
    }

    /**
     * 获取一些标签和分类
     * 目前暂时写死
     *
     * @return PictureTagCategory（标签列表和分类列表，以供展示给用户）
     */
    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "高清壁纸", "头像", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("壁纸", "头像", "模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    /**
     * 图片审核（管理员）
     * 管理员对用户上传的图片进行审核
     *
     * @param pictureReviewRequest PictureReviewRequest 要审核的图片id，状态，审核信息
     * @param request              HttpServletRequest
     * @return Boolean
     */
    @PostMapping("/review")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> doPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        return ResultUtils.success(true);
    }


}
