package com.flora.ai.ecommerce.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量存储 RAG Advisor
 * 根据用户查询从向量数据库检索相关内容，并增强提示词
 */
@Slf4j
public class VectorStoreRagAdvisor implements StreamAdvisor {

    private final VectorStore vectorStore;

    /**
     * 默认检索数量
     */
    private static final int DEFAULT_TOP_K = 5;

    /**
     * 相似度阈值（低于此值的结果将被过滤）
     */
    private static final double SIMILARITY_THRESHOLD = 0.5;

    /**
     * RAG 提示词模板
     */
    private static final PromptTemplate RAG_PROMPT_TEMPLATE = new PromptTemplate("""
            你是一名专业的电商订单管理助手，精通各种电商问答情景。请根据以下上下文信息回答用户问题。
            
            ## 上下文信息
            {context}

            请根据上下文内容来回复用户：
            
            ## 用户提问
            {question}
            
            ## 回答要求
            
            **规则**：
            1. **严格基于上下文**：根据知识库提供的上下文进行回答
            2. **个人定位**：保持自身的专业性，侧重于电商知识
            3. **禁止关键词**：避免使用"根据上下文"等容易暴露的用词
            
            **回答范围判断**：
            - ✅ 如果用户问题与上下文信息直接相关，请提供详细、准确的回答
            - ✅ 如果用户问题与上下文信息间接相关，可以基于已有信息进行合理推断
            - ❌ 如果用户问题完全超出上下文范围，或者上下文信息不足以回答该问题
        
            **无法回答时的统一回复**：
            如果遇到基本上与电商、订单管理情景不符的问题时统一回答：
            "不好意思，这类问题超出了我的专业认知范围，我无法给到您满意的回答 😟"
        
            **图片展示**：
            如需要展示图片，请使用 Markdown 格式：![](图片链接)
        
            请严格按照以上要求回答问题
            """);

    public VectorStoreRagAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.info("## 向量存储 RAG Advisor 执行");

        // 获取用户输入的问题
        Prompt prompt = chatClientRequest.prompt();
        UserMessage userMessage = prompt.getUserMessage();
        String userQuery = userMessage.getText();

        // 从向量数据库检索相关内容
        List<Document> relevantDocuments = retrieveRelevantDocuments(userQuery);

        // 构建增强提示词
        ChatClientRequest enhancedRequest = buildEnhancedRequest(chatClientRequest, userQuery, relevantDocuments);

        // 继续调用链
        return streamAdvisorChain.nextStream(enhancedRequest);
    }

    /**
     * 从向量数据库检索相关文档
     *
     * @param query 用户查询
     * @return 相关文档列表
     */
    private List<Document> retrieveRelevantDocuments(String query) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(query)
                    .topK(DEFAULT_TOP_K)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            // 过滤低相似度的结果
            List<Document> filteredResults = results.stream()
                    .filter(doc -> doc.getScore() == null || doc.getScore() >= SIMILARITY_THRESHOLD)
                    .collect(Collectors.toList());

            log.info("## RAG 检索完成，原始结果数: {}，过滤后结果数: {}",
                    results.size(), filteredResults.size());

            return filteredResults;

        } catch (Exception e) {
            log.error("## 向量检索失败", e);
            return List.of();
        }
    }

    /**
     * 构建增强的 ChatClientRequest
     *
     * @param originalRequest 原始请求
     * @param userQuery       用户问题
     * @param documents       检索到的文档
     * @return 增强后的请求
     */
    private ChatClientRequest buildEnhancedRequest(ChatClientRequest originalRequest,
                                                    String userQuery,
                                                    List<Document> documents) {
        // 如果没有检索到相关文档，保持原请求不变
        if (documents.isEmpty()) {
            log.info("## 未检索到相关文档，使用原始提示词");
            return originalRequest;
        }

        // 构建上下文内容
        String context = buildContext(documents);

        // 使用模板构建增强提示词
        Prompt enhancedPrompt = RAG_PROMPT_TEMPLATE.create(Map.of(
                "context", context,
                "question", userQuery
        ), originalRequest.prompt().getOptions());

        log.info("## RAG 增强提示词构建完成，用户问题: {}", userQuery);
        log.debug("## 增强后提示词: {}", enhancedPrompt.getUserMessage().getText());

        // 构建新的 ChatClientRequest
        return ChatClientRequest.builder()
                .prompt(enhancedPrompt)
                .build();
    }

    /**
     * 构建上下文内容
     *
     * @param documents 文档列表
     * @return 格式化的上下文字符串
     */
    private String buildContext(List<Document> documents) {
        StringBuilder contextBuilder = new StringBuilder();

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);

            contextBuilder.append("【片段 ").append(i + 1).append("】");

            // 如果有来源文件名，添加来源信息
            if (doc.getMetadata() != null && doc.getMetadata().get("originalFileName") != null) {
                contextBuilder.append(" 来源: ")
                        .append(doc.getMetadata().get("originalFileName"));
            }

            // 添加相似度分数
            if (doc.getScore() != null) {
                contextBuilder.append(" (相关度: ")
                        .append(String.format("%.2f", doc.getScore()))
                        .append(")");
            }

            contextBuilder.append("\n");
            contextBuilder.append(doc.getText());
            contextBuilder.append("\n\n");
        }

        return contextBuilder.toString().trim();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        // 在 CustomChatMemoryAdvisor (order=0) 之后执行
        // 确保先加载历史记忆，再进行 RAG 增强
        return 1;
    }
}
