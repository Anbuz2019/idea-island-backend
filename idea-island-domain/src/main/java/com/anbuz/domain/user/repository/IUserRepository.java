package com.anbuz.domain.user.repository;

import com.anbuz.domain.user.model.entity.User;

import java.util.Optional;

public interface IUserRepository {

    void save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    void update(User user);

}
