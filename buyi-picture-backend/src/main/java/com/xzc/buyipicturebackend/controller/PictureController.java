package com.xzc.buyipicturebackend.controller;

import com.xzc.buyipicturebackend.annotation.AuthCheck;
import com.xzc.buyipicturebackend.common.BaseResponse;
import com.xzc.buyipicturebackend.common.ResultUtils;
import com.xzc.buyipicturebackend.constant.UserConstant;
import com.xzc.buyipicturebackend.manager.CosManager;
import com.xzc.buyipicturebackend.model.dto.PictureUploadRequest;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.PictureVO;
import com.xzc.buyipicturebackend.service.PictureService;
import com.xzc.buyipicturebackend.service.UserService;
import com.xzc.buyipicturebackend.service.impl.PictureServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

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
     */
    @PostMapping("/upload")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<PictureVO> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        // 新增或更新图片
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


}
