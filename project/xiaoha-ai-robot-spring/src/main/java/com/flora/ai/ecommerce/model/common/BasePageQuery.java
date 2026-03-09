package com.flora.ai.ecommerce.model.common;

import lombok.Data;

@Data
public class BasePageQuery {
    /**
     * 当前页码
     */
    private Long current = 1L;
    /**
     * 每页展示的数据量
     */
    private Long size = 10L;
}

