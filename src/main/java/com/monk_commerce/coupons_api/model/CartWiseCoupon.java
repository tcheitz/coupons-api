package com.monk_commerce.coupons_api.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@DiscriminatorValue("cart-wise")
public class CartWiseCoupon extends Coupon {
    private Integer threshold;
}

