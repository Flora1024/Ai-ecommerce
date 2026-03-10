package com.flora.ai.ecommerce.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiCustomerServiceMdStatusEnum {

    PENDING(0, "待处理"),
    VECTORIZING(1, "向量化中"),
    COMPLETED(2, "成功"),
    FAILED(3, "失败")
    ;

    private final Integer code;
    private final String description;

    public static AiCustomerServiceMdStatusEnum codeOf(Integer code) {

        if (code == null) {
            return null;
        }
        for (AiCustomerServiceMdStatusEnum statusEnum : values()) {
            if (statusEnum.getCode().equals(code)) {
                return statusEnum;
            }
        }
        return null;
    }
}
