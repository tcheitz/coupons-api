package com.monk_commerce.coupons_api.dto;

import lombok.Data;

@Data
public class CouponResponse {
    private Long id;
    private String type;
    private Integer discount;
}

