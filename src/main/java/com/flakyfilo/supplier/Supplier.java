package com.flakyfilo.supplier;

import com.flakyfilo.common.BaseTimeEntity;
import com.flakyfilo.common.Validate;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "supplier")
public class Supplier extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(length = 200)
    private String note;

    @Builder(access = AccessLevel.PRIVATE)
    private Supplier(String name, String note) {
        this.name = name;
        this.note = note;
    }

    public static Supplier register(String name, String note) {
        Validate.notBlank(name, "구매처명");
        return Supplier.builder().name(name).note(note).build();
    }
}
