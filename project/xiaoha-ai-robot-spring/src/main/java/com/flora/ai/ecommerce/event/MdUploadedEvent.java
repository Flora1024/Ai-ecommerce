package com.flora.ai.ecommerce.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MdUploadedEvent {
    /**
     * 文件ID
     */
    private Long id;

    /**
     * 文件路径
     */
    private String filePath;

    /**
     * 元数据
     */
    private Map<String, Object> metadatas;
}
