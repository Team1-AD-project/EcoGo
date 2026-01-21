package com.example.EcoGo.repository;

import com.example.EcoGo.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;



public interface UserRepository extends MongoRepository<User, String> {
    // 可以在这里定义自定义查询方法，比如：
    User findByUsername(String username);
}
