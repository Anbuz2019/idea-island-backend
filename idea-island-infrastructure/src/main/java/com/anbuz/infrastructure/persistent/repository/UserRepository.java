package com.anbuz.infrastructure.persistent.repository;

import com.anbuz.domain.user.model.entity.User;
import com.anbuz.domain.user.repository.IUserRepository;
import com.anbuz.infrastructure.persistent.dao.IUserDao;
import com.anbuz.infrastructure.persistent.po.UserPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepository implements IUserRepository {

    private final IUserDao userDao;

    @Override
    public void save(User user) {
        userDao.insert(toUserPO(user));
    }

    @Override
    public Optional<User> findById(Long id) {
        return userDao.selectById(id).map(this::toUser);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userDao.selectByUsername(username).map(this::toUser);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDao.selectByEmail(email).map(this::toUser);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return userDao.selectByPhone(phone).map(this::toUser);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userDao.countByUsername(username) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return userDao.countByEmail(email) > 0;
    }

    @Override
    public boolean existsByPhone(String phone) {
        return userDao.countByPhone(phone) > 0;
    }

    @Override
    public void update(User user) {
        userDao.update(toUserPO(user));
    }

    private UserPO toUserPO(User user) {
        return UserPO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .passwordHash(user.getPasswordHash())
                .nickname(user.getNickname())
                .avatarKey(user.getAvatarKey())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private User toUser(UserPO po) {
        return User.builder()
                .id(po.getId())
                .username(po.getUsername())
                .email(po.getEmail())
                .phone(po.getPhone())
                .passwordHash(po.getPasswordHash())
                .nickname(po.getNickname())
                .avatarKey(po.getAvatarKey())
                .status(po.getStatus())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }

}
