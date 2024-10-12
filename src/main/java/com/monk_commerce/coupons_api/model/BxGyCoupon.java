package com.monk_commerce.coupons_api.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@DiscriminatorValue("bxgy")
public class BxGyCoupon extends Coupon {

    private Integer repetition_limit;

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BuyProduct> buyProducts = new ArrayList<>();

    @OneToMany(mappedBy = "coupon", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GetProduct> getProducts = new ArrayList<>();
}
