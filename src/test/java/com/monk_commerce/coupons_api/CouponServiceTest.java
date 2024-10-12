package com.monk_commerce.coupons_api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.monk_commerce.coupons_api.dto.*;
import com.monk_commerce.coupons_api.exception.CouponNotFoundException;
import com.monk_commerce.coupons_api.model.*;
import com.monk_commerce.coupons_api.repository.BuyProductRepository;
import com.monk_commerce.coupons_api.repository.CouponRepository;
import com.monk_commerce.coupons_api.repository.GetProductRepository;
import com.monk_commerce.coupons_api.service.CouponService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.*;

public class CouponServiceTest {

    @InjectMocks
    private CouponService couponService;

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private BuyProductRepository buyProductRepository;

    @Mock
    private GetProductRepository getProductRepository;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateCartWiseCoupon() {
        // Arrange
        CouponRequest couponRequest = new CouponRequest();
        couponRequest.setType("cart-wise");
        CouponRequest.CouponDetails details = new CouponRequest.CouponDetails();
        details.setDiscount(10);
        details.setThreshold(200);
        couponRequest.setDetails(details);

        CartWiseCoupon cartWiseCoupon = new CartWiseCoupon();
        cartWiseCoupon.setDiscount(10);
        cartWiseCoupon.setThreshold(200);

        when(couponRepository.save(any(CartWiseCoupon.class))).thenReturn(cartWiseCoupon);

        // Act
        CouponResponse response = couponService.createCoupon(couponRequest);

        // Assert
        assertNotNull(response);
        assertEquals(10, response.getDiscount());
        verify(couponRepository, times(1)).save(any(CartWiseCoupon.class));
    }

    @Test
    public void testCreateProductWiseCoupon() {
        // Arrange
        CouponRequest couponRequest = new CouponRequest();
        couponRequest.setType("product-wise");
        CouponRequest.CouponDetails details = new CouponRequest.CouponDetails();
        details.setProduct_id(1L);
        details.setDiscount(20);
        couponRequest.setDetails(details);

        ProductWiseCoupon productWiseCoupon = new ProductWiseCoupon();
        productWiseCoupon.setProductId(1L);
        productWiseCoupon.setDiscount(20);

        when(couponRepository.save(any(ProductWiseCoupon.class))).thenReturn(productWiseCoupon);

        // Act
        CouponResponse response = couponService.createCoupon(couponRequest);

        // Assert
        assertNotNull(response);
        assertEquals(20, response.getDiscount());
        verify(couponRepository, times(1)).save(any(ProductWiseCoupon.class));
    }

    @Test
    public void testCreateBxGyCoupon() {
        // Arrange
        CouponRequest couponRequest = new CouponRequest();
        couponRequest.setType("bxgy");

        CouponRequest.CouponDetails details = new CouponRequest.CouponDetails();
        details.setRepetition_limit(2);

        
        List<BuyProduct> buyProducts = new ArrayList<>();
        BuyProduct buyProduct = new BuyProduct();
        buyProduct.setProduct_id(1L);
        buyProduct.setQuantity(2);
        buyProducts.add(buyProduct);
        details.setBuy_products(buyProducts);  

        
        List<GetProduct> getProducts = new ArrayList<>();
        GetProduct getProduct = new GetProduct();
        getProduct.setProduct_id(2L);
        getProduct.setQuantity(1);
        getProducts.add(getProduct);
        details.setGet_products(getProducts);  

        couponRequest.setDetails(details);

        BxGyCoupon bxGyCoupon = new BxGyCoupon();
        bxGyCoupon.setRepetition_limit(2);

        when(couponRepository.save(any(BxGyCoupon.class))).thenReturn(bxGyCoupon);

        // Act
        CouponResponse response = couponService.createCoupon(couponRequest);

        // Assert
        assertNotNull(response);
        assertEquals(2, bxGyCoupon.getRepetition_limit());
        verify(couponRepository, times(1)).save(any(BxGyCoupon.class));
    }


    @Test
    public void testUpdateCoupon() {
        // Arrange
        CouponRequest updatedDetails = new CouponRequest();
        CouponRequest.CouponDetails details = new CouponRequest.CouponDetails();
        details.setDiscount(15);
        updatedDetails.setDetails(details);

        CartWiseCoupon existingCoupon = new CartWiseCoupon();
        existingCoupon.setId(1L);
        existingCoupon.setDiscount(10);
        existingCoupon.setThreshold(200);

        when(couponRepository.findById(1L)).thenReturn(Optional.of(existingCoupon));
        when(couponRepository.save(any(CartWiseCoupon.class))).thenReturn(existingCoupon);

        // Act
        Coupon updatedCoupon = couponService.updateCoupon(1L, updatedDetails);

        // Assert
        assertNotNull(updatedCoupon);
        assertEquals(15, updatedCoupon.getDiscount());
        verify(couponRepository, times(1)).save(any(CartWiseCoupon.class));
    }

