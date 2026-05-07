package org.sparta_coffee.domain.menu.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta_coffee.global.entity.BaseEntity;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "menus")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false)
    private boolean active = true;


    @Builder
    public Menu(String name, long price) {
        this.name = name;
        this.price = price;
    }

    public void update(String name, long price) {
        this.name = name;
        this.price = price;
    }

    // 소프트 딜리트
    public void delete() {
        this.active = false;
    }


}
