package com.example.ecommerce.backend.model;


import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false)
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private int inStock;

    public void decreaseInStock(int quantity) {
        if(quantity > inStock)
            throw new ProductOutOfStockException("Unable to file Order, requested quantity is more, then currently in stock!");
        inStock -= quantity;
    }
}

class ProductOutOfStockException extends RuntimeException{

    public ProductOutOfStockException(String msg) {
        super(msg);
    }
}