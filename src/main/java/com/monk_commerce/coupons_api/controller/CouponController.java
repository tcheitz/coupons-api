package com.monk_commerce.coupons_api.controller;

import com.monk_commerce.coupons_api.dto.Cart;
import com.monk_commerce.coupons_api.dto.CartRequest;
import com.monk_commerce.coupons_api.dto.CouponRequest;
import com.monk_commerce.coupons_api.dto.CouponResponse;
import com.monk_commerce.coupons_api.model.Coupon;
import com.monk_commerce.coupons_api.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping
public class CouponController {
    @Autowired
    private CouponService couponService;

    @PostMapping("/coupons")
    public ResponseEntity<CouponResponse> createCoupon(@RequestBody CouponRequest couponRequest) {
        CouponResponse response = couponService.createCoupon(couponRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/coupons/{id}")
    public ResponseEntity<Coupon> getCoupon(@PathVariable Long id) {
        Coupon coupon = couponService.getCouponById(id);
        return coupon != null ? ResponseEntity.ok(coupon) : ResponseEntity.notFound().build();
    }

    @GetMapping("/coupons")
    public ResponseEntity<List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<Coupon> updateCoupon(@PathVariable Long id, @RequestBody CouponRequest body) {
        return ResponseEntity.ok(couponService.updateCoupon(id, body));
    }

    @DeleteMapping("/coupons/{id}")
    public ResponseEntity<Void> deleteCoupon(@PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/applicable-coupons")
    public ResponseEntity<Map<String, Object>> getApplicableCoupons(@RequestBody CartRequest cartRequest) {
        List<Map<String, Object>> applicableCoupons = couponService.getApplicableCoupons(cartRequest);
        Map<String, Object> response = new HashMap<>();
        response.put("applicable_coupons", applicableCoupons);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/apply-coupon/{id}")
    public ResponseEntity<Map<String, Object>> applyCoupon(@PathVariable Long id, @RequestBody CartRequest cartRequest) {
        Map<String, Object> updatedCart = couponService.applyCoupon(id, cartRequest);
        if (updatedCart == null) {
            return ResponseEntity.badRequest().body(Collections.singletonMap("error", "Invalid coupon ID"));
        }
        return ResponseEntity.ok(Collections.singletonMap("updated_cart", updatedCart));
    }
}