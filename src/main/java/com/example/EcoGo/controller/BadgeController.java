package com.example.EcoGo.controller;

import com.example.EcoGo.model.Badge;

import com.example.EcoGo.service.BadgeServiceImpl;
import com.example.EcoGo.dto.ResponseMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class BadgeController {

    @Autowired
    private BadgeServiceImpl badgeService;

    // 购买徽章
    // @PostMapping("/mobile/badges/{badge_id}/purchase")
    @PostMapping("/badges/{badge_id}/purchase")
    public ResponseMessage<Object> purchaseBadge(@PathVariable("badge_id") String badgeId, @RequestBody Map<String, String> payload) {
        try {
            // 调用 Service 获取结果
            Object result = badgeService.purchaseBadge(payload.get("user_id"), badgeId);
            // 使用你的静态 success 方法封装
            return ResponseMessage.success(result);
        } catch (Exception e) {
            // 因为不能修改 ResponseMessage 加 helper 方法，所以这里直接用构造函数返回错误
            // code=400, message=错误信息, data=null
            return new ResponseMessage<>(400, e.getMessage(), null);
        }
    }

    // 佩戴/卸下 (含互斥)
    // @PutMapping("/mobile/badges/{badge_id}/display")
    @PutMapping("/badges/{badge_id}/display")
    public ResponseMessage<Object> toggleDisplay(@PathVariable("badge_id") String badgeId, @RequestBody Map<String, Object> payload) {
        try {
            // 获取参数
            String userId = (String) payload.get("user_id");
            Boolean isDisplay = (Boolean) payload.get("is_display");

            Object result = badgeService.toggleBadgeDisplay(userId, badgeId, isDisplay);
            return ResponseMessage.success(result);
        } catch (Exception e) {
            return new ResponseMessage<>(400, e.getMessage(), null);
        }
    }

    // 商城列表
    @GetMapping("/badges/shop")
    // @GetMapping("/mobile/badges/shop")
    public ResponseMessage<Object> getShopList() {
        return ResponseMessage.success(badgeService.getShopList());
    }

    // 我的背包
    @GetMapping("/badges/user/{user_id}")
    // @GetMapping("/mobile/badges/user/{user_id}")
    public ResponseMessage<Object> getMyBadges(@PathVariable("user_id") String userId) {
        return ResponseMessage.success(badgeService.getMyBadges(userId));
    }

    // 管理员创建 (用于初始化测试数据)
    @PostMapping("/badges")
    // @PostMapping("/web/badges")
    public ResponseMessage<Object> createBadge(@RequestBody Badge badge) {
        return ResponseMessage.success(badgeService.createBadge(badge));
    }
    /**
     * 管理员修改徽章
     * URL: PUT /api/v1/badges/{badge_id}
     */
    @PutMapping("/badges/{badge_id}")
    public ResponseMessage<Object> updateBadge(
            @PathVariable("badge_id") String badgeId, 
            @RequestBody Badge badgeDetails) {
        try {
            // 调用 Service 更新
            Badge updatedBadge = badgeService.updateBadge(badgeId, badgeDetails);
            return ResponseMessage.success(updatedBadge);
        } catch (Exception e) {
            return new ResponseMessage<>(400, e.getMessage(), null);
        }
    }

    /**
     * 管理员删除徽章
     * URL: DELETE /api/v1/badges/{badge_id}
     */
    @DeleteMapping("/badges/{badge_id}")
    public ResponseMessage<Object> deleteBadge(@PathVariable("badge_id") String badgeId) {
        try {
            // 调用 Service 删除
            badgeService.deleteBadge(badgeId);
            return ResponseMessage.success("删除成功"); // Data 可以返回简单的字符串提示
        } catch (Exception e) {
            return new ResponseMessage<>(400, e.getMessage(), null);
        }
    }
}