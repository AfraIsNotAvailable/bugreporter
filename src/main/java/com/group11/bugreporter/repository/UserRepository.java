package com.group11.bugreporter.repository;

import com.group11.bugreporter.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
