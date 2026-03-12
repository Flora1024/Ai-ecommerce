package com.flora.ai.ecommerce.service.impl;

import com.flora.ai.ecommerce.domain.dos.AiCustomerServiceMdStorageDO;
import com.flora.ai.ecommerce.enums.AiCustomerServiceMdStatusEnum;
import com.flora.ai.ecommerce.enums.ResponseCodeEnum;
import com.flora.ai.ecommerce.event.MdUploadedEvent;
import com.flora.ai.ecommerce.event.XlsxUploadedEvent;
import com.flora.ai.ecommerce.exception.BizException;
import com.flora.ai.ecommerce.model.vo.xlsxQuery.DuckdbSqlReqVO;
import com.flora.ai.ecommerce.prompt.DataAnalysisPrompt;
import com.flora.ai.ecommerce.service.AnalysisService;
import com.flora.ai.ecommerce.utils.Response;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.duckdb.DuckDBConnection;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.databind.type.LogicalType.Collection;

@Service
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

    @Resource
    private Connection connection;
    @Resource
    private NamedParameterJdbcTemplate jdbcTemplate;
    @Resource
    private ApplicationEventPublisher eventpublisher;
    @Value("${customer-service.xlsx-storage-path}")
    private String xlsxStoragePath;
    @Value("xlsx_data")
    private String tableName;

    /**
     * 上传 xlsx 文件
     * @param file
     * @return
     */
    @Override
    public Response<?> uploadXlsxFile(MultipartFile file) {
        // 校验文件是否为空
        if (file == null || file.isEmpty()) {
            return Response.fail("文件不能为空");
        }
        // 获取原始文件名
        String originalFilename = file.getOriginalFilename();
        // 验证文件类型
        if (StringUtils.isBlank(originalFilename) || !isXlsxFile(originalFilename)) {
            throw new BizException(ResponseCodeEnum.ONLY_SUPPORT_XLSX);
        }

        try {
            // 生成新的文件名,防止上传覆盖
            String newFileName = UUID.randomUUID().toString() + "-" + originalFilename;
            // 构建存储路径
            Path storageDirectory = Paths.get(xlsxStoragePath);
            Path targetPath = storageDirectory.resolve(newFileName);

            // 目录不存在时创建目录
            if (!Files.exists(storageDirectory)) {
                Files.createDirectory(storageDirectory);
            }
            // 保存文件
            file.transferTo(targetPath.toFile());

            // 记录操作日志
            log.info("## Xlsx 数据文件存储成功, 文件名：{} -> 存储路径：{}", originalFilename, targetPath);

            // 发布事件
            eventpublisher.publishEvent(XlsxUploadedEvent.builder()
                    .filePath(targetPath.toString())
                    .build());

            return Response.success();

        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new BizException(ResponseCodeEnum.UPLOAD_FILE_FAILED);
        }
    }

    /**
     * 执行SQL查询
     * @param sql
     * @return
     */
    @Override
    public String executeSql2Markdown(String sql) {

        // 检查表是否存在
        if (!isTableExists()) {
            throw new BizException(ResponseCodeEnum.TABLE_NOT_FOUND);
        }

        // SQL校验
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            throw new BizException(ResponseCodeEnum.ONLY_SUPPORT_SELECT);
        }

        // 执行SQL查询语句
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, new MapSqlParameterSource());

        // 转换成 Markdown
        return convertToMarkdownTable(results);
    }

    /**
     * 构建一阶段提示词
     * @param userMessage
     * @return
     */
    @Override
    public Prompt buildPromptFirstStage(String userMessage) {
        PromptTemplate promptTemplate = DataAnalysisPrompt.SQL_GENERATION_PROMPT_TEMPLATE;
        // 获取表名、表结构、用户问题
        return promptTemplate.create(
                Map.of(
                        "tableName", tableName,
                        "schemaSummary", getTableSchema(),
                        "question", userMessage
                )
        );
    }

    /**
     * 构建二阶段提示词
     * @param userMessage
     * @param sql
     * @param resultMarkdown
     * @return
     */
    @Override
    public Prompt buildPromptSecondStage(String userMessage, String sql, String resultMarkdown) {
        PromptTemplate promptTemplate = DataAnalysisPrompt.ANSWER_GENERATION_PROMPT_TEMPLATE;
        // 获取用户问题、SQL、结果Markdown
        return promptTemplate.create(
                Map.of(
                        "question", userMessage,
                        "sql", sql,
                        "resultMarkdown", resultMarkdown
                )
        );
    }

    private boolean isXlsxFile(String fileName) {
        return fileName.endsWith(".xlsx");
    }

    private boolean isTableExists() {
        // 构建查询语句
        String checkExistsSql = "SELECT COUNT(*) FROM duckdb_tables() WHERE table_name = :tableName";
        MapSqlParameterSource params = new MapSqlParameterSource("tableName", tableName);
        // 检查表是否存在
        int count = jdbcTemplate.queryForObject(checkExistsSql, params, Integer.class);
        return count != 0;
    }

    private String getTableSchema() {
        // 查询表结构
        String sql = "DESCRIBE " + tableName;
        StringBuilder schemaSummary = new StringBuilder();
        jdbcTemplate.query(sql, (rs, rowNum) -> {
            schemaSummary.append(rs.getString("column_name"))
                    .append(" : ")
                    .append(rs.getString("column_type"))
                    .append("\n");
            return null;
        });
        return schemaSummary.toString();
    }

    private String convertToMarkdownTable(List<Map<String, Object>> results) {
        if (results.isEmpty()) {
            return "查询结果为空";
        }

        // 获取列名（从第一行提取）
        Set<String> columns = results.get(0).keySet();

        StringBuilder markdown = new StringBuilder();

        // 表头
        markdown.append("| ").append(String.join(" | ", columns)).append(" |\n");

        // 分隔线
        markdown.append(columns.stream()
                .map(c -> "---")
                .collect(Collectors.joining(" | ", "| ", " |\n")));

        // 数据行
        for (Map<String, Object> row : results) {
            String rowStr = columns.stream()
                    .map(col -> String.valueOf(row.getOrDefault(col, "")))
                    .collect(Collectors.joining(" | ", "| ", " |\n"));
            markdown.append(rowStr);
        }

        // 如果数据太多，添加摘要信息
        if (results.size() > 100) {
            markdown.append("\n> 注：数据共 ").append(results.size()).append(" 行，仅展示前 100 行");
        }

        return markdown.toString();
    }
}
