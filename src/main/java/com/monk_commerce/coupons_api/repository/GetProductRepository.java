package com.monk_commerce.coupons_api.repository;

import com.monk_commerce.coupons_api.model.GetProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GetProductRepository extends JpaRepository<GetProduct, Long> {
    void deleteByCouponId(Long couponId);
}
