package com.flora.ai.ecommerce.model.vo.customerService;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteMarkdownFileReqVO {

    @NotNull(message = " 问答文件ID不能为空 ")
    private Long id;
}
