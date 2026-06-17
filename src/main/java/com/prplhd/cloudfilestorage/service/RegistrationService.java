package com.prplhd.cloudfilestorage.service;

import com.prplhd.cloudfilestorage.dao.UserDao;
import com.prplhd.cloudfilestorage.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public void register(String username, String password) {
        String passwordHash = passwordEncoder.encode(password);

        UserEntity user = new UserEntity(null, username, passwordHash);

        userDao.save(user);
    }
}
