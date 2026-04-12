package com.groom.foocle.repository;

import com.groom.foocle.entity.User;
import com.groom.foocle.entity.enums.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailAndProvider(String email, Provider provider);
}
