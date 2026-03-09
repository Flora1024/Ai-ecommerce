package com.flora.ai.ecommerce.model.vo.customerService;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AiCustomerServiceChatReqVO {

    @NotBlank(message = "message不能为空")
    private String message;

    private String chatId;
}
