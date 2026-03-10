package com.flora.ai.ecommerce.service.impl;

import com.flora.ai.ecommerce.domain.dos.AiCustomerServiceMdStorageDO;
import com.flora.ai.ecommerce.enums.AiCustomerServiceMdStatusEnum;
import com.flora.ai.ecommerce.enums.ResponseCodeEnum;
import com.flora.ai.ecommerce.event.MdUploadedEvent;
import com.flora.ai.ecommerce.event.XlsxUploadedEvent;
import com.flora.ai.ecommerce.exception.BizException;
import com.flora.ai.ecommerce.model.vo.xlsxQuery.DuckdbSqlReqVO;
import com.flora.ai.ecommerce.service.AnalysisService;
import com.flora.ai.ecommerce.utils.Response;
import com.google.common.collect.Maps;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.duckdb.DuckDBConnection;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @Override
    public Response<?> queryXlsxData(DuckdbSqlReqVO duckdbSqlReqVO) throws SQLException {
        String sql = duckdbSqlReqVO.getSql();

        // 检查表是否存在
        if (!isTableExists()) {
            return Response.fail("表不存在");
        }

        // todo SQL校验

        // todo 执行SQL查询语句，返回结果用什么封装？

        return Response.success();
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
}
