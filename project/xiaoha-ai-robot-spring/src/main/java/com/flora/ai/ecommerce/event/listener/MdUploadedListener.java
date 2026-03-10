package com.flora.ai.ecommerce.event.listener;

import com.flora.ai.ecommerce.domain.dos.AiCustomerServiceMdStorageDO;
import com.flora.ai.ecommerce.domain.mapper.MdStorageMapper;
import com.flora.ai.ecommerce.enums.AiCustomerServiceMdStatusEnum;
import com.flora.ai.ecommerce.event.MdUploadedEvent;
import com.flora.ai.ecommerce.reader.MarkdownReader;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MdUploadedListener {

    @Resource
    private MarkdownReader markdownReader;
    @Resource
    private MdStorageMapper mdStorageMapper;
    @Resource
    private VectorStore vectorStore;
    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * Markdown 文件向量化
     * 
     * @param event
     */
    @EventListener
    @Async("eventTaskExecutor")
    public void vectorizing(MdUploadedEvent event) {
        log.info("## MdUploadedEvent: {}", event);
        Long id = event.getId();
        String filePath = event.getFilePath();
        Map<String, Object> metadatas = event.getMetadatas();

        // 更新存储文件的状态值
        mdStorageMapper.updateById(AiCustomerServiceMdStorageDO.builder()
                .id(id)
                .status(AiCustomerServiceMdStatusEnum.VECTORIZING.getCode())
                .updateTime(LocalDateTime.now())
                .build());

        // 编程式事务
        boolean isSuccess = Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            try {
                // 读取文件
                org.springframework.core.io.Resource resource = new FileSystemResource(filePath);
                // 解析为 Document 集合
                List<Document> documents = markdownReader.loadMarkdown(resource, metadatas);

                log.info("## documents: {}", documents);

                // 向量化并入库
                for (Document document : documents) {
                    // 防止重复添加相同文档进入库
                    List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                            .query(document.getText())
                            .topK(1)
                            .build());

                    if (!results.isEmpty() && results.get(0).getScore() > 0.99)
                        continue;

                    // 通过向量模型，将文档向量化存储到 PGVector中
                    vectorStore.add(List.of(document));
                }

                // 更新存储文件的处理状态
                mdStorageMapper.updateById(AiCustomerServiceMdStorageDO.builder()
                        .id(id)
                        .status(AiCustomerServiceMdStatusEnum.COMPLETED.getCode())
                        .updateTime(LocalDateTime.now())
                        .build());

                return true;
            } catch (Exception e) {
                log.error("向量化失败", e);
                status.setRollbackOnly();
                return false;
            }
        }));

        // 失败处理
        if (!isSuccess) {
            mdStorageMapper.updateById(AiCustomerServiceMdStorageDO.builder()
                    .id(id)
                    .status(AiCustomerServiceMdStatusEnum.FAILED.getCode())
                    .updateTime(LocalDateTime.now())
                    .build());
        }
    }
}
