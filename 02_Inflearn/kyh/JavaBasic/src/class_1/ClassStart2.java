package class_1;

public class ClassStart2 {
    public static void main(String[] args) {
        Student[] students = {
            new Student("학생1", 15, 90),
            new Student("학생2", 16, 80)
        };

        for (Student student : students) {
            student.print();
        }
    }
}
