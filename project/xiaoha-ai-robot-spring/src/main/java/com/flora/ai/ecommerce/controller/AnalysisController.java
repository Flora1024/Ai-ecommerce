package com.flora.ai.ecommerce.controller;

import com.flora.ai.ecommerce.aspect.ApiOperationLog;
import com.flora.ai.ecommerce.enums.ResponseCodeEnum;
import com.flora.ai.ecommerce.exception.BizException;
import com.flora.ai.ecommerce.model.AIResponse;
import com.flora.ai.ecommerce.model.vo.chat.DataAnalysisReqVO;
import com.flora.ai.ecommerce.model.vo.xlsxQuery.DuckdbSqlReqVO;
import com.flora.ai.ecommerce.service.AnalysisService;
import com.flora.ai.ecommerce.utils.Response;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.sql.SQLException;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    @Resource
    private AnalysisService analysisService;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    /**
     * 上传 xlsx 文件
     * @param file
     * @return
     */
    @PostMapping(value = "/xlsx/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> uploadXlsxFile(@RequestPart(value = "file", required = false) MultipartFile file) {
        return analysisService.uploadXlsxFile(file);
    }

    /**
     * 数据分析
     * @param dataAnalysisReqVO
     * @return
     */
    @PostMapping(value = "/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ApiOperationLog(description = "数据分析")
    public Flux<AIResponse> chat(@RequestBody @Validated DataAnalysisReqVO dataAnalysisReqVO) {

        // 用户消息
        String userMessage = dataAnalysisReqVO.getMessage();
        // 模型名称
        String modelName = dataAnalysisReqVO.getModelName();
        // 温度值
        Double temperature = dataAnalysisReqVO.getTemperature();

        // 构建 ChatModel
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        // 构建 ChatClientRequestSpec
        ChatClient.ChatClientRequestSpec chatClientPreRequestSpec = ChatClient.create(chatModel)
                .prompt(analysisService.buildPromptFirstStage(userMessage))
                .options(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(temperature)
                        .build());

        String sql = chatClientPreRequestSpec.call().content();
        String resultMarkdown = "";
        try {
            resultMarkdown = analysisService.executeSql2Markdown(sql);
        } catch (SQLException e) {
            throw new BizException(ResponseCodeEnum.SQL_ERROR);
        }

        // 构建 ChatClientRequestSpec
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = ChatClient.create(chatModel)
                .prompt(analysisService.buildPromptSecondStage(userMessage, sql, resultMarkdown))
                .options(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(temperature)
                        .build());

        // todo 添加Advisor

        // 流式输出
        return chatClientRequestSpec
                .stream()
                .content()
                .mapNotNull(text -> AIResponse.builder().v(text).build()); // 构建返参 AIResponse
    }
}
