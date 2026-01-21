package com.example.EcoGo.service;


import com.example.EcoGo.dto.UserResponseDto;
import com.example.EcoGo.exception.BusinessException;
import com.example.EcoGo.exception.errorcode.ErrorCode;

import com.example.EcoGo.interfacemethods.UserInterface;
import com.example.EcoGo.model.User;
import com.example.EcoGo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional
public class UserImplementation implements UserInterface {

    @Autowired
    private UserRepository userRepository;

    // 1. 修正方法语法：补充参数、返回值类型、方法体结束符
    // 2. 返回值改为UserDTO（而不是User）
    public UserResponseDto getUserByUsername(String username) {
        // 根据传入的用户名查询用户（避免硬编码"test"，提高复用性）
        User user = userRepository.findByUsername(username);

        // 空值判断：避免用户不存在时出现NullPointerException
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // Model转DTO
        UserResponseDto userDTO = new UserResponseDto(username);
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());

        return userDTO;
    }
}