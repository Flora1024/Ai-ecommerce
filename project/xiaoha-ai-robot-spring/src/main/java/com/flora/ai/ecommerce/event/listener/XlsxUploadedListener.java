package com.flora.ai.ecommerce.event.listener;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.flora.ai.ecommerce.domain.dos.AiCustomerServiceMdStorageDO;
import com.flora.ai.ecommerce.event.XlsxUploadedEvent;
import com.flora.ai.ecommerce.event.XlsxUploadedEvent;
import com.google.common.collect.Lists;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class XlsxUploadedListener {

    @Resource(name = "duckdbDataSource")
    private DataSource dataSource;
    @Resource(name = "sharedDuckdbConnection")
    private Connection connection;
    @Value("xlsx_data")
    private String tableName;

    @EventListener
    @Async("eventTaskExecutor")
    public void LoadXlsx(XlsxUploadedEvent event) throws SQLException {

        log.info("## 正在加载xlsx文件，导入本地数据库: {}", event);
        String filePath = event.getFilePath();
        List<String> headers = Lists.newArrayList();

        EasyExcel.read(filePath, new AnalysisEventListener<Map<Integer, String>>() {

            private PreparedStatement pstmt = null;
            private int batchSize = 0;

            @Override
            public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                // 1. 获取表头
                headers.addAll(headMap.values().stream()
                        .map(h -> h.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_"))
                        .collect(Collectors.toList()));

                // 2. 动态创建 DuckDB 表
                try {
                    String columns = headers.stream().map(h -> "\"" + h + "\" VARCHAR").collect(Collectors.joining(", "));
                    String createSql = String.format("CREATE TABLE %s (%s)", tableName, columns);
                    connection.createStatement().execute("DROP TABLE IF EXISTS " + tableName);
                    connection.createStatement().execute(createSql);

                    String placeholders = headers.stream().map(h -> "?").collect(Collectors.joining(", "));
                    pstmt = connection.prepareStatement(String.format("INSERT INTO %s VALUES (%s)", tableName, placeholders));
                } catch (SQLException e) {
                    throw new RuntimeException("创建表失败", e);
                }
            }

            @Override
            public void invoke(Map<Integer, String> data, AnalysisContext context) {
                // 3. 批量插入数据
                try {
                    for (int i = 0; i < headers.size(); i++) {
                        pstmt.setString(i + 1, data.get(i));
                    }
                    pstmt.addBatch();
                    batchSize++;

                    if (batchSize >= 1000) { // 每1000行提交一次
                        pstmt.executeBatch();
                        batchSize = 0;
                    }
                } catch (SQLException e) {
                    throw new RuntimeException("数据插入失败", e);
                }
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                try {
                    if (batchSize > 0) pstmt.executeBatch();
                    if (pstmt != null) pstmt.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }).sheet().doRead();

        log.info("## xlsx文件导入数据库完成, 表字段为: {}", headers);

        verifyData(connection, tableName);
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
