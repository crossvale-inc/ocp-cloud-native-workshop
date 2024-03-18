package org.xv.workshops.quarkus.panache.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@Cacheable
public class OrderEntity extends PanacheEntity {

    @Column(length = 40, unique = true)
    public String name;

    public OrderEntity() {
    }

    public OrderEntity(String name) {
        this.name = name;
    }
}
