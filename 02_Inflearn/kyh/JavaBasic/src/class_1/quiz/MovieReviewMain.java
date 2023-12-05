package class_1.quiz;

public class MovieReviewMain {
    public static void main(String[] args) {
        MovieReview[] movieReviews = {
            new MovieReview("아바타", "1000만영화다"),
            new MovieReview("범죄도시", "장르는 마동석")
        };
        for (MovieReview movieReview : movieReviews) {
            movieReview.print();
        }
    }
}
