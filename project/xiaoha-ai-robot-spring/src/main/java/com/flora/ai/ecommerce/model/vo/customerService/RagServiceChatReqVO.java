package com.flora.ai.ecommerce.model.vo.customerService;

import com.flora.ai.ecommerce.model.vo.ChatRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RagServiceChatReqVO implements ChatRequest {

    @NotBlank(message = "message不能为空")
    private String message;

    private String chatId;
}
