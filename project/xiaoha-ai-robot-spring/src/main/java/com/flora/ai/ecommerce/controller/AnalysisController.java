package com.flora.ai.ecommerce.controller;

import com.flora.ai.ecommerce.aspect.ApiOperationLog;
import com.flora.ai.ecommerce.model.AIResponse;
import com.flora.ai.ecommerce.model.vo.xlsxQuery.DuckdbSqlReqVO;
import com.flora.ai.ecommerce.service.AnalysisService;
import com.flora.ai.ecommerce.utils.Response;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
     * 查询 xlsx 文件数据
     * @param duckdbSqlReqVO
     * @return
     * @throws SQLException
     */
    @PostMapping("/xlsx/query")
    public Response<?> queryXlsxData(@RequestBody @Validated DuckdbSqlReqVO duckdbSqlReqVO) throws SQLException {
        return analysisService.queryXlsxData(duckdbSqlReqVO);
    }

    @PostMapping("/completion")
    @ApiOperationLog(description = "数据分析")
    public Response<AIResponse> chat(@RequestBody @Validated DataAnalysisReqVO dataAnalysisReqVO) {
        // 构建 ChatModel
        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        // 构建 ChatClient
        ChatClient chatClient = ChatClient.create(chatModel);

        // 构建 ChatClientRequestSpec
        ChatClient.ChatClientRequestSpec chatClientRequestSpec = chatClient
    }
}
