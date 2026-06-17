package com.prplhd.cloudfilestorage.dao;

import com.prplhd.cloudfilestorage.entity.UserEntity;

import java.util.Optional;

public interface UserDao {

    public Optional<UserEntity> findByUsername(String username);

    public UserEntity save(UserEntity entity);
}
