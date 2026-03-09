package com.flora.ai.ecommerce.advisor;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.google.common.collect.Lists;
import com.flora.ai.ecommerce.domain.dos.ChatMessageDO;
import com.flora.ai.ecommerce.domain.mapper.ChatMessageMapper;
import com.flora.ai.ecommerce.model.vo.chat.AiChatReqVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.Message;
import reactor.core.publisher.Flux;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
public class CustomChatMemoryAdvisor implements StreamAdvisor {

    private final ChatMessageMapper chatMessageMapper;
    private final AiChatReqVO aiChatReqVO;
    private final int limit;

    public CustomChatMemoryAdvisor(ChatMessageMapper chatMessageMapper, AiChatReqVO aiChatReqVO, int limit) {
        this.chatMessageMapper = chatMessageMapper;
        this.aiChatReqVO = aiChatReqVO;
        this.limit = limit;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
        log.info("## 自定义聊天记忆 Advisor");

        // 对话 id
        String chatUuid = aiChatReqVO.getChatId();

        // 查询数据库拉取最新的聊天记录
        List<ChatMessageDO> messages = chatMessageMapper.selectList(Wrappers.<ChatMessageDO>lambdaQuery()
                .eq(ChatMessageDO::getChatUuid, chatUuid)
                .orderByDesc(ChatMessageDO::getCreateTime)
                .last(String.format("LIMIT %d", limit)));

        // 按发布时间升序排序
        List<ChatMessageDO> sortedMessages = messages.stream()
                .sorted(Comparator.comparing(ChatMessageDO::getCreateTime))
                .toList();

        // 所有消息
        List<Message> messageList = Lists.newArrayList();

        // 数据库记录转换为对应类型的消息
        for (ChatMessageDO chatMessageDO : sortedMessages) {
            // 消息类型
            String type = chatMessageDO.getRole();
            if (Objects.equals(type, MessageType.USER.getValue())) {
                Message userMessage = new UserMessage(chatMessageDO.getContent());
                messageList.add(userMessage);
            }   else if (Objects.equals(type, MessageType.ASSISTANT.getValue())){
                Message assistantMessage = new AssistantMessage(chatMessageDO.getContent());
                messageList.add(assistantMessage);
            }
        }

        // 除了记忆消息还要添加当前用户消息
        messageList.addAll(chatClientRequest.prompt().getInstructions());

        // 构建一个新的 CharClientRequest
        ChatClientRequest processedChatClientRequest = chatClientRequest
                .mutate()
                .prompt(chatClientRequest.prompt().mutate().messages(messageList).build())
                .build();

        return streamAdvisorChain.nextStream(processedChatClientRequest);
    }
}
