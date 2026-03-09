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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AnalysisServiceImpl implements AnalysisService {

    @Resource
    private Connection connection;
    @Value("${customer-service.xlsx-storage-path}")
    private String xlsxStoragePath;
    @Resource
    private ApplicationEventPublisher eventpublisher;

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
        if (StringUtils.isBlank(sql)) {
            return Response.fail("SQL不能为空");
        }
        verifyData(connection, "xlsx_data");
        return Response.success();
    }

    private boolean isXlsxFile(String fileName) {
        return fileName.endsWith(".xlsx");
    }

    /**
     * 验证数据库中的数据
     */
    private static void verifyData(Connection conn, String tableName) throws SQLException {
        System.out.println("\n========== 数据验证开始 ==========");

        // 1. 查询总条数
        String countSql = "SELECT COUNT(*) FROM " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            if (rs.next()) {
                System.out.println("表 " + tableName + " 中的数据条数: " + rs.getInt(1));
            }
        }

        // 2. 查询表结构
        System.out.println("\n表结构:");
        String schemaSql = "DESCRIBE " + tableName;
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(schemaSql)) {
            while (rs.next()) {
                System.out.println("  - " + rs.getString("column_name") + " : " + rs.getString("column_type"));
            }
        }

        // 3. 查询前3条数据样本
        System.out.println("\n前3条数据样本:");
        String sampleSql = "SELECT * FROM " + tableName + " LIMIT 3";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sampleSql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // 打印表头
            for (int i = 1; i <= columnCount; i++) {
                System.out.print(meta.getColumnName(i) + "\t");
            }
            System.out.println();

            // 打印数据
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    System.out.print(rs.getString(i) + "\t");
                }
                System.out.println();
            }
        }

        System.out.println("========== 数据验证结束 ==========\n");
    }
}
