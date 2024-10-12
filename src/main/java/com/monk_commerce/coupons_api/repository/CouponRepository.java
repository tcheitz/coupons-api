package com.monk_commerce.coupons_api.repository;

import com.monk_commerce.coupons_api.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {
}

