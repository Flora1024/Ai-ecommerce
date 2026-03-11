package com.flora.ai.ecommerce.prompt;

import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

public class DataAnalysisPrompt {

    public static final PromptTemplate SQL_GENERATION_PROMPT_TEMPLATE = new PromptTemplate("""
            你是一个专业的 SQL 数据分析专家，
            现在用户上传了一份电商订单数据，
            经过数据处理，导入到了内存数据库DuckDB中。
            
            数据库表名为:
            {tableName}
            
            数据库表结构为:
            {schemaSummary}
            
            用户基于这份电商订单数据，提出了以下问题:
            {question}
            
            请注意：
            1. 请根据用户的问题生成符合 DuckDB 语法的 SQL 语句。
            2. 始终使用双引号引用列名，例如 SELECT "订单金额" FROM tableName
            3. 只输出 SQL 语句，不要有任何多余的解释。
            4. 如果用户问题描述过于暧昧，请你凭借经验生成用户最有可能想要查询的 SQL 语句。
            """);
}
