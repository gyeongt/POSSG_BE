package possg.com.a.dto;

public class ProductDto {
    private int productSeq;
    private int categoryId;
    private String productName;
    private int price;
    private int priceDiscount;
    private int stockQuantity;
    private String expirationDate;
    private double discountRate;
    private int promotionInfo;
    private String barcode;
    private String imgUrl;

    // 생성자
    public ProductDto() {}
    
    public ProductDto(String productName) {
    	super();
    	this.productName = productName;
    }
    
	public ProductDto(int productSeq, int categoryId, String productName, int price, int priceDiscount, int stockQuantity,
			String expirationDate, double discountRate, int promotionInfo, String barcode, String imgUrl) {
		super();
		this.productSeq = productSeq;
		this.categoryId = categoryId;
		this.productName = productName;
		this.price = price;
		this.priceDiscount = priceDiscount;
		this.stockQuantity = stockQuantity;
		this.expirationDate = expirationDate;
		this.discountRate = discountRate;
		this.promotionInfo = promotionInfo;
		this.barcode = barcode;
		this.imgUrl = imgUrl;
	}

	public int getProductSeq() {
		return productSeq;
	}

	public void setProductSeq(int productSeq) {
		this.productSeq = productSeq;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	public String getProductName() {
		return productName;
	}

	public void setProductName(String productName) {
		this.productName = productName;
	}

	public int getPrice() {
		return price;
	}

	public void setPrice(int price) {
		this.price = price;
	}

	public int getPriceDiscount() {
		return priceDiscount;
	}

	public void setPriceDiscount(int priceDiscount) {
		this.priceDiscount = priceDiscount;
	}

	public int getStockQuantity() {
		return stockQuantity;
	}

	public void setStockQuantity(int stockQuantity) {
		this.stockQuantity = stockQuantity;
	}

	public String getExpirationDate() {
		return expirationDate;
	}

	public void setExpirationDate(String expirationDate) {
		this.expirationDate = expirationDate;
	}

	public double getDiscountRate() {
		return discountRate;
	}

	public void setDiscountRate(double discountRate) {
		this.discountRate = discountRate;
	}

	public int getPromotionInfo() {
		return promotionInfo;
	}

	public void setPromotionInfo(int promotionInfo) {
		this.promotionInfo = promotionInfo;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getImgUrl() {
		return imgUrl;
	}

	public void setImgUrl(String imgUrl) {
		this.imgUrl = imgUrl;
	}

	@Override
	public String toString() {
		return "ProductDto [productSeq=" + productSeq + ", categoryId=" + categoryId + ", productName=" + productName
				+ ", price=" + price + ", priceDiscount=" + priceDiscount + ", stockQuantity=" + stockQuantity
				+ ", expirationDate=" + expirationDate + ", discountRate=" + discountRate + ", promotionInfo="
				+ promotionInfo + ", barcode=" + barcode + ", imgUrl=" + imgUrl + "]";
	}

}