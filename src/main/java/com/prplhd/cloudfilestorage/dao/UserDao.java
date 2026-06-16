package com.prplhd.cloudfilestorage.dao;

import java.util.Optional;

public interface UserDao<ID, E> {

    public Optional<E> findByUsername(String username);

    public E save(E entity);
}
