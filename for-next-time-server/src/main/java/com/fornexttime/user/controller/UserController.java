package com.fornexttime.user.controller;

import com.fornexttime.common.model.Result;
import com.fornexttime.user.req.ChangePasswordReq;
import com.fornexttime.user.res.UserProfileRes;
import com.fornexttime.user.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/**
 * 提供当前登录用户的资料与账户安全接口。
 */
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    /** 用户业务服务。 */
    @Resource
    private UserService userService;

    /**
     * 获取当前登录用户的资料，不暴露密码摘要等内部字段。
     *
     * @return 当前用户资料
     */
    @GetMapping("/profile")
    public Result<UserProfileRes> profile() {
        // 1. 交由服务读取当前用户范围内的资料。
        return Result.success(userService.getProfile());
    }

    /**
     * 上传当前登录用户的头像，仅接受 JPEG 或 PNG 格式。
     *
     * @param file 前端 multipart 表单中的头像文件
     * @return 包含最新头像地址的当前用户资料
     */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UserProfileRes> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 1. 交由服务校验文件并保存当前用户的头像。
        return Result.success(userService.uploadAvatar(file));
    }

    /**
     * 修改当前登录用户的密码；修改成功后前端应重新登录。
     *
     * @param request 已校验的密码修改请求
     * @return 空成功响应
     */
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordReq request) {
        // 1. 交由服务校验旧密码并安全更新密码摘要。
        userService.changePassword(request);
        // 2. 返回不包含敏感信息的成功响应。
        return Result.success();
    }
}
