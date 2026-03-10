package com.flora.ai.ecommerce.reader;

import cn.hutool.core.collection.CollUtil;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MarkdownReader {

    public List<Document> loadMarkdown(Resource resource, Map<String, Object> metadatas) {
        // 阅读器配置类
        MarkdownDocumentReaderConfig.Builder configBuilder = MarkdownDocumentReaderConfig.builder()
                .withHorizontalRuleCreateDocument(true)
                .withIncludeCodeBlock(false)
                .withIncludeBlockquote(false);

        // 元数据
        if (CollUtil.isNotEmpty(metadatas)) {
            configBuilder.withAdditionalMetadata(metadatas);
        }

        // 创建阅读器
        MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, configBuilder.build());

        // 读取并转换为 Document 文档集合
        return reader.get();
    }
}
