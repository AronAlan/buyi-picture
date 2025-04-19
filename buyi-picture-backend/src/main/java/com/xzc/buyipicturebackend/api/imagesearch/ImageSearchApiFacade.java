package com.xzc.buyipicturebackend.api.imagesearch;

import com.xzc.buyipicturebackend.api.imagesearch.model.ImageSearchResult;
import com.xzc.buyipicturebackend.api.imagesearch.sub.GetImageListApi;
import com.xzc.buyipicturebackend.api.imagesearch.sub.GetImageUrlApi;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 图片搜索API整合
 * 门面模式
 *
 * @author xuzhichao
 * @since 2025-03-13
 */
@Slf4j
public class ImageSearchApiFacade {
    public static List<ImageSearchResult> searchImages(String imageUrl,Integer start){
        String imageUrlOn360 = GetImageUrlApi.getImageUrl(imageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageUrlOn360,start);
        return imageList;
    }

    public static void main(String[] args) {
        //测试整合的以图搜图功能
        String imageUrl = "https://buyi-picture-1334180391.cos.ap-shanghai.myqcloud.com//public/1872227414019698690/2025-01-10_Fz9muibtq5rirO2Y.jpg";
        List<ImageSearchResult> result = searchImages(imageUrl,0);
        System.out.println("搜索成功，结果列表：" + result);
    }
}
