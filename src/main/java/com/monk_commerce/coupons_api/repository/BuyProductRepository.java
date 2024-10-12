package com.monk_commerce.coupons_api.repository;

import com.monk_commerce.coupons_api.model.BuyProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BuyProductRepository extends JpaRepository<BuyProduct, Long> {
    void deleteByCouponId(Long couponId);
}