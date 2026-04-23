package com.anbuz.infrastructure.persistent.dao;

import com.anbuz.infrastructure.persistent.po.UserPO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

/**
 * 用户 MyBatis Mapper，负责 user 表和用户维度统计查询。
 */
@Mapper
public interface IUserDao {

    void insert(UserPO user);

    void update(UserPO user);

    Optional<UserPO> selectById(@Param("id") Long id);

    Optional<UserPO> selectByEmail(@Param("email") String email);

    Optional<UserPO> selectByPhone(@Param("phone") String phone);

    Optional<UserPO> selectByUsername(@Param("username") String username);

    int countByEmail(@Param("email") String email);

    int countByPhone(@Param("phone") String phone);

    int countByUsername(@Param("username") String username);

}
