package com.xzc.buyipicturebackend.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xzc.buyipicturebackend.annotation.AuthCheck;
import com.xzc.buyipicturebackend.api.aliyun.AliyunAiApi;
import com.xzc.buyipicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xzc.buyipicturebackend.api.aliyun.model.CreatePictureOutPaintingTaskRequest;
import com.xzc.buyipicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.xzc.buyipicturebackend.api.imagesearch.ImageSearchApiFacade;
import com.xzc.buyipicturebackend.api.imagesearch.model.ImageSearchResult;
import com.xzc.buyipicturebackend.common.BaseResponse;
import com.xzc.buyipicturebackend.common.ResultUtils;
import com.xzc.buyipicturebackend.constant.UserConstant;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.manager.auth.SpaceUserAuthManager;
import com.xzc.buyipicturebackend.manager.auth.StpKit;
import com.xzc.buyipicturebackend.manager.auth.annotation.SaSpaceCheckPermission;
import com.xzc.buyipicturebackend.manager.auth.model.SpaceUserPermissionConstant;
import com.xzc.buyipicturebackend.model.dto.*;
import com.xzc.buyipicturebackend.model.dto.picture.*;
import com.xzc.buyipicturebackend.model.entity.Picture;
import com.xzc.buyipicturebackend.model.entity.Space;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.enums.PictureReviewStatusEnum;
import com.xzc.buyipicturebackend.model.vo.picture.PictureTagCategory;
import com.xzc.buyipicturebackend.model.vo.picture.PictureVo;
import com.xzc.buyipicturebackend.service.PictureService;
import com.xzc.buyipicturebackend.service.SpaceService;
import com.xzc.buyipicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

    @Resource
    private SpaceService spaceService;

    @Resource
    private AliyunAiApi aliyunAiApi;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    /**
     * 上传本地图片（可重新上传）（重新上传时，暂为直接在云中多上传一张图片，旧图片保留了，暂未删除）
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
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVo> uploadPicture(@RequestPart("file") MultipartFile multipartFile
            , PictureUploadRequest pictureUploadRequest
            , HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        // 1.如果是上传到私有空间
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 必须空间创建人（管理员）才能上传
            if (!loginUser.getId().equals(space.getUserId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
            }
        }

        // 2.新增或更新图片
        PictureVo pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传url图片（可重新上传）
     *
     * @param pictureUploadRequest 上传图片后需填写图片信息
     * @param request              HttpServletRequest
     * @return PictureVO（脱敏）
     */
    @PostMapping("/upload/url")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVo> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest
            , HttpServletRequest request) {
        // 新增或更新图片
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVo pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
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
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        // 操作数据库。删COS资源文件
        pictureService.deletePicture(deleteRequest.getId(), userService.getLoginUser(request));
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
     * 编辑图片（给用户使用，管理员也会在管理页面使用）
     * 上传时可填写图片相关信息
     *
     * @param pictureEditRequest PictureEditRequest
     * @param request            HttpServletRequest
     * @return Boolean
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest
            , HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        pictureService.editPicture(pictureEditRequest, userService.getLoginUser(request));

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
    public BaseResponse<PictureVo> getPictureVoById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        // 如果为私有空间图片，仅本人可查看
        //if (picture.getSpaceId() != null) {
        //    pictureService.checkPictureAuth(userService.getLoginUser(request), picture);
        //}
        //改为权限列表鉴权
        Space space =null;
        Long spaceId = picture.getSpaceId();
        if (spaceId!=null){
            boolean permission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!permission,ErrorCode.NO_AUTH_ERROR,"仅该空间的本人可查看此图片");
            space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space==null,ErrorCode.NOT_FOUND_ERROR,"空间不存在");
        }
        //获取权限列表，供前端展示出操作按钮
        User loginUser = userService.getLoginUser(request);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space, loginUser);
        PictureVo pictureVo = pictureService.getPictureVO(picture, request);
        pictureVo.setPermissionList(permissionList);

        // 获取封装类
        return ResultUtils.success(pictureVo);
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
     * 分页获取图片列表（封装类）（用户）（公共图库）
     *
     * @param pictureQueryRequest PictureQueryRequest
     * @param request             HttpServletRequest
     * @return Page<PictureVO>（脱敏）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVo>> listPictureVoByPage(@RequestBody PictureQueryRequest pictureQueryRequest
            , HttpServletRequest request) {
        // 限制爬虫
        ThrowUtils.throwIf(pictureQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR);
        // 查看公共图库的接口，不允许查看私有空间
        ThrowUtils.throwIf(pictureQueryRequest.getSpaceId() != null, ErrorCode.PARAMS_ERROR);

        // 普通用户默认只能查看已过审的数据
        pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
        // 只查看没有spaceId（公共图库）的图片
        pictureQueryRequest.setNullSpaceId(true);

        // 本地缓存-> redis缓存 -> 数据库
        return ResultUtils.success(pictureService.getDataFromCacheOrDb(pictureQueryRequest, request));
    }

    /**
     * 分页获取图片列表（封装类）（用户）（私有图库）
     *
     * @param pictureQueryRequest PictureQueryRequest
     * @param request             HttpServletRequest
     * @return Page<PictureVO>（脱敏）
     */
    @PostMapping("/list/space/page/vo")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.SPACE_USER_MANAGE)
    public BaseResponse<Page<PictureVo>> listSpacePictureVoByPage(@RequestBody PictureQueryRequest pictureQueryRequest
            , HttpServletRequest request) {
        // 查看私有空间的接口，必须有spaceId
        Long spaceId = pictureQueryRequest.getSpaceId();
        ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "图库空间不存在");

        //if (!userService.getLoginUser(request).getId().equals(space.getUserId())) {
        //    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有权限查看不属于您的私有空间");
        //}

        pictureQueryRequest.setSpaceId(spaceId);
        pictureQueryRequest.setNullSpaceId(false);
        // 查询数据库。私有空间更新频率无法确定，不设置缓存
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size)
                , pictureService.getQueryWrapper(pictureQueryRequest));
        Page<PictureVo> pictureVoPage = pictureService.getPictureVoPage(picturePage, request);
        return ResultUtils.success(pictureVoPage);
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
        // 标签
        List<String> tagList = Arrays.asList("热门", "高清壁纸", "头像", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        // 分类
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

    /**
     * 批量创建（抓取）图片
     *
     * @param pictureUploadByBatchRequest 批量抓取图片请求（关键词，条数，名称前缀）
     * @param request                     HttpServletRequest
     * @return 上传成功的图片数量
     */
    @PostMapping("/upload/batch")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> uploadPictureByBatch(
            @RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,
            HttpServletRequest request
    ) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest, loginUser);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 手动刷新缓存
     *
     * @param request HttpServletRequest
     * @return true
     */
    @GetMapping("/refresh/cache")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> refreshCache(HttpServletRequest request) {
        pictureService.deleteAllCache();
        return ResultUtils.success(true);
    }

    /**
     * 以图搜图
     *
     * @param searchPictureByPictureRequest 图片id请求
     * @return List<ImageSearchResult>
     */
    @PostMapping("/search/picture")
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest) {
        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);
        Long pictureId = searchPictureByPictureRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null || pictureId <= 0, ErrorCode.PARAMS_ERROR);
        Picture picture = pictureService.getById(pictureId);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        List<ImageSearchResult> resultList = new ArrayList<>();
        //360搜图以start控制查询页数。本平台支持查询50条
        int start = 0;
        while (resultList.size() <= 50) {
            List<ImageSearchResult> result = ImageSearchApiFacade.searchImages(picture.getUrl(), start);
            if (result.isEmpty()) {
                break;
            }
            resultList.addAll(result);
            start += result.size();
        }
        return ResultUtils.success(resultList);
    }

    /**
     * 以色调搜图
     *
     * @param searchPictureByColorRequest 以色调搜图请求（色调，空间id）
     * @return List<PictureVo>
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVo>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVo> pictureVos = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVos);
    }

    /**
     * 批量编辑图片（分类或标签或名称）
     *
     * @param pictureEditByBatchRequest 批量编辑图片请求
     * @return true
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 创建AI扩图任务
     *
     * @param createPictureOutPaintingTaskRequest 创建AI扩图任务请求
     * @return 创建结果（响应）
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null
                || createPictureOutPaintingTaskRequest.getPictureId() <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest, loginUser);
        return ResultUtils.success(response);
    }

    /**
     * 查询AI扩图任务信息或详细情况
     *
     * @param taskId 查询的任务id
     * @return 查询结果（响应）
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "查询任务id不能为空");
        GetOutPaintingTaskResponse response = aliyunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(response);
    }

}
