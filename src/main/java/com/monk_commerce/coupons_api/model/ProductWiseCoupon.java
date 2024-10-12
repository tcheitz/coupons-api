package com.monk_commerce.coupons_api.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Data;

@Data
@Entity
@DiscriminatorValue("product-wise")
public class ProductWiseCoupon extends Coupon {
    private Long productId;
}
