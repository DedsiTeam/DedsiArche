package com.dedsi.arche.system.infrastructure.users.repositories;
import com.dedsi.arche.system.domain.users.*;
import org.springframework.stereotype.Repository;
import java.util.*;
/** 基础设施将业务仓储契约适配至 Spring Data。 */
@Repository
public class JpaUserRepository implements UserRepository {
    private final UserJpaRepository delegate;
    public JpaUserRepository(UserJpaRepository delegate) { this.delegate=delegate; }
    public Optional<User> findById(UUID id) { return delegate.findById(id); }
    public Optional<User> findByUsername(String username) { return delegate.findByUsername(username); }
    public User save(User user) { return delegate.save(user); }
}
