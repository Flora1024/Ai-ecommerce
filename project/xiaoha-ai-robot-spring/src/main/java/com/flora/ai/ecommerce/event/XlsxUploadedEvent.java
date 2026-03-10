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
public class XlsxUploadedEvent {

    /**
     * 文件路径
     */
    private String filePath;

}
