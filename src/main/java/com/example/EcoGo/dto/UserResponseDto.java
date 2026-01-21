package com.example.EcoGo.dto;


public class UserResponseDto {//根据自己的需要造比如UserUpdateDto
    private String username;
    private String email;

    public UserResponseDto(String username) {
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    // 只暴露需要给前端的字段，不含 password
    // getter/setter
}



