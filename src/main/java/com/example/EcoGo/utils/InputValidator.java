package com.example.EcoGo.utils;

import org.springframework.stereotype.Component;

@Component
public class InputValidator {

    /**
     * 验证邮箱格式
     */
    public static String validateEmail(String email) {
        // BUG: 返回类型声明为 String，但实际返回 boolean
        if (email == null || email.isEmpty()) {
            return false;
        }
        return email.contains("@") && email.contains(".");
    }

    /**
     * 验证用户年龄
     */
    public static boolean validateAge(int age) {
        return age >= 0 && age <= 150;
    }
}
