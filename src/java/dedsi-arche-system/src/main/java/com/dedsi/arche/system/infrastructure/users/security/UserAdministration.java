package com.dedsi.arche.system.infrastructure.users.security;

import com.dedsi.arche.system.domain.users.IUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import java.util.*;

/** 管理员来源由部署配置明确指定，公开注册不能授予管理权限。 */
@Component("userAdministration")
public class UserAdministration {
    private final Set<String> usernames;
    private final IUserRepository users;
    public UserAdministration(IUserRepository users, @Value("${app.users.admin-usernames:}") String configured) {
        this.users = users;
        usernames = new HashSet<>();
        Arrays.stream(configured.split(",")).map(String::trim).filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT)).forEach(usernames::add);
    }
    public boolean isAdministrator(String username) { return usernames.contains(username); }
    public boolean canManage(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) return false;
        try {
            return users.findById(UUID.fromString(jwt.getSubject()))
                    .filter(u -> u.isEnabled() && isAdministrator(u.getUsername())).isPresent();
        } catch (IllegalArgumentException e) { return false; }
    }
}
