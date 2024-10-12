## Coupons Management

### Use Cases
- Set a minimum cart value required to apply the coupon.
- Apply coupon at the checkout stage.
- Update cart totals based on applied coupons.
- Notify users of coupon application failures
- Coupons must be used before a specified date.
- Maximum number of times a coupon can be redeemed (per user).
- Define whether multiple coupons can be applied together.

### Assumptions
- Once a coupon is created as a specific type, it cannot be changed to another type.
- Each coupon is applied separately and independently of others. There's no mechanism to apply multiple coupons together.
- The service assumes that all items in the cart exist and are available when applying coupons. There's no validation for stock or availability.

### Limitations
- The coupon table is a single table, which may lead to data redundancy.
- Fetching all available coupons (getAllCoupons()) could lead to performance issues as the dataset grows. Filtering and determining applicable coupons happens in memory, which may not scale well for large datasets.
- There's minimal validation for certain conditions, such as checking if a coupon has expired.