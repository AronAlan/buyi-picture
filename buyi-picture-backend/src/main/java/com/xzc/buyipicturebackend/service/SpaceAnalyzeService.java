package com.xzc.buyipicturebackend.service;

import com.xzc.buyipicturebackend.model.dto.space.analyze.SpaceUsageAnalyzeRequest;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.space.analyze.SpaceUsageAnalyzeResponse;

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

}
