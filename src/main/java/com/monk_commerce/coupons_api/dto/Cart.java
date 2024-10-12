package com.monk_commerce.coupons_api.dto;

import lombok.Data;

import java.util.List;

@Data
public class Cart {
    private List<CartItem> items;
}

