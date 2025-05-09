package com.xzc.buyipicturebackend.service;

import com.xzc.buyipicturebackend.model.dto.space.analyze.*;
import com.xzc.buyipicturebackend.model.entity.Space;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.space.analyze.*;

import java.util.List;

/**
 * @author xuzhichao
 * @description 空间分析服务类
 * @createDate 2025-05-06 16:50:13
 */
public interface SpaceAnalyzeService {

    /**
     * 空间资源使用分析，对空间资源的使用情况进行分析
     *
     * @param spaceUsageAnalyzeRequest 空间资源使用分析请求
     * @param loginUser                登录用户
     * @return 分析结果
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 空间图片的分类分析
     *
     * @param spaceCategoryAnalyzeRequest 空间图片分类分析请求
     * @param loginUser                   登录用户
     * @return 分析结果
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 空间图片的标签分析
     *
     * @param spaceTagAnalyzeRequest 空间图片标签分析请求
     * @param loginUser              登录用户
     * @return 分析结果
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 空间图片的大小范围与对应数量分析
     *
     * @param spaceSizeAnalyzeRequest 空间图片大小范围与对应数量的分析请求类
     * @param loginUser               登录用户
     * @return 分析结果
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 空间中用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest 空间中用户上传行为分析请求类
     * @param loginUser               登录用户
     * @return 分析结果
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    /**
     * 空间使用排行分析
     *
     * @param spaceRankAnalyzeRequest 空间使用排行分析请求类
     * @param loginUser               登录用户
     * @return 分析结果
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
