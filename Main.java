/**
 * Application entry point. Wires together the file paths used for
 * persistence and starts the console UI loop.
 *
 * Run with the optional "--sample" argument (e.g. "java -cp bin Main --sample")
 * to pre-load a small set of sample expenses for testing/demo purposes.
 * Sample data is only inserted if no expenses already exist on disk.
 */
public class Main {
    public static void main(String[] args) {
        String expensesFilePath = "data/expenses.csv";
        String categoriesFilePath = "data/categories.txt";

        boolean loadSampleData = false;
        for (String arg : args) {
            if (arg.equalsIgnoreCase("--sample")) {
                loadSampleData = true;
            }
        }

        ExpenseTrackerApp app = new ExpenseTrackerApp(expensesFilePath, categoriesFilePath);
        app.run(loadSampleData);
    }
}
