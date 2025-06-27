package com.yupi.usercenter.exception;

import com.yupi.usercenter.common.ErrorCode;

/**
 * 自定义异常类
 *
 * @author <a href="https://github.com/zcnovice"> zcnovice</a>

 */
public class BusinessException extends RuntimeException {

    /**
     * 异常码
     */
    private final int code;

    /**```
     * 描述
     */
    private final String description;

    /* 下面是几种不同的构造函数 (重载了3个构造函数)*/

    /* 全部自定义 */
    public BusinessException(String message, int code, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    /* 选取已经定义好的枚举类(ErrorCode) */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = errorCode.getDescription();
    }

    /* 只自定义描述 */
    public BusinessException(ErrorCode errorCode, String description) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.description = description;
    }

    public int getCode() {
        return code;
    }


    public String getDescription() {
        return description;
    }
}
