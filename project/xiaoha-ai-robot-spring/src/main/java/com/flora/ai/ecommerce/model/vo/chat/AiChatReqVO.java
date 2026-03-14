package com.flora.ai.ecommerce.model.vo.chat;

import com.flora.ai.ecommerce.model.vo.ChatRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AiChatReqVO implements ChatRequest {

    @NotBlank(message = "用户消息不能为空")
    private String message;

    private String chatId;

    private Boolean networkSearch = false;

    @NotBlank(message = "调用的大模型名称不能为空")
    private String modelName;

    private Double temperature = 0.7;
}
