package com.xzc.buyipicturebackend.service;

import com.xzc.buyipicturebackend.model.dto.PictureUploadRequest;
import com.xzc.buyipicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xzc.buyipicturebackend.model.entity.User;
import com.xzc.buyipicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author xuzhichao
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2024-12-31 16:05:11
 */
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile        MultipartFile 文件
     * @param pictureUploadRequest PictureUploadRequest 图片上传请求（包含图片id）
     * @param loginUser            User
     * @return PictureVO上传返回的结果
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
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
}
