package com.prplhd.cloudfilestorage.security;

import com.prplhd.cloudfilestorage.dao.UserDao;
import com.prplhd.cloudfilestorage.entity.UserEntity;
import com.prplhd.cloudfilestorage.exception.InvalidCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserDao userDao;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userDao.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Username '" + username + "' not found"));

        return new UserPrincipal(user);
    }
}
