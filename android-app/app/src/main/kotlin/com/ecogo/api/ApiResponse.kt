package com.ecogo.api

/**
 * API 统一响应格式
 * 对应后端的 ResponseMessage<T>
 */
data class ApiResponse<T>(
    val code: Int = 0,
    val message: String = "",
    val data: T? = null
) {
    // 计算属性，根据 code 判断是否成功
    val success: Boolean get() = code == 200
}

/**
 * 空响应（没有数据体）
 */
data class EmptyResponse(
    val message: String = "Success"
)
