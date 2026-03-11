package com.flora.ai.ecommerce.service;

import com.flora.ai.ecommerce.model.vo.xlsxQuery.DuckdbSqlReqVO;
import com.flora.ai.ecommerce.utils.Response;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;

public interface AnalysisService {

    /**
     * 上传 xlsx 文件
     * @param file
     * @return
     */
    public Response<?> uploadXlsxFile(MultipartFile file);

    /**
     * 查询 xlsx 文件数据
     * @param sql
     * @return
     */
    public Response<?> queryXlsxData(String sql) throws SQLException;

    /**
     * 构建一阶段提示词
     * @param userMessage
     * @return
     */
    public Prompt buildPromptFirstStage(String userMessage);

}
