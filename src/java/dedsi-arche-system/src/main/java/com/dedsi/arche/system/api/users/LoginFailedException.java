package com.dedsi.arche.system.api.users;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * 登录凭据校验失败，统一隐藏账号是否存在。
 */
public class LoginFailedException extends BadCredentialsException {
    public LoginFailedException() {
        super("账号或密码错误");
    }
}
