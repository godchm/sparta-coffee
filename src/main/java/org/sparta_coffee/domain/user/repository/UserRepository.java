package org.sparta_coffee.domain.user.repository;

import jakarta.persistence.LockModeType;
import org.sparta_coffee.domain.point.entity.UserPoint;
import org.sparta_coffee.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>
{



    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
