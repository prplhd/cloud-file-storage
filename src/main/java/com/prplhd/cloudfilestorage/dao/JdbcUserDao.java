package com.prplhd.cloudfilestorage.dao;

import com.prplhd.cloudfilestorage.entity.UserEntity;
import com.prplhd.cloudfilestorage.exception.UserAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class JdbcUserDao implements UserDao<Long, UserEntity> {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private final RowMapper<UserEntity> userRowMapper = ((rs, rowNum) -> new UserEntity(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("password_hash")
    ));

    @Override
    public Optional<UserEntity> findByUsername(String username) {
        String findByUsernameQuery = """
                SELECT id, username, password_hash
                FROM users
                WHERE lower(username) = lower(:username)
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("username", username);

        List<UserEntity> users = jdbcTemplate.query(findByUsernameQuery, parameters, userRowMapper);

        return users.stream().findFirst();
    }

    @Override
    public UserEntity save(UserEntity entity) {
        String saveUserQuery = """
                INSERT INTO users (username, password_hash)
                VALUES (:username, :passwordHash)
                RETURNING id, username, password_hash
                """;

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("username", entity.username())
                .addValue("passwordHash", entity.passwordHash());

        try {
            return jdbcTemplate.queryForObject(saveUserQuery, parameters, userRowMapper);

        } catch (DuplicateKeyException e) {
            throw new UserAlreadyExistsException("Username '" + entity.username() + "' already taken", e);
        }
    }
}
