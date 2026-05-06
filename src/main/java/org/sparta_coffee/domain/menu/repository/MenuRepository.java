package org.sparta_coffee.domain.menu.repository;

import java.util.List;
import java.util.Optional;

import org.sparta_coffee.domain.menu.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    List<Menu> findAllByActiveTrueOrderByIdAsc();

    Optional<Menu> findByIdAndActiveTrue(Long id);

    Optional<Menu> findByName(String name);
}
