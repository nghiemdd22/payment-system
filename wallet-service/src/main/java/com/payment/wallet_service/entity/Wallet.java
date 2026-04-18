package com.payment.wallet_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // @Column(nullable = false, length = 30)
    // private String type;
    @Enumerated(EnumType.STRING)
    private Type type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    // Bổ sung trường currency theo DB schema
    @Column(nullable = false, length = 3)
    private String currency; // Thường sẽ gán = "VND" khi tạo mới

    // VŨ KHÍ BÍ MẬT CHỐNG TRỪ ÂM TIỀN KHI CHỊU TẢI CAO
    @Version
    @Column(nullable = false)
    private Integer version;
}
// @Enumerated(EnumType.STRING)
// private Type type;