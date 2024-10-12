package com.monk_commerce.coupons_api.service;

import com.monk_commerce.coupons_api.dto.*;
import com.monk_commerce.coupons_api.exception.CouponNotFoundException;
import com.monk_commerce.coupons_api.model.*;
import com.monk_commerce.coupons_api.repository.BuyProductRepository;
import com.monk_commerce.coupons_api.repository.CouponRepository;
import com.monk_commerce.coupons_api.repository.GetProductRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class CouponService {
    @Autowired
    private CouponRepository couponRepository;
    @Autowired
    private BuyProductRepository buyProductRepository;
    @Autowired
    private GetProductRepository getProductRepository;
    private Integer freeProductCount = 0;

    @Transactional
    public CouponResponse createCoupon(CouponRequest couponRequest) {
        Coupon coupon = switch (couponRequest.getType()) {
            case "cart-wise" -> createCartWiseCoupon(couponRequest);
            case "product-wise" -> createProductWiseCoupon(couponRequest);
            case "bxgy" -> createBxGyCoupon(couponRequest);
            default -> throw new IllegalArgumentException("Invalid coupon type: " + couponRequest.getType());
        };
        coupon = couponRepository.save(coupon);
        return prepareCouponResponse(coupon);
    }

    @Transactional
    public Coupon updateCoupon(Long id, CouponRequest updatedCouponDetails) {
        Coupon existingCoupon = getCouponById(id);
        if (existingCoupon == null) {
            throw new CouponNotFoundException("Coupon not found for id: " + id);
        }
        updateCouponDetails(existingCoupon, updatedCouponDetails);
        return couponRepository.save(existingCoupon);
    }


    public void deleteCoupon(Long id) {
        couponRepository.deleteById(id);
    }

    public List<Map<String, Object>> getApplicableCoupons(CartRequest cartRequest) {
        List<Coupon> allCoupons = getAllCoupons();
        List<CartItem> cartItems = cartRequest.getCart().getItems();

        if (cartItems == null) {
            return new ArrayList<>();
        }

        double cartTotal = calculateCartTotal(cartItems);
        Map<Long, CartItem> cartMap = buildCartMap(cartItems);

        List<Map<String, Object>> applicableCoupons = new ArrayList<>();
        for (Coupon coupon : allCoupons) {
            Map<String, Object> applicableCoupon = getApplicableCoupon(cartTotal, cartMap, coupon);
            if (applicableCoupon != null) {
                applicableCoupons.add(applicableCoupon);
            }
        }

        applicableCoupons.sort((coupon1, coupon2) -> Double.compare((Double) coupon2.get("discount"), (Double) coupon1.get("discount")));

        return applicableCoupons;
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    public Map<String, Object> applyCoupon(Long id, CartRequest cartRequest) {
        Coupon coupon = getCouponById(id);
        List<CartItem> cartItems = cartRequest.getCart().getItems();
        double totalDiscount = 0;
        double totalPrice = 0;

        Map<Long, CartItem> cartMap = new HashMap<>();
        for (CartItem item : cartItems) {
            cartMap.put(item.getProduct_id(), item);
            totalPrice += item.getPrice() * item.getQuantity();
        }

        totalDiscount += applyCouponBasedOnType(coupon, cartMap, cartItems, totalPrice);

        List<Map<String, Object>> updatedItems = prepareUpdatedItems(cartItems, coupon, totalDiscount);

        double finalPrice =  totalPrice - totalDiscount;
        if(coupon instanceof BxGyCoupon){
            finalPrice = totalPrice;
            totalPrice = totalPrice + totalDiscount;
        }
        Map<String, Object> updatedCart = new HashMap<>();
        updatedCart.put("items", updatedItems);
        updatedCart.put("total_price", totalPrice);
        updatedCart.put("total_discount", totalDiscount);
        updatedCart.put("final_price", finalPrice);

        return updatedCart;
    }

    public Coupon getCouponById(Long id) {
        return couponRepository.findById(id).orElseThrow(() -> new CouponNotFoundException("Coupon with ID " + id + " not found"));
    }

    private double applyCouponBasedOnType(Coupon coupon, Map<Long, CartItem> cartMap, List<CartItem> cartItems, double totalPrice) {
        double totalDiscount = 0;

        if (coupon instanceof CartWiseCoupon) {
            totalDiscount += (totalPrice * coupon.getDiscount()) / 100;
        } else if (coupon instanceof ProductWiseCoupon) {
            CartItem cartItem = cartMap.get(((ProductWiseCoupon) coupon).getProductId());
            if (cartItem != null) {
                totalDiscount += (cartItem.getPrice() * cartItem.getQuantity() * coupon.getDiscount()) / 100;
            }
        } else if (coupon instanceof BxGyCoupon) {
            totalDiscount += calculateBxGyDiscount(coupon, cartMap);
        }

        return totalDiscount;
    }

    private double calculateBxGyDiscount(Coupon coupon, Map<Long, CartItem> cartMap) {
        double totalDiscount = 0;
        int totalBuyQuantity = 0;
        boolean canApply = true;

        for (BuyProduct buyProduct : ((BxGyCoupon) coupon).getBuyProducts()) {
            CartItem cartItem = cartMap.get(buyProduct.getProduct_id());
            if (cartItem == null || cartItem.getQuantity() < buyProduct.getQuantity()) {
                canApply = false;
                break;
            }
            totalBuyQuantity += cartItem.getQuantity() / buyProduct.getQuantity();
        }

        if (canApply) {
            freeProductCount = Math.min(totalBuyQuantity, ((BxGyCoupon) coupon).getRepetition_limit());
            for (GetProduct getProduct : ((BxGyCoupon) coupon).getGetProducts()) {
                CartItem getItem = cartMap.get(getProduct.getProduct_id());
                if (getItem != null) {
                    totalDiscount += getItem.getPrice() * freeProductCount;
                }
            }
        }

        return totalDiscount;
    }

    private List<Map<String, Object>> prepareUpdatedItems(List<CartItem> cartItems, Coupon coupon, double totalDiscount) {
        List<Map<String, Object>> updatedItems = new ArrayList<>();

        for (CartItem item : cartItems) {
            Map<String, Object> updatedItem = new HashMap<>();
            updatedItem.put("product_id", item.getProduct_id());
            updatedItem.put("quantity", item.getQuantity());
            updatedItem.put("price", item.getPrice());

            if (coupon instanceof ProductWiseCoupon && Objects.equals(item.getProduct_id(), ((ProductWiseCoupon) coupon).getProductId())) {
                updatedItem.put("total_discount", (item.getPrice() * item.getQuantity() * coupon.getDiscount()) / 100);
            } else if (coupon instanceof BxGyCoupon) {
                for (GetProduct getProduct : ((BxGyCoupon) coupon).getGetProducts()) {
                    if (Objects.equals(item.getProduct_id(), getProduct.getProduct_id())) {
                        updatedItem.put("total_discount", totalDiscount);
                        updatedItem.put("quantity", item.getQuantity()+freeProductCount);
                    } else {
                        updatedItem.put("total_discount", 0);
                    }
                }

            } else {
                updatedItem.put("total_discount", 0);
            }

            updatedItems.add(updatedItem);
        }

        return updatedItems;
    }

    private Coupon createCartWiseCoupon(CouponRequest couponRequest) {
        CartWiseCoupon cartCoupon = new CartWiseCoupon();
        cartCoupon.setDiscount(couponRequest.getDetails().getDiscount());
        cartCoupon.setThreshold(couponRequest.getDetails().getThreshold());
        return cartCoupon;
    }

    private Coupon createProductWiseCoupon(CouponRequest couponRequest) {
        ProductWiseCoupon productCoupon = new ProductWiseCoupon();
        productCoupon.setProductId(couponRequest.getDetails().getProduct_id());
        productCoupon.setDiscount(couponRequest.getDetails().getDiscount());
        return productCoupon;
    }

    private Coupon createBxGyCoupon(CouponRequest couponRequest) {
        BxGyCoupon bxGyCoupon = new BxGyCoupon();
        bxGyCoupon.setRepetition_limit(couponRequest.getDetails().getRepetition_limit());
        bxGyCoupon.setDiscount(couponRequest.getDetails().getDiscount());

        // Set up buy products
        for (BuyProduct buyProduct : couponRequest.getDetails().getBuy_products()) {
            BuyProduct bp = new BuyProduct();
            bp.setProduct_id(buyProduct.getProduct_id());
            bp.setQuantity(buyProduct.getQuantity());
            bp.setCoupon(bxGyCoupon);
            bxGyCoupon.getBuyProducts().add(bp);
        }

        // Set up get products
        for (GetProduct getProduct : couponRequest.getDetails().getGet_products()) {
            GetProduct gp = new GetProduct();
            gp.setProduct_id(getProduct.getProduct_id());
            gp.setQuantity(getProduct.getQuantity());
            gp.setCoupon(bxGyCoupon);
            bxGyCoupon.getGetProducts().add(gp);
        }

        return bxGyCoupon;
    }

    private CouponResponse prepareCouponResponse(Coupon coupon) {
        CouponResponse response = new CouponResponse();
        response.setId(coupon.getId());
        response.setDiscount(coupon.getDiscount());
        response.setType(String.valueOf(coupon.getClass()).split("\\.")[4]);
        return response;
    }

    private void updateCouponDetails(Coupon existingCoupon, CouponRequest updatedCouponDetails) {
        if (existingCoupon instanceof CartWiseCoupon) {
            updateCartWiseCoupon((CartWiseCoupon) existingCoupon, updatedCouponDetails);
        } else if (existingCoupon instanceof ProductWiseCoupon) {
            updateProductWiseCoupon((ProductWiseCoupon) existingCoupon, updatedCouponDetails);
        } else if (existingCoupon instanceof BxGyCoupon) {
            updateBxGyCoupon((BxGyCoupon) existingCoupon, updatedCouponDetails);
        }
    }

    private void updateCartWiseCoupon(CartWiseCoupon coupon, CouponRequest updatedCouponDetails) {
        Optional.ofNullable(updatedCouponDetails.getDetails().getThreshold()).ifPresent(coupon::setThreshold);

        Optional.ofNullable(updatedCouponDetails.getDetails().getDiscount()).ifPresent(coupon::setDiscount);
    }

    private void updateProductWiseCoupon(ProductWiseCoupon coupon, CouponRequest updatedCouponDetails) {
        Optional.ofNullable(updatedCouponDetails.getDetails().getProduct_id()).ifPresent(coupon::setProductId);

        Optional.ofNullable(updatedCouponDetails.getDetails().getDiscount()).ifPresent(coupon::setDiscount);
    }

    private void updateBxGyCoupon(BxGyCoupon coupon, CouponRequest updatedCouponDetails) {
        Optional.ofNullable(updatedCouponDetails.getDetails().getRepetition_limit()).ifPresent(coupon::setRepetition_limit);

        // Update associated BuyProducts
        if (updatedCouponDetails.getDetails().getBuy_products() != null) {
            // Assuming you want to completely replace BuyProducts
            coupon.getBuyProducts().clear();
            for (BuyProduct buyProduct : updatedCouponDetails.getDetails().getBuy_products()) {
                buyProduct.setCoupon(coupon);
                coupon.getBuyProducts().add(buyProduct); // Add directly to the existing list
            }
        }

        // Update associated GetProducts
        if (updatedCouponDetails.getDetails().getGet_products() != null) {
            // Assuming you want to completely replace GetProducts
            coupon.getGetProducts().clear();
            for (GetProduct getProduct : updatedCouponDetails.getDetails().getGet_products()) {
                getProduct.setCoupon(coupon);
                coupon.getGetProducts().add(getProduct); // Add directly to the existing list
            }
        }
    }

    private Map<Long, CartItem> buildCartMap(List<CartItem> cartItems) {
        Map<Long, CartItem> cartMap = new HashMap<>();
        for (CartItem item : cartItems) {
            cartMap.put(item.getProduct_id(), item);
        }
        return cartMap;
    }

    private Map<String, Object> getApplicableCoupon(double cartTotal, Map<Long, CartItem> cartMap, Coupon coupon) {
        Map<String, Object> applicableCoupon = null;

        if (coupon instanceof CartWiseCoupon) {
            applicableCoupon = getCartWiseCoupon(cartTotal, (CartWiseCoupon) coupon);
        } else if (coupon instanceof ProductWiseCoupon) {
            applicableCoupon = getProductWiseCoupon(cartMap, (ProductWiseCoupon) coupon);
        } else if (coupon instanceof BxGyCoupon) {
            applicableCoupon = getBxGyCoupon(cartMap, (BxGyCoupon) coupon);
        }

        return applicableCoupon;
    }

    private Map<String, Object> getCartWiseCoupon(double cartTotal, CartWiseCoupon coupon) {
        if (cartTotal > coupon.getThreshold()) {
            double discount = (cartTotal * coupon.getDiscount()) / 100;
            Map<String, Object> applicableCoupon = new HashMap<>();
            applicableCoupon.put("coupon_id", coupon.getId());
            applicableCoupon.put("type", "cart-wise");
            applicableCoupon.put("discount", discount);
            return applicableCoupon;
        }
        return null;
    }

    private Map<String, Object> getProductWiseCoupon(Map<Long, CartItem> cartMap, ProductWiseCoupon coupon) {
        CartItem cartItem = cartMap.get(coupon.getProductId());
        if (cartItem != null) {
            double discount = (cartItem.getPrice() * cartItem.getQuantity() * coupon.getDiscount()) / 100;
            Map<String, Object> applicableCoupon = new HashMap<>();
            applicableCoupon.put("coupon_id", coupon.getId());
            applicableCoupon.put("type", "product-wise");
            applicableCoupon.put("discount", discount);
            return applicableCoupon;
        }
        return null;
    }

    private Map<String, Object> getBxGyCoupon(Map<Long, CartItem> cartMap, BxGyCoupon coupon) {
        int totalBuyQuantity = 0;
        boolean canApply = true;

        for (BuyProduct buyProduct : coupon.getBuyProducts()) {
            CartItem cartItem = cartMap.get(buyProduct.getProduct_id());
            if (cartItem == null || cartItem.getQuantity() < buyProduct.getQuantity()) {
                canApply = false;
                break;
            }
            totalBuyQuantity += cartItem.getQuantity() / buyProduct.getQuantity();
        }

        if (canApply) {
            freeProductCount = Math.min(totalBuyQuantity, coupon.getRepetition_limit());
            double discount = 0;
            for (GetProduct getProduct : coupon.getGetProducts()) {
                CartItem getItem = cartMap.get(getProduct.getProduct_id());
                if (getItem != null) {
                    discount += getItem.getPrice() * freeProductCount;
                }
            }

            Map<String, Object> applicableCoupon = new HashMap<>();
            applicableCoupon.put("coupon_id", coupon.getId());
            applicableCoupon.put("type", "bxgy");
            applicableCoupon.put("discount", discount);
            return applicableCoupon;
        }

        return null;
    }

    private double calculateCartTotal(List<CartItem> cartItems) {
        return cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
    }

}

