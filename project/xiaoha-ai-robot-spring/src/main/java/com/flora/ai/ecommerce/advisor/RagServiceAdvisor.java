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

@Slf4j
public class RagServiceAdvisor implements StreamAdvisor {

    private final VectorStore vectorStore;

    private static final int DEFAULT_TOP_K = 5;

    private static final PromptTemplate CUSTOMER_SERVICE_PROMPT_TEMPLATE = new PromptTemplate("""
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
            - 如果用户问题与上下文信息直接相关，请提供详细、准确的回答
            - 如果用户问题与上下文信息间接相关，可以基于已有信息进行合理推断
            - 如果用户问题完全超出上下文范围，或者上下文信息不足以回答该问题
        
            **无法回答时的统一回复**：
            如果遇到基本上与电商、订单管理情景不符的问题时统一回答：
            "不好意思，这类问题超出了我的专业认知范围，我无法给到您满意的回答 😟"
        
            **图片展示**：
            如需要展示图片，请使用 Markdown 格式：![](图片链接)
        
            请严格按照以上要求回答问题
            """);

    public RagServiceAdvisor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        Prompt prompt = chatClientRequest.prompt();
        UserMessage userMessage = prompt.getUserMessage();

        // 向量库查询
        List<Document> documents = vectorStore.similaritySearch(SearchRequest.builder()
                .query(userMessage.getText())
                .topK(DEFAULT_TOP_K)
                .build());

        String context = buildContext(documents);

        // 填充提示词
        Prompt newPrompt = CUSTOMER_SERVICE_PROMPT_TEMPLATE.create(Map.of(
                "context", context,
                "question", userMessage.getText()
        ), chatClientRequest.prompt().getOptions());

        log.info("## 增强后的提示词如下: {}", newPrompt);

        ChatClientRequest newChatClientRequest = ChatClientRequest.builder()
                .prompt(newPrompt)
                .build();

        return streamAdvisorChain.nextStream(newChatClientRequest);
    }

    private String buildContext(List<Document> documents) {
        StringBuilder contextTemp = new StringBuilder();

        for (Document document : documents) {
            contextTemp.append(String.format("""
                        %s
                        ---\n
                        """, document.getText()));
        }

        return contextTemp.toString();
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
