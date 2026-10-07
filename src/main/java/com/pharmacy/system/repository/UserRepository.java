package com.pharmacy.system.repository;

import com.pharmacy.system.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Retrieve user details by username during authentication & JWT validation
    Optional<User> findByUsername(String username);

    // Prevent registering staff members with duplicate usernames
    boolean existByUsername(String username);
}
