package class_1.quiz;

public class ProductOrder {
    String productName;
    int price;
    int quantity;

    public ProductOrder(String productName, int price, int quantity) {
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    public int calc() {
        return this.price * this.quantity;
    }

    public void print() {
        System.out.println("상품명: " + this.productName + ", 수량: " +this.quantity+ ", 가격: " + this.calc());
    }
}
