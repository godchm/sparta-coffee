package org.sparta_coffee.domain.point.repository;

import jakarta.persistence.LockModeType;
import org.sparta_coffee.domain.point.entity.UserPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserPointRepository extends JpaRepository<UserPoint, Long> {




    Optional<UserPoint> findByUserId(Long userId);

}
