package com.xzc.buyipicturebackend.controller;

import com.xzc.buyipicturebackend.common.BaseResponse;
import com.xzc.buyipicturebackend.common.ResultUtils;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.model.dto.space.analyze.*;
import com.xzc.buyipicturebackend.model.entity.Space;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.space.analyze.*;
import com.xzc.buyipicturebackend.service.SpaceAnalyzeService;
import com.xzc.buyipicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 空间分析 控制层
 *
 * @author xuzhichao
 * @since 2025-01-10
 */
@Slf4j
@RestController
@RequestMapping("/space/analyze")
public class SpaceAnalyzeController {

    @Resource
    private UserService userService;

    @Resource
    private SpaceAnalyzeService spaceAnalyzeService;

    /**
     * 空间资源使用情况分析（获取空间使用情况）
     *
     * @param spaceUsageAnalyzeRequest 空间资源使用分析请求
     * @param request                  HttpServletRequest
     * @return 分析结果
     */
    @PostMapping("/usage")
    public BaseResponse<SpaceUsageAnalyzeResponse> getSpaceUsageAnalyze(
            @RequestBody SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser));
    }

    /**
     * 空间图片分类分析
     *
     * @param spaceCategoryAnalyzeRequest 空间图片分类分析请求
     * @param request                     HttpServletRequest
     * @return 分析结果
     */
    @PostMapping("/category")
    public BaseResponse<List<SpaceCategoryAnalyzeResponse>> getSpaceCategoryAnalyze(
            @RequestBody SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser));
    }

    /**
     * 空间图片标签分析
     *
     * @param spaceTagAnalyzeRequest 空间图片标签分析请求
     * @param request                HttpServletRequest
     * @return 分析结果
     */
    @PostMapping("/tag")
    public BaseResponse<List<SpaceTagAnalyzeResponse>> getSpaceTagAnalyze(
            @RequestBody SpaceTagAnalyzeRequest spaceTagAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser));
    }

    /**
     * 空间图片大小范围与对应数量的分析
     *
     * @param spaceSizeAnalyzeRequest 空间图片大小范围与对应数量分析请求
     * @param request                 HttpServletRequest
     * @return 分析结果
     */
    @PostMapping("/size")
    public BaseResponse<List<SpaceSizeAnalyzeResponse>> getSpaceSizeAnalyze(
            @RequestBody SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser));
    }

    /**
     * 空间中用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest 空间中用户上传行为分析请求
     * @param request                 HttpServletRequest
     * @return 分析结果
     */
    @PostMapping("/user")
    public BaseResponse<List<SpaceUserAnalyzeResponse>> getSpaceUserAnalyze(
            @RequestBody SpaceUserAnalyzeRequest spaceUserAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser));
    }

    /**
     * 空间使用排行分析
     *
     * @param spaceRankAnalyzeRequest 空间使用排行分析请求
     * @param request                 HttpServletRequest
     * @return 分析结果
     */
    @PostMapping("/rank")
    public BaseResponse<List<Space>> getSpaceRankAnalyze(
            @RequestBody SpaceRankAnalyzeRequest spaceRankAnalyzeRequest
            , HttpServletRequest request) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(spaceAnalyzeService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser));
    }
}
