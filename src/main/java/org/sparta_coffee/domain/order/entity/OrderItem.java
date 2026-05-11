package org.sparta_coffee.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.sparta_coffee.domain.menu.entity.Menu;
import org.sparta_coffee.global.entity.BaseEntity;


/**
 * 주문에 포함된 개별 메뉴 정보를 저장하는 엔티티.
 *
 * 하나의 주문(Order)에 여러 메뉴와 수량을 담기 위한 구조로,
 * 장바구니처럼 주문 항목 목록을 표현.
 *
 * 다만 결제 전 임시 장바구니라기보다는
 * 주문 생성 시점에 확정된 메뉴 목록을 저장하는 주문 상세 내역이다.
 *
 * 메뉴명과 가격은 주문 당시의 값을 보존하기 위해 별도 필드로 저장한다.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;


    @Column(nullable = false)
    private String menuName;

    @Column(nullable = false)
    private long menuPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private long subtotalAmount;

    @Builder
    public OrderItem(Order order, Menu menu, int quantity) {
        this.order = order;
        this.menu = menu;
        this.menuName = menu.getName();
        this.menuPrice = menu.getPrice();
        this.quantity = quantity;
        this.subtotalAmount = menu.getPrice() * quantity;
    }

    public Long getOrderId() {
        return order.getId();
    }

    public Long getMenuId() {
        return menu.getId();
    }

}
