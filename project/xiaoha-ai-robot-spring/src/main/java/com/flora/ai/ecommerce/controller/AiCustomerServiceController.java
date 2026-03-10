package com.flora.ai.ecommerce.controller;

import com.flora.ai.ecommerce.advisor.CustomerServiceAdvisor;
import com.flora.ai.ecommerce.aspect.ApiOperationLog;
import com.flora.ai.ecommerce.model.AIResponse;
import com.flora.ai.ecommerce.model.vo.customerService.*;
import com.flora.ai.ecommerce.service.CustomerService;
import com.flora.ai.ecommerce.utils.PageResponse;
import com.flora.ai.ecommerce.utils.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/customer-service")
public class AiCustomerServiceController {

    @Resource
    private CustomerService customerService;
    @Resource
    private VectorStore vectorStore;

    @Value("${customer-service.model}")
    private String model;
    @Value("${customer-service.temperature}")
    private Double temperature;
    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @PostMapping(value = "/md/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> uploadMarkDownFile(@RequestPart(value = "file", required = false) MultipartFile file) {
        return customerService.uploadMarkDownFile(file);
    }

    @PostMapping("/md/delete")
    @ApiOperationLog(description = "删除 Markdown 文件")
    public Response<?> deleteMarkdownFile(@RequestBody @Validated DeleteMarkdownFileReqVO deleteMarkdownFileReqVO) {
        return customerService.deleteMarkdownFile(deleteMarkdownFileReqVO);
    }

    @PostMapping("/md/list")
    @ApiOperationLog(description = "查询 Markdown 文件列表")
    public PageResponse<FindMarkdownFilePageListRspVO> findMarkdownFilePageList(@RequestBody @Validated FindMarkdownFilePageListReqVO findMarkdownFilePageListReqVO) {
        return customerService.findMarkdownFilePageList(findMarkdownFilePageListReqVO);
    }

    @PostMapping("/md/update")
    @ApiOperationLog(description = "更新 Markdown 文件")
    public Response<?> UpdateMarkdownFile(@RequestBody @Validated UpdateMarkdownFileReqVO updateMarkdownFileReqVO) {
        return customerService.UpdateMarkdownFile(updateMarkdownFileReqVO);
    }

    @PostMapping(value = "/chat/completion", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    //    @GetMapping(value = "/chat/completion", produces = "text/html;charset=utf-8")
    @ApiOperationLog(description = "电商助手问答 [RAG]")
    public Flux<AIResponse> chat(@RequestBody @Validated AiCustomerServiceChatReqVO aiCustomerServiceChatReqVO) {
        // 用户提示词
         String message = aiCustomerServiceChatReqVO.getMessage();

        ChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build())
                .build();

        // 设置模型的名称，温度值
        ChatClient.ChatClientRequestSpec requestSpec = ChatClient.create(chatModel)
                .prompt()
                .options(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .user(message);

        List<Advisor> advisors = new ArrayList<>();
        advisors.add(new CustomerServiceAdvisor(vectorStore));

        requestSpec.advisors(advisors);

        return requestSpec
                .stream()
                .content()
                .mapNotNull(text -> AIResponse.builder().v(text).build()); // 构建返参 AIResponse;
    }
}