    @Test
    public void testUpdateCouponNotFound() {
        // Arrange
        CouponRequest updatedDetails = new CouponRequest();
        when(couponRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CouponNotFoundException.class, () -> {
            couponService.updateCoupon(1L, updatedDetails);
        });
    }


    @Test
    public void testGetApplicableCoupons_CartWiseCoupon() {
        // Arrange
        Cart cart = new Cart();
        cart.setItems(Arrays.asList(
                new CartItem(1L, 2, 100.0),
                new CartItem(2L, 1, 200.0)  
        ));

        CartWiseCoupon cartWiseCoupon = new CartWiseCoupon();
        cartWiseCoupon.setId(1L);
        cartWiseCoupon.setDiscount(10); 
        cartWiseCoupon.setThreshold(250); 

        when(couponRepository.findAll()).thenReturn(Collections.singletonList(cartWiseCoupon));

        // Act
        List<Map<String, Object>> result = couponService.getApplicableCoupons(new CartRequest(cart));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("cart-wise", result.get(0).get("type"));
        assertEquals(40.0, result.get(0).get("discount"));
    }

    @Test
    public void testGetApplicableCoupons_ProductWiseCoupon() {
        // Arrange
        Cart cart = new Cart();
        cart.setItems(Arrays.asList(
                new CartItem(1L, 2, 100.0),
                new CartItem(2L, 1, 200.0)
        ));

        ProductWiseCoupon productWiseCoupon = new ProductWiseCoupon();
        productWiseCoupon.setId(2L);
        productWiseCoupon.setProductId(1L);
        productWiseCoupon.setDiscount(20);

        when(couponRepository.findAll()).thenReturn(Collections.singletonList(productWiseCoupon));

        // Act
        List<Map<String, Object>> result = couponService.getApplicableCoupons(new CartRequest(cart));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("product-wise", result.get(0).get("type"));
        assertEquals(40.0, result.get(0).get("discount"));
    }

    @Test
    public void testGetApplicableCoupons_BxGyCoupon() {
        // Arrange
        Cart cart = new Cart();
        cart.setItems(Arrays.asList(
                new CartItem(1L, 5, 100.0),
                new CartItem(2L, 2, 200.0)
        ));

        BxGyCoupon bxGyCoupon = new BxGyCoupon();
        bxGyCoupon.setId(3L);
        bxGyCoupon.setRepetition_limit(2);

        
        BuyProduct buyProduct = new BuyProduct();
        buyProduct.setProduct_id(1L);
        buyProduct.setQuantity(2);

        GetProduct getProduct = new GetProduct();
        getProduct.setProduct_id(2L);
        getProduct.setQuantity(1);

        bxGyCoupon.setBuyProducts(Collections.singletonList(buyProduct));
        bxGyCoupon.setGetProducts(Collections.singletonList(getProduct));

        when(couponRepository.findAll()).thenReturn(Collections.singletonList(bxGyCoupon));

        // Act
        List<Map<String, Object>> result = couponService.getApplicableCoupons(new CartRequest(cart));

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("bxgy", result.get(0).get("type"));
        assertEquals(400.0, result.get(0).get("discount"));
    }

    @Test
    public void testGetApplicableCoupons_NoCoupons() {
        // Arrange
        Cart cart = new Cart();
        cart.setItems(Arrays.asList(
                new CartItem(1L, 2, 100.0),
                new CartItem(2L, 1, 200.0)
        ));

        when(couponRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = couponService.getApplicableCoupons(new CartRequest(cart));

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testGetApplicableCoupons_NullCartItems() {
        // Arrange
        Cart cart = new Cart();
        cart.setItems(null);

        when(couponRepository.findAll()).thenReturn(Collections.emptyList());

        // Act
        List<Map<String, Object>> result = couponService.getApplicableCoupons(new CartRequest(cart));

        // Assert
        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    public void testDeleteCoupon() {
        // Arrange
        Long couponId = 1L;

        // Act
        couponService.deleteCoupon(couponId);

        // Assert
        verify(couponRepository, times(1)).deleteById(couponId);
    }

    @Test
    public void testApplyCoupon_CartWise() {
        // Arrange
        Long couponId = 1L;
        CartRequest cartRequest = new CartRequest();
        Cart cart = new Cart();
        cart.setItems(Arrays.asList(new CartItem(1L, 2, 100.0)));
        cartRequest.setCart(cart);

        CartWiseCoupon cartWiseCoupon = new CartWiseCoupon();
        cartWiseCoupon.setDiscount(10);

        when(couponRepository.findById(couponId)).thenReturn(Optional.of(cartWiseCoupon));

        // Act
        Map<String, Object> result = couponService.applyCoupon(couponId, cartRequest);

        // Assert
        assertNotNull(result);
        assertEquals(200.0, result.get("total_price"));
        assertEquals(20.0, result.get("total_discount"));
        assertEquals(180.0, result.get("final_price"));
    }

    @Test
    public void testGetCouponById_CouponNotFound() {
        // Arrange
        Long couponId = 1L;
        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CouponNotFoundException.class, () -> {
            couponService.getCouponById(couponId);
        });
    }
}

