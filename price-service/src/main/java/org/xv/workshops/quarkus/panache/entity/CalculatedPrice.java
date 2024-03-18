package org.xv.workshops.quarkus.panache.entity;

import java.math.BigDecimal;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Cacheable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
@Cacheable
public class CalculatedPrice extends PanacheEntity {

	@Column(unique=false)
	public Long orderId;
	
    @Column(length = 40, unique = false)
    public String calculatedBy;
    
    @Column(unique = false)
    public BigDecimal price;

    public CalculatedPrice() {
    }

    public CalculatedPrice(Long orderId, String calculatedBy, BigDecimal price) {
        this.orderId = orderId;
        this.calculatedBy = calculatedBy;
        this.price = price;
    }
}
