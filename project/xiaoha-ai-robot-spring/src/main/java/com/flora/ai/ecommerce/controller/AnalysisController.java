package com.flora.ai.ecommerce.controller;

import com.flora.ai.ecommerce.model.vo.xlsxQuery.DuckdbSqlReqVO;
import com.flora.ai.ecommerce.service.AnalysisService;
import com.flora.ai.ecommerce.utils.Response;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;

@RestController
@RequestMapping("/analysis")
public class AnalysisController {

    @Resource
    private AnalysisService analysisService;

    @PostMapping(value = "/xlsx/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<?> uploadXlsxFile(@RequestPart(value = "file", required = false) MultipartFile file) {
        return analysisService.uploadXlsxFile(file);
    }

    @PostMapping("/xlsx/query")
    public Response<?> queryXlsxData(@RequestBody @Validated DuckdbSqlReqVO duckdbSqlReqVO) throws SQLException {
        return analysisService.queryXlsxData(duckdbSqlReqVO);
    }
}
