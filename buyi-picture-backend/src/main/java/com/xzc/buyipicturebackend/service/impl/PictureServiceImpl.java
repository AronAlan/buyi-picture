package com.xzc.buyipicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import com.xzc.buyipicturebackend.common.ResultUtils;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import com.xzc.buyipicturebackend.manager.download.DownloadManager;
import com.xzc.buyipicturebackend.manager.upload.FilePictureUpload;
import com.xzc.buyipicturebackend.manager.upload.PictureUploadTemplate;
import com.xzc.buyipicturebackend.manager.upload.UrlPictureUpload;
import com.xzc.buyipicturebackend.mapper.PictureMapper;
import com.xzc.buyipicturebackend.model.dto.*;
import com.xzc.buyipicturebackend.model.entity.Picture;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.enums.PictureReviewStatusEnum;
import com.xzc.buyipicturebackend.model.vo.PictureVO;
import com.xzc.buyipicturebackend.model.vo.UserVo;
import com.xzc.buyipicturebackend.service.PictureService;
import com.xzc.buyipicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author xuzhichao
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2024-12-31 16:05:11
 */
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {

    @Resource
    private DownloadManager downloadManager;

    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

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
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        ThrowUtils.throwIf(inputSource == null, ErrorCode.PARAMS_ERROR, "图片为空");

        //判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        //如果是更新图片
        if (pictureId != null) {
            // 校验图片是否存在
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");

            // 仅本人或管理员可编辑或修改图片的相关信息
            // 其实是第二次及之后再次点击图片上传按钮，相当于换一张图片
            // 但是每次上传，都会保存（也相当于修改了）图片信息到数据库
            // 主要为了防止他人通过修改参数信息来篡改所属他人的已存在的图片
            //（不验证身份信息的话，请求体参数中增加一个pictureUploadRequest，即可修改他人的图片信息）
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }

        // 按照用户id划分目录。存放在COS的public目录下
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());

        // 上传图片，得到返回信息（根据inputSource类型区分上传方式，本地图片或url）
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setWebpUrl(uploadPictureResult.getWebpUrl());
        String picName = uploadPictureResult.getPicName();
        // 批量抓取图片时，使用管理员自定义的名称
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getCategory())) {
            picture.setCategory(pictureUploadRequest.getCategory());
        }
        if (pictureUploadRequest != null && CollUtil.isNotEmpty(pictureUploadRequest.getTags())) {
            picture.setTags(JSONUtil.toJsonStr(pictureUploadRequest.getTags()));
        }
        // 补充审核参数（用户和管理员都可上传图片，用户上传改为待审核，管理员上传自动过审）
        fillReviewParams(picture, loginUser, false);
        //如果pictureId不为空，表示更新，否则是新增
        if (pictureId != null) {
            //是更新，补充id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
            picture.setUpdateTime(new Date());
        } else {
            picture.setCreateTime(new Date());
            picture.setEditTime(new Date());
            picture.setUpdateTime(new Date());
        }

        //保存到数据库
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");

        PictureVO pictureVO = PictureVO.objToVo(picture);
        pictureVO.setUser(UserVo.objToVo(loginUser));
        return pictureVO;
    }

    /**
     * 下载图片
     *
     * @param filepath 文件路径
     * @param response 响应对象
     */
    @Override
    public void downloadPicture(String filepath, HttpServletResponse response) throws IOException {
        COSObjectInputStream cosObjectInput = null;
        try {
            COSObject cosObject = downloadManager.getObject(filepath);
            cosObjectInput = cosObject.getObjectContent();
            // 处理下载到的流
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (Exception e) {
            log.error("file download error, filepath = {}", filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "下载失败");
        } finally {
            if (cosObjectInput != null) {
                cosObjectInput.close();
            }
        }
    }

    /**
     * 构造图片查询请求QueryWrapper
     *
     * @param pictureQueryRequest PictureQueryRequest
     * @return QueryWrapper<Picture>
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        return queryWrapper;
    }

    /**
     * 获取单个图片的封装
     *
     * @param picture Picture
     * @param request HttpServletRequest
     * @return PictureVO（脱敏）
     */
    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVo userVO = userService.getUserVo(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装（用户）
     * Page<Picture> --> Page<PictureVO>
     *
     * @param picturePage Page<Picture>
     * @param request     HttpServletRequest
     * @return Page<PictureVO>（脱敏）
     */
    @Override
    public Page<PictureVO> getPictureVoPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVoPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        // 如果数据为空
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVoPage;
        }

        // Picture列表 -> PictureVO列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());

        // 1.关联查询用户信息
        // 提取图片对应的userId，去重
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        // 根据userId查询用户详细信息，分组后得到userId与用户详细信息的一一对应。（List<User>列表中其实只有一个用户元素）
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));

        // 2.填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = userIdUserListMap.get(userId).get(0);
            pictureVO.setUser(userService.getUserVo(user));
        });

        pictureVoPage.setRecords(pictureVOList);
        return pictureVoPage;
    }

    /**
     * 图片数据（id,url,简介）校验
     * 用于更新和修改图片时进行判断
     *
     * @param picture 图片
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    /**
     * 图片审核（管理员）
     *
     * @param pictureReviewRequest PictureReviewRequest管理员进行审核发送到后端的请求
     * @param loginUser            登录用户
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        // （pictureReviewRequest里要修改的状态不能是未审核）不能将已审核的数据改为未审核
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        //判断是否存在
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        //已经是需要修改的状态了
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }

        //更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        boolean result = this.updateById(updatePicture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "审核失败，数据库错误");
    }

    /**
     * 用户或管理员上传图片时，修改审核图片的审核状态
     * 填充审核参数（审核状态，审核员Id，审核信息，审核时间）
     *
     * @param picture   Picture
     * @param loginUser User
     * @param isEdit    是否为修改（供显示审核信息中修改和上传的不同）
     */
    @Override
    public void fillReviewParams(Picture picture, User loginUser, Boolean isEdit) {
        if (userService.isAdmin(loginUser)) {
            //如果是管理员上传的图片，则自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            if (isEdit) {
                picture.setReviewMessage("管理员编辑，自动过审");
            } else {
                picture.setReviewMessage("管理员上传，自动过审");
            }
            picture.setReviewTime(new Date());
        } else {
            //用户上传的，审核状态改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest 批量抓取图片请求
     * @param loginUser                   登录用户
     * @return 成功创建的图片数
     */
    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        //校验数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 20, ErrorCode.PARAMS_ERROR, "一次最多抓取20条");
        //要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);

        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取抓取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取抓取页面失败");
        }
        //最外层元素
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        //图片元素
        //获取包含完整数据的元素
        Elements imgElementList = div.select(".iusc");
        int uploadCount = 0;

        for (Element imgElement : imgElementList) {
            // 获取data-m属性中的JSON字符串
            String dataM = imgElement.attr("m");
            String fileUrl;
            try {
                // 解析JSON字符串
                JSONObject jsonObject = JSONUtil.parseObj(dataM);
                // 获取murl字段（原始图片URL）
                fileUrl = jsonObject.getStr("murl");
            } catch (Exception e) {
                log.error("解析图片数据失败", e);
                continue;
            }

            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            // 使用自定义前缀，名称前缀默认等于搜索关键词
            String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
            if (StrUtil.isBlank(namePrefix)) {
                namePrefix = searchText;
            }

            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            if (StrUtil.isNotBlank(namePrefix)) {
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            String category = pictureUploadByBatchRequest.getCategory();
            if (StrUtil.isNotBlank(category)) {
                pictureUploadRequest.setCategory(category);
            }
            List<String> tags = pictureUploadByBatchRequest.getTags();
            if (CollUtil.isNotEmpty(tags)) {
                pictureUploadRequest.setTags(tags);
            }
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    private final Cache<String, String> LOCAL_CACHE =
            Caffeine.newBuilder().initialCapacity(1024)
                    .maximumSize(10000L)
                    // 缓存 5 分钟移除
                    .expireAfterWrite(5L, TimeUnit.MINUTES)
                    .build();

    /**
     * 从缓存中读取图片VOs
     * 本地缓存-> redis缓存 -> 数据库
     *
     * @param pictureQueryRequest PictureQueryRequest
     * @param request             HttpServletRequest
     * @return Page<PictureVO>
     */
    @Override
    public Page<PictureVO> getDataFromCacheOrDb(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 构建缓存 key （将查询条件pictureQueryRequest作为key）
        String queryCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        String hashKey = DigestUtils.md5DigestAsHex(queryCondition.getBytes());
        String cacheKey = "buyipicture:listPictureVOByPage:" + hashKey;

        // 1. 查询本地缓存（Caffeine）
        String cachedValue = LOCAL_CACHE.getIfPresent(cacheKey);
        if (cachedValue != null) {
            return JSONUtil.toBean(cachedValue, Page.class);
        }

        // 2. 查询分布式缓存（Redis）
        ValueOperations<String, String> valueOps = stringRedisTemplate.opsForValue();
        cachedValue = valueOps.get(cacheKey);
        if (cachedValue != null) {
            // 如果命中 Redis，存入本地缓存并返回
            LOCAL_CACHE.put(cacheKey, cachedValue);
            return JSONUtil.toBean(cachedValue, Page.class);
        }

        // 3. 查询数据库
        Page<Picture> picturePage = this.page(new Page<>(current, size), getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVoPage = getPictureVoPage(picturePage, request);

        // 4. 更新缓存
        String cacheValue = JSONUtil.toJsonStr(pictureVoPage);
        // 更新本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValue);
        // 更新 Redis 缓存，设置过期时间为 5 分钟
        valueOps.set(cacheKey, cacheValue, 5, TimeUnit.MINUTES);

        return pictureVoPage;
    }

    /**
     * 删除包含本地缓存和redis缓存在内的所有缓存内容
     */
    @Override
    public void deleteAllCache() {
        // 1. 清除本地缓存
        LOCAL_CACHE.invalidateAll();

        // 2. 清除 Redis 缓存
        String keyPattern = "buyipicture:listPictureVOByPage:*";
        Set<String> keys = stringRedisTemplate.keys(keyPattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

}




