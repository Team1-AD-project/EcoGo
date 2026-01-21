package com.example.EcoGo.exception;

import com.example.EcoGo.exception.errorcode.ErrorCode;
import com.example.EcoGo.dto.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseBody
    public ResponseMessage handlerBusinessException(BusinessException e){
        return new ResponseMessage(e.getCode(),e.getMessage(),null);
    }

    // 处理系统异常（如空指针、数据库连接失败）
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseMessage handlerSystemException(Exception e) {
        log.error("系统异常", e); // 记录详细日志
        return new ResponseMessage(ErrorCode.SYSTEM_ERROR.getCode(), ErrorCode.SYSTEM_ERROR.getMessage(),null );
    }
}
