package com.flora.ai.ecommerce.model.vo.customerService;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FindMarkdownFilePageListRspVO {

    private Long id;

    private String originalFileName;

    private String fileSize;

    /**
     * 处理状态：0-待处理 1-向量化中 2-已完成 3-失败
     */
    private Integer status;

    private String remark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
