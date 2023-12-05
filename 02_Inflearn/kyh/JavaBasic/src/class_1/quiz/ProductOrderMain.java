package class_1.quiz;

public class ProductOrderMain {
    public static void main(String[] args) {
        ProductOrder[] productOrders = {
            new ProductOrder("1", 100, 10),
            new ProductOrder("2", 200, 2)
        };

        int totalAmount = 0;
        for (ProductOrder productOrder : productOrders) {
            productOrder.print();
            totalAmount += productOrder.calc();
        }

        System.out.println("totalAmount = " + totalAmount);
    }

}
