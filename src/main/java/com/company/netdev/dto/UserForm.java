package com.company.netdev.dto;

import lombok.Data;

@Data
public class UserForm {
    private String username;
    /** 新建必填；编辑时填写则重置密码 */
    private String password;
    private String name;
    private String role;
    private String phone;
    private String email;
    private Boolean enabled;
}
