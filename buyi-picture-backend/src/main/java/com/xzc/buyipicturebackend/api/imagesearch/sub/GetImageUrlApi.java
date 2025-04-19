package com.xzc.buyipicturebackend.api.imagesearch.sub;

import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * 获取360搜图的图片
 * 先上传到360服务器，再返回图片地址
 * @author xuzhichao
 * @since 2025-03-13
 */
@Slf4j
public class GetImageUrlApi {
    /**
     * 获取图片页面地址
     *
     * @param imageUrl
     * @return
     */
    public static String getImageUrl(String imageUrl) {
        String url = "https://st.so.com/r?src=st&srcsp=home&img_url=" + imageUrl + "&submittype=imgurl";
        try {
            Document document = Jsoup.connect(url).timeout(5000).get();
            Element imgElement = document.selectFirst(".img_img");
            if (imgElement != null) {
                String soImageUrl = "";
                // 获取当前元素的属性
                String style = imgElement.attr("style");
                if (style.contains("background-image:url(")) {
                    // 提取URL部分
                    int start = style.indexOf("url(") + 4;  // 从"Url("之后开始
                    int end = style.indexOf(")", start);    // 找到右括号的位置
                    if (start > 4 && end > start) {
                        soImageUrl = style.substring(start, end);
                    }
                }
                return soImageUrl;
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜图失败");
        } catch (Exception e) {
            log.error("搜图失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜图失败");
        }
    }

    public static void main(String[] args) {
        //测试功能
        String imageUrl = "https://buyi-picture-1334180391.cos.ap-shanghai.myqcloud.com//public/1872227414019698690/2025-01-10_Fz9muibtq5rirO2Y.jpg";
        String result = getImageUrl(imageUrl);
        System.out.println("搜索成功，结果URL：" + result);
    }
}
