package class_1.quiz;

public class MovieReview {
    String title;
    String review;

    public MovieReview(String title, String review) {
        this.title = title;
        this.review = review;
    }

    public void print() {
        System.out.println("영화: " + this.title + ", 리뷰: " + this.title);
    }
}
