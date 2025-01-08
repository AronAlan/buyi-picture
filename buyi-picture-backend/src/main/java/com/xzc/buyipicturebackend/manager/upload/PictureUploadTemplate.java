package com.xzc.buyipicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.xzc.buyipicturebackend.config.CosClientConfig;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.model.dto.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板类
 * 实现：1.文件上传  2.根据url地址上传
 *
 * @author xuzhichao
 * @since 2025-01-06
 */
@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;

    /**
     * 上传图片（本地图片或url）
     *
     * @param inputSource      文件源（本地图片或url）
     * @param uploadPathPrefix 上传路径前缀
     * @return UploadPictureResult图片上传并解析的结果
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1.校验图片
        validPicture(inputSource);

        // 2.图片上传地址
        String uuid = RandomUtil.randomString(16);

        // 获取文件名
        String originFilename = getOriginFilename(inputSource);

        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3.创建临时文件
            file = File.createTempFile(uploadPath, null);

            //处理文件源，并转为File（MultipartFile转File，或从url直接下载）
            processFile(inputSource, file);

            // 4.上传图片，获取图片信息（上传原图和压缩为webp的图片，一次性上传两张图片）
            PutObjectResult putObjectResult = putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 压缩处理结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                // 压缩处理成webp后的图片对象
                CIObject compressedCiObject = objectList.get(0);
                // 5.封装webp返回结果
                return buildResult(originFilename, compressedCiObject, uploadPath, imageInfo, file);
            }

            // 5.封装原图返回结果
            return buildResult(originFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 删除临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     *
     * @param inputSource Object（文件或url）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource Object（文件或url）
     * @return 原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     *
     * @param inputSource 输入源
     * @param file        文件
     * @throws Exception
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 封装原图返回结果
     *
     * @param originFilename 源文件名
     * @param file           文件
     * @param uploadPath     上传路径
     * @param imageInfo      图片信息
     * @return 图片解析返回结果
     */
    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        //没有生成压缩webp图，则使用原图
        uploadPictureResult.setWebpUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 封装webp返回结果
     * 只增加：多上传了一个webp格式的图片；网页显示的是webp格式的，下载时是原图格式
     * @param originFilename     源文件名
     * @param compressedCiObject 压缩后的webp图
     * @param uploadPath         原图片地址
     * @return 图片解析返回结果
     */
    private UploadPictureResult buildResult(String originFilename, CIObject compressedCiObject
            , String uploadPath, ImageInfo imageInfo, File file) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = compressedCiObject.getWidth();
        int picHeight = compressedCiObject.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        // 这里设置图片的格式和大小属性还是原图的格式和大小
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setWebpUrl(cosClientConfig.getHost() + "/" + compressedCiObject.getKey());
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

    /**
     * 上传对象（附带图片信息）并返回包含图片基本信息
     * 包含上传原图和压缩为webp的图片，一次性上传两张图片
     *
     * @param key  唯一键
     * @param file 文件
     */
    protected PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1表示返回原图信息
        picOperations.setIsPicInfo(1);

        List<PicOperations.Rule> rules = new ArrayList<>();

        // 图片压缩（转成webp格式）
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setRule("imageMogr2/format/webp");
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setFileId(webpKey);
        rules.add(compressRule);

        // 构造处理参数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        return cosClient.putObject(putObjectRequest);
    }
}
