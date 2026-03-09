package com.flora.ai.ecommerce.model.vo.xlsxQuery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DuckdbSqlReqVO {

    /**
     * sql查询语句
     */
    private String sql;
}
