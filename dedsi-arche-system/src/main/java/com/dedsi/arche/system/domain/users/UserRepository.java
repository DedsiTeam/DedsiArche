package com.dedsi.arche.system.domain.users;
import java.util.Optional;
import java.util.UUID;
/** 用户聚合仓储契约。 */
public interface UserRepository {
    Optional<User> findById(UUID id);
    Optional<User> findByUsername(String username);
    User save(User user);
}
