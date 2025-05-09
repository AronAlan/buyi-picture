package com.xzc.buyipicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.model.dto.space.analyze.*;
import com.xzc.buyipicturebackend.model.entity.Picture;
import com.xzc.buyipicturebackend.model.entity.Space;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.space.analyze.*;
import com.xzc.buyipicturebackend.service.PictureService;
import com.xzc.buyipicturebackend.service.SpaceAnalyzeService;
import com.xzc.buyipicturebackend.service.SpaceService;
import com.xzc.buyipicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xuzhichao
 * @description 空间分析服务实现类
 * @createDate 2025-05-06 16:50:13
 */
@Service
public class SpaceAnalyzeServiceImpl implements SpaceAnalyzeService {

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    /**
     * 校验空间分析的权限
     *
     * @param spaceAnalyzeRequest 空间分析公共请求
     * @param loginUser           登录用户
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 如果是全空间分析或公共图库分析，则仅管理员可访问
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可进行全部或公共图库分析");
        } else {
            // 用户进行私有空间分析
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验用户权限，仅管理员和本人有权限
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 根据不同的查询范围 填充queryWrapper（加限定条件）
     *
     * @param spaceAnalyzeRequest 空间分析公共请求
     * @param queryWrapper        QueryWrapper
     */
    private void fillAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
            return;
        }

        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    /**
     * 空间资源使用分析，对空间资源的使用情况进行分析
     *
     * @param spaceUsageAnalyzeRequest 空间资源使用分析请求
     * @param loginUser                登录用户
     * @return 分析结果
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 全部或公共图库分析
        if (spaceUsageAnalyzeRequest.isQueryAll() || spaceUsageAnalyzeRequest.isQueryPublic()) {
            // 仅管理员有权限进行该操作
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可进行全部或公共图库分析");

            // 统计公共图库的资源使用
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("picSize");
            // 添加限定条件。查全部则不添加任何，查公共则spaceId为null
            fillAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, queryWrapper);

            // 直接返回Object，提升性能节约空间
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(queryWrapper);
            long usedSize = pictureObjList.stream().mapToLong(result -> result instanceof Long ? (Long) result : 0).sum();
            long usedCount = pictureObjList.size();
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            // 查全部或公共图库则无上限，无比例
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            spaceUsageAnalyzeResponse.setSizeUsageRatio(null);
            return spaceUsageAnalyzeResponse;
        }
        // 私人空间分析
        else {
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId <= 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 权限校验，仅管理员或本人可操作
            spaceService.checkSpaceAuth(loginUser, space);

            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(space.getTotalSize());
            spaceUsageAnalyzeResponse.setUsedCount(space.getTotalCount());
            spaceUsageAnalyzeResponse.setMaxSize(space.getMaxSize());
            spaceUsageAnalyzeResponse.setMaxCount(space.getMaxCount());
            spaceUsageAnalyzeResponse.setSizeUsageRatio(NumberUtil.round(space.getTotalSize() * 100.0 / space.getMaxSize(), 2).doubleValue());
            spaceUsageAnalyzeResponse.setCountUsageRatio(NumberUtil.round(space.getTotalCount() * 100.0 / space.getMaxCount(), 2).doubleValue());
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 空间图片的分类分析
     *
     * @param spaceCategoryAnalyzeRequest 空间图片分类分析请求
     * @param loginUser                   登录用户
     * @return 分析结果
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 权限校验
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);

        // 构造查询
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, queryWrapper);
        queryWrapper.select("category AS category",
                        "COUNT(*) AS count",
                        "SUM(picSize) AS totalSize")
                .groupBy("category");
        queryWrapper.eq("isDelete", 0);

        // 查询并转换结果
        return pictureService.getBaseMapper().selectMaps(queryWrapper)
                .stream()
                .map(result -> {
                    String category = result.get("category") != null ? result.get("category").toString() : "未分类";
                    Long count = ((Number) result.get("count")).longValue();
                    Long totalSize = ((Number) result.get("totalSize")).longValue();
                    return new SpaceCategoryAnalyzeResponse(category, count, totalSize);
                })
                .collect(Collectors.toList());
    }

    /**
     * 空间图片的标签分析
     *
     * @param spaceTagAnalyzeRequest 空间图片标签分析请求
     * @param loginUser              登录用户
     * @return 分析结果
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 权限校验
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);

        // 构造查询
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceTagAnalyzeRequest, queryWrapper);
        queryWrapper.select("tags")
                .eq("isDelete", 0);

        // 查询
        List<String> tagsJsonList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .filter(ObjUtil::isNotNull)
                .map(Object::toString)
                .collect(Collectors.toList());

        // 合并所有标签并统计使用次数
        Map<String, Long> tagCountMap = tagsJsonList.stream()
                .flatMap(tagsJson -> JSONUtil.toList(tagsJson, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));

        // 按关联图片数量降序排序，并转换响应对象
        return tagCountMap.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .map(entry -> new SpaceTagAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 空间图片的大小范围与对应数量分析
     *
     * @param spaceSizeAnalyzeRequest 空间图片大小范围与对应数量的分析请求类
     * @param loginUser               登录用户
     * @return 分析结果
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);

        // 权限校验
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);

        // 构造查询
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, queryWrapper);
        queryWrapper.select("picSize")
                .eq("isDelete", 0);
        // 数据库中为bigint，直接取值可能会映射为Integer，需转换为Long
        List<Long> picSizeList = pictureService.getBaseMapper().selectObjs(queryWrapper)
                .stream()
                .map(size -> ((Number) size).longValue())
                .collect(Collectors.toList());

        // 定义分段范围，使用有序Map，统计对应图片数量
        Map<String, Long> sizeRangesMapCount = new LinkedHashMap<>();
        sizeRangesMapCount.put("<100KB", picSizeList.stream().filter(size -> size < 100 * 1024).count());
        sizeRangesMapCount.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        sizeRangesMapCount.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        sizeRangesMapCount.put(">1MB", picSizeList.stream().filter(size -> size >= 1024 * 1024).count());

        return sizeRangesMapCount.entrySet().stream()
                .map(entry -> new SpaceSizeAnalyzeResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    /**
     * 空间中用户上传行为分析
     *
     * @param spaceUserAnalyzeRequest 空间中用户上传行为分析请求类
     * @param loginUser               登录用户
     * @return 分析结果
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        Long userId = spaceUserAnalyzeRequest.getUserId();
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAMS_ERROR);

        // 权限校验
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);

        // 构造查询
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        fillAnalyzeQueryWrapper(spaceUserAnalyzeRequest, queryWrapper);
        // 分析用户上传行为，不需要只查询未删除的，凡上传行为均查询
        queryWrapper.eq("userId", userId);

        // 分析维度，日周月
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        switch (timeDimension) {
            case "day":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m-%d') AS period",
                        "COUNT(*) AS count");
                break;
            case "week":
                queryWrapper.select("YEARWEEK(createTime) AS period",
                        "COUNT(*) AS count");
                break;
            case "month":
                queryWrapper.select("DATE_FORMAT(createTime,'%Y-%m') AS period",
                        "COUNT(*) AS count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持非日、周、月以外的时间维度");
        }

        // 分组排序
        queryWrapper.groupBy("period").orderByAsc("period");

        // 查询，转换
        List<Map<String, Object>> results = pictureService.getBaseMapper().selectMaps(queryWrapper);
        return results.stream()
                .map(res -> {
                    String period = res.get("period").toString();
                    Long count = ((Number) res.get("count")).longValue();
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
    }

    /**
     * 空间使用排行分析
     *
     * @param spaceRankAnalyzeRequest 空间使用排行分析请求类
     * @param loginUser               登录用户
     * @return 分析结果
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR, "仅管理员可查看空间排行");

        // 构造查询
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("id", "spaceName", "userId", "totalSize")
                .eq("isDelete", 0)
                .orderByAsc("totalSize")
                .last("LIMIT " + spaceRankAnalyzeRequest.getTopN());

        return spaceService.list(queryWrapper);
    }
}




