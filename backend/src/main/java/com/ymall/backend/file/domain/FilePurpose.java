package com.ymall.backend.file.domain;

public enum FilePurpose {

    PRODUCT_IMAGE("public/products", true),
    REVIEW_IMAGE("public/reviews", true),
    PROFILE_IMAGE("public/profiles", true),
    EVENT_BANNER("public/banners", true),
    PRODUCT_INQUIRY("private/product-inquiries", false),
    REFUND_EVIDENCE("private/refunds", false),
    CUSTOMER_INQUIRY("private/customer-inquiries", false),
    SELLER_DOCUMENT("private/seller-applications", false);

    private final String storageDirectory;
    private final boolean publiclyAccessible;

    FilePurpose(String storageDirectory, boolean publiclyAccessible) {
        this.storageDirectory = storageDirectory;
        this.publiclyAccessible = publiclyAccessible;
    }

    public String storageDirectory() {
        return storageDirectory;
    }

    public boolean isPubliclyAccessible() {
        return publiclyAccessible;
    }
}
