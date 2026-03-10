package com.flora.ai.ecommerce.model.vo.customerService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UpdateMarkdownFileReqVO {

    @NotNull(message = "文件ID不能为空")
    private Long id;

    @NotBlank(message = "文件描述不能为空")
    private String remark;
}
