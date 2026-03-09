package com.flora.ai.ecommerce.enums;

import com.flora.ai.ecommerce.exception.BaseExceptionInterface;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResponseCodeEnum implements BaseExceptionInterface {

    // ----------- 通用异常状态码 -----------
    SYSTEM_ERROR("1000", "系统故障，请稍后重试"),
    PARAM_NOT_VALID("1001", "参数错误"),

    // ----------- 业务异常状态码 -----------
    CHAT_NOT_EXISTED("2000", "此对话不存在"),
    UPLOAD_FILE_CANT_EMPTY("2001", "上传的文件不能为空"),
    ONLY_SUPPORT_MARKDOWN("2002", "目前仅支持上传 Markdown 格式的文件"),
    UPLOAD_FILE_FAILED("2003", "文件存储异常"),
    MARKDOWN_FILE_NOT_FOUND("2004", "该 Markdown 文件不存在"),
    MARKDOWN_FILE_CANT_DELETE("2005", "正在处理中的 Markdown 问答文件，不允许删除"),
    ONLY_SUPPORT_XLSX("2006", "数据分析仅支持 Markdown 格式的文件"),
    ;

    private String errorCode;
    private String errorMessage;

}

