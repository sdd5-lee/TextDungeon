package com.textdungeon.billing;

/** 상점 프리미엄 탭에 보여줄 상품 한 개 */
public final class ProductOffer {
    public final Product product;
    /** Play가 준 이름. 없으면 Product.displayName */
    public final String title;
    /** Play가 준 현지 가격 문구 (예: ₩5,900). 그대로 화면에 쓴다 */
    public final String priceText;
    public final boolean owned;
    public final boolean pending;
    /** 앞 단계 부스터가 있어서 지금 살 수 있는지 */
    public final boolean purchasable;

    public ProductOffer(Product product, String title, String priceText,
                        boolean owned, boolean pending, boolean purchasable) {
        this.product = product;
        this.title = title == null || title.isEmpty() ? product.displayName : title;
        this.priceText = priceText;
        this.owned = owned;
        this.pending = pending;
        this.purchasable = purchasable;
    }
}
