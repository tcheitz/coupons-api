package com.monk_commerce.coupons_api.dto;

import com.monk_commerce.coupons_api.model.BuyProduct;
import com.monk_commerce.coupons_api.model.GetProduct;
import lombok.Data;

import java.util.List;

@Data
public class CouponRequest {
    private String type;
    private CouponDetails details;

    @Data
    public static class CouponDetails {
        private Integer threshold;
        private Integer discount;
        private Long product_id;
        private List<BuyProduct> buy_products;
        private List<GetProduct> get_products;
        private Integer repetition_limit;
    }
}
