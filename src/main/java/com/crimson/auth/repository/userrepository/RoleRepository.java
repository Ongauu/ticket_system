package com.crimson.auth.repository.userrepository;

import com.crimson.auth.entity.userEntity.Role;
import com.crimson.auth.entity.userEntity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
