package com.xzc.buyipicturebackend.api.aliyun;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.xzc.buyipicturebackend.api.aliyun.model.CreateOutPaintingTaskRequest;
import com.xzc.buyipicturebackend.api.aliyun.model.CreateOutPaintingTaskResponse;
import com.xzc.buyipicturebackend.api.aliyun.model.GetOutPaintingTaskResponse;
import com.xzc.buyipicturebackend.exception.BusinessException;
import com.xzc.buyipicturebackend.exception.ErrorCode;
import com.xzc.buyipicturebackend.exception.ThrowUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 阿里云AI服务API
 *
 * @author xuzhichao
 * @since 2025-04-23
 */
@Slf4j
@Component
public class AliyunAiApi {
    /**
     * 阿里云API Key配置
     */
    @Value("${aliYunAi.apiKey}")
    private String apiKey;

    /**
     * 创建任务地址
     */
    public static final String CREATE_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/image2image/out-painting";

    /**
     * 查询任务状态
     */
    public static final String GET_OUT_PAINTING_TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/%s";

    /**
     * 创建任务
     *
     * @param createOutPaintingTaskRequest 创建扩图任务的请求
     * @return 查询AI扩图任务时的响应
     */
    public CreateOutPaintingTaskResponse createOutPaintingTask(CreateOutPaintingTaskRequest createOutPaintingTaskRequest) {
        ThrowUtils.throwIf(createOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR, "扩图参数为空");

        // 发送Http请求
        HttpRequest httpRequest = HttpRequest.post(CREATE_OUT_PAINTING_TASK_URL)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey)
                // 必须开启异步处理
                .header("X-DashScope-Async", "enable")
                .header(Header.CONTENT_TYPE, ContentType.JSON.getValue())
                .body(JSONUtil.toJsonStr(createOutPaintingTaskRequest));

        // 响应
        try (HttpResponse httpResponse = httpRequest.execute()) {
            if (!httpResponse.isOk()) {
                log.error("请求异常：{}", httpResponse.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图失败");
            }

            CreateOutPaintingTaskResponse response = JSONUtil.toBean(httpResponse.body(), CreateOutPaintingTaskResponse.class);
            // 接口错误码，若接口请求成功则不会返回该码
            String errorCode = response.getCode();
            if (StrUtil.isNotBlank(errorCode)) {
                log.error("AI扩图失败，errorCode:{}，errorMessage:{}", errorCode, response.getMessage());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "AI扩图接口响应异常");
            }

            return response;
        }
    }

    /**
     * 查询所创建的任务
     *
     * @param taskId 请求唯一标识
     * @return 查询AI扩图任务时的响应
     */
    public GetOutPaintingTaskResponse getOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR, "AI任务id不能为空");
        String queryUrl = String.format(GET_OUT_PAINTING_TASK_URL, taskId);
        HttpRequest httpRequest = HttpRequest.get(queryUrl)
                .header(Header.AUTHORIZATION, "Bearer " + apiKey);
        try (HttpResponse response = httpRequest.execute()) {
            if (!response.isOk()) {
                log.error("获取AI任务失败，{}", response.body());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取AI任务失败");
            }
            return JSONUtil.toBean(response.body(), GetOutPaintingTaskResponse.class);
        }
    }


}
