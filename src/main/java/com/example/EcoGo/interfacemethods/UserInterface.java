package com.example.EcoGo.interfacemethods;

import com.example.EcoGo.dto.UserResponseDto;


public interface UserInterface {
    UserResponseDto getUserByUsername(String username);
}
