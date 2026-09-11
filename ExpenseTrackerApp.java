import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Drives the console user interface: menus, prompts and wiring between the
 * various manager/service classes. Contains no business logic itself -
 * that lives in ExpenseManager, CategoryManager and ReportGenerator.
 */
public class ExpenseTrackerApp {

    private final Scanner scanner = new Scanner(System.in);
    private final ExpenseManager expenseManager = new ExpenseManager();
    private final CategoryManager categoryManager = new CategoryManager();
    private final DataManager dataManager;
    private final ReportGenerator reportGenerator;

    public ExpenseTrackerApp(String expensesFilePath, String categoriesFilePath) {
        this.dataManager = new DataManager(expensesFilePath, categoriesFilePath);
        this.reportGenerator = new ReportGenerator(expenseManager);
    }

    public void run() {
        run(false);
    }

    /**
     * Starts the application. When loadSampleData is true and no expenses
     * were restored from disk, a small set of sample expenses is inserted
     * to make manual testing/demoing easier. Sample data is never inserted
     * on top of a user's existing real data.
     */
    public void run(boolean loadSampleData) {
        dataManager.loadCategories(categoryManager);
        dataManager.loadExpenses(expenseManager);

        if (loadSampleData && expenseManager.isEmpty()) {
            insertSampleData();
            dataManager.saveExpenses(expenseManager);
            System.out.println("Sample test data loaded.");
        }

        System.out.println("Data loaded successfully.");

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readMenuChoice();
            switch (choice) {
                case 1: addExpenseFlow(); break;
                case 2: viewAllExpensesFlow(); break;
                case 3: trackingMenu(); break;
                case 4: filterMenu(); break;
                case 5: modifyExpenseFlow(); break;
                case 6: deleteExpenseFlow(); break;
                case 7: categoryMenu(); break;
                case 8: reportMenu(); break;
                case 9: reportGenerator.printSpendingInsights(); break;
                case 10:
                    running = false;
                    System.out.println("Thank you for using Personal Expense Tracker. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid choice. Please select a number between 1 and 10.");
            }
        }
        scanner.close();
    }

    /**
     * Inserts a small, fixed set of sample expenses for demo/testing
     * purposes only. Only called when explicitly enabled and only when the
     * expense list is currently empty (see run(boolean)).
     */
    private void insertSampleData() {
        expenseManager.addExpense(LocalDate.of(2026, 9, 1), new BigDecimal("250"), categoryManager.getCanonicalName("Food"), "Lunch");
        expenseManager.addExpense(LocalDate.of(2026, 9, 2), new BigDecimal("1200"), categoryManager.getCanonicalName("Shopping"), "Shoes");
        expenseManager.addExpense(LocalDate.of(2026, 9, 3), new BigDecimal("100"), categoryManager.getCanonicalName("Transportation"), "Bus");
        expenseManager.addExpense(LocalDate.of(2026, 9, 5), new BigDecimal("3500"), categoryManager.getCanonicalName("Travel"), "Hotel");
        expenseManager.addExpense(LocalDate.of(2026, 9, 6), new BigDecimal("800"), categoryManager.getCanonicalName("Entertainment"), "Movie");
    }

    // ==================== MAIN MENU ====================

    private void printMainMenu() {
        System.out.println();
        System.out.println("==================================================");
        System.out.println("PERSONAL EXPENSE TRACKER");
        System.out.println("==================================================");
        System.out.println("1. Add Expense");
        System.out.println("2. View All Expenses");
        System.out.println("3. Track Expenses");
        System.out.println("4. Filter Expenses");
        System.out.println("5. Modify Expense");
        System.out.println("6. Delete Expense");
        System.out.println("7. Manage Categories");
        System.out.println("8. Reports");
        System.out.println("9. Spending Insights");
        System.out.println("10. Exit");
        System.out.println("==================================================");
        System.out.print("Enter your choice: ");
    }

    // ==================== 1. ADD EXPENSE ====================

    private void addExpenseFlow() {
        System.out.println();
        System.out.println("---------------- ADD EXPENSE ----------------");

        if (categoryManager.isEmpty()) {
            System.out.println("No categories exist. Please add a category first.");
            return;
        }

        LocalDate date = readRequiredDate("Enter Date (YYYY-MM-DD): ");
        BigDecimal amount = readRequiredAmount("Enter Amount: ");
        String category = readRequiredCategory();
        String description = readRequiredDescription("Enter Description: ");

        Expense expense = expenseManager.addExpense(date, amount, category, description);
        dataManager.saveExpenses(expenseManager);

        System.out.println("Expense added successfully.");
        System.out.println("Generated Expense ID: " + expense.getId());
    }

    // ==================== 2. VIEW ALL EXPENSES ====================

    private void viewAllExpensesFlow() {
        System.out.println();
        List<Expense> all = expenseManager.getAllExpensesSorted();
        printExpenseTable(all, "ALL EXPENSES");
    }

    private void printExpenseTable(List<Expense> list, String title) {
        System.out.println("---------------------------------------------------------");
        System.out.println(title);
        System.out.println("---------------------------------------------------------");

        if (list.isEmpty()) {
            System.out.println("No expenses recorded yet.");
            System.out.println("---------------------------------------------------------");
            return;
        }

        System.out.printf("%-8s %-12s %-14s %-16s %-20s%n", "ID", "DATE", "AMOUNT", "CATEGORY", "DESCRIPTION");
        BigDecimal total = BigDecimal.ZERO;
        for (Expense e : list) {
            System.out.printf("%-8d %-12s %-14s %-16s %-20s%n",
                    e.getId(), e.getDate(), MoneyUtil.format(e.getAmount()), e.getCategory(), e.getDescription());
            total = total.add(e.getAmount());
        }
        System.out.println("---------------------------------------------------------");
        System.out.println("Total: " + MoneyUtil.format(total) + " (" + list.size() + " transactions)");
        System.out.println("---------------------------------------------------------");
    }

    // ==================== 3. TRACKING MENU ====================

    private void trackingMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("==================================================");
            System.out.println("EXPENSE TRACKING");
            System.out.println("==================================================");
            System.out.println("1. Overall Summary");
            System.out.println("2. Summary by Date Range");
            System.out.println("3. Summary by Category");
            System.out.println("4. Back");
            System.out.println("==================================================");
            System.out.print("Enter your choice: ");
            int choice = readMenuChoice();
            switch (choice) {
                case 1: overallSummary(); break;
                case 2: summaryByDateRange(); break;
                case 3: summaryByCategory(); break;
                case 4: back = true; break;
                default: System.out.println("Invalid choice. Please select a number between 1 and 4.");
            }
        }
    }

    private void overallSummary() {
        List<Expense> all = expenseManager.getAllExpensesSorted();
        System.out.println();
        System.out.println("---------------- OVERALL SUMMARY ----------------");
        if (all.isEmpty()) {
            System.out.println("No expenses recorded yet.");
            return;
        }
        BigDecimal total = expenseManager.getTotalAmount(all);
        BigDecimal average = expenseManager.getAverageAmount(all);
        Expense highest = expenseManager.getHighestExpense(all);
        Expense lowest = expenseManager.getLowestExpense(all);

        System.out.println("Total Expenses: " + MoneyUtil.format(total));
        System.out.println("Number of Transactions: " + all.size());
        System.out.println("Average Expense: " + MoneyUtil.format(average));
        System.out.println("Highest Expense: " + MoneyUtil.format(highest.getAmount()));
        System.out.println("Lowest Expense: " + MoneyUtil.format(lowest.getAmount()));
    }

    private void summaryByDateRange() {
        System.out.println();
        System.out.println("---------------- SUMMARY BY DATE RANGE ----------------");
        LocalDate start = readRequiredDate("Start Date (YYYY-MM-DD): ");
        LocalDate end = readValidatedDateRangeEnd(start);

        List<Expense> matches = expenseManager.filterByDateRange(start, end);
        BigDecimal total = expenseManager.getTotalAmount(matches);
        System.out.println("Number of Transactions: " + matches.size());
        System.out.println("Total Expenses from " + start + " to " + end + ": " + MoneyUtil.format(total));
    }

    private void summaryByCategory() {
        System.out.println();
        System.out.println("---------------- SUMMARY BY CATEGORY ----------------");
        Map<String, BigDecimal> totals = expenseManager.getTotalsByCategory();
        if (totals.isEmpty()) {
            System.out.println("No expenses recorded yet.");
            return;
        }
        BigDecimal grandTotal = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : totals.entrySet()) {
            System.out.printf("%-16s %s%n", entry.getKey(), MoneyUtil.format(entry.getValue()));
            grandTotal = grandTotal.add(entry.getValue());
        }
        System.out.println("---------------------------------------------------------");
        System.out.println("Grand Total: " + MoneyUtil.format(grandTotal));
    }

    // ==================== 4. FILTER MENU ====================

    private void filterMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("==================================================");
            System.out.println("FILTER EXPENSES");
            System.out.println("==================================================");
            System.out.println("1. Filter by Date Range");
            System.out.println("2. Filter by Category");
            System.out.println("3. Filter by Amount Range");
            System.out.println("4. Combined Filter");
            System.out.println("5. Back");
            System.out.println("==================================================");
            System.out.print("Enter your choice: ");
            int choice = readMenuChoice();
            switch (choice) {
                case 1: filterByDateRangeFlow(); break;
                case 2: filterByCategoryFlow(); break;
                case 3: filterByAmountRangeFlow(); break;
                case 4: combinedFilterFlow(); break;
                case 5: back = true; break;
                default: System.out.println("Invalid choice. Please select a number between 1 and 5.");
            }
        }
    }

    private void filterByDateRangeFlow() {
        LocalDate start = readRequiredDate("Start Date (YYYY-MM-DD): ");
        LocalDate end = readValidatedDateRangeEnd(start);
        List<Expense> matches = expenseManager.filterByDateRange(start, end);
        printFilterResults(matches);
    }

    private void filterByCategoryFlow() {
        String category = readRequiredCategory();
        List<Expense> matches = expenseManager.filterByCategory(category);
        printFilterResults(matches);
    }

    private void filterByAmountRangeFlow() {
        BigDecimal min = readRequiredAmount("Minimum Amount: ");
        BigDecimal max = readValidatedAmountRangeMax(min);
        List<Expense> matches = expenseManager.filterByAmountRange(min, max);
        printFilterResults(matches);
    }

    private void combinedFilterFlow() {
        System.out.println("Leave any field blank to skip that criterion.");

        LocalDate start = readOptionalDate("Start Date (YYYY-MM-DD, optional): ");
        LocalDate end = readOptionalDate("End Date (YYYY-MM-DD, optional): ");
        try {
            InputValidator.validateDateRange(start, end);
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
            return;
        }

        String category = readOptionalCategory("Category (optional): ");

        BigDecimal min = readOptionalAmount("Minimum Amount (optional): ");
        BigDecimal max = readOptionalAmount("Maximum Amount (optional): ");
        try {
            InputValidator.validateAmountRange(min, max);
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
            return;
        }

        List<Expense> matches = expenseManager.combinedFilter(start, end, category, min, max);
        printFilterResults(matches);
    }

    private void printFilterResults(List<Expense> matches) {
        System.out.println();
        if (matches.isEmpty()) {
            System.out.println("No expenses found matching the selected criteria.");
            return;
        }
        printExpenseTable(matches, "FILTERED RESULTS");
    }

    // ==================== 5. MODIFY EXPENSE ====================

    private void modifyExpenseFlow() {
        System.out.println();
        System.out.println("---------------- MODIFY EXPENSE ----------------");
        int id = readInt("Enter Expense ID: ");
        Expense expense = expenseManager.findById(id);
        if (expense == null) {
            System.out.println("Expense ID not found.");
            return;
        }

        System.out.println("Current Details -> " + expense);
        System.out.println("What would you like to modify?");
        System.out.println("1. Date");
        System.out.println("2. Amount");
        System.out.println("3. Category");
        System.out.println("4. Description");
        System.out.println("5. All Fields");
        System.out.print("Enter your choice: ");
        int choice = readMenuChoice();

        System.out.println("Press Enter to keep the existing value for any field.");

        switch (choice) {
            case 1:
                updateDate(expense);
                break;
            case 2:
                updateAmount(expense);
                break;
            case 3:
                updateCategory(expense);
                break;
            case 4:
                updateDescription(expense);
                break;
            case 5:
                updateDate(expense);
                updateAmount(expense);
                updateCategory(expense);
                updateDescription(expense);
                break;
            default:
                System.out.println("Invalid choice. No changes made.");
                return;
        }

        dataManager.saveExpenses(expenseManager);
        System.out.println("Expense updated successfully.");
    }

    private void updateDate(Expense expense) {
        while (true) {
            System.out.print("New Date [" + expense.getDate() + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return;
            try {
                expense.setDate(InputValidator.parseDate(input));
                return;
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void updateAmount(Expense expense) {
        while (true) {
            System.out.print("New Amount [" + expense.getAmount() + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return;
            try {
                expense.setAmount(InputValidator.parseAmount(input));
                return;
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private void updateCategory(Expense expense) {
        while (true) {
            System.out.print("New Category [" + expense.getCategory() + "]: ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return;
            if (!categoryManager.exists(input)) {
                System.out.println("Category '" + input + "' does not exist. Use category management to add it first.");
                continue;
            }
            expense.setCategory(categoryManager.getCanonicalName(input));
            return;
        }
    }

    private void updateDescription(Expense expense) {
        while (true) {
            System.out.print("New Description [" + expense.getDescription() + "]: ");
            String input = scanner.nextLine();
            if (input.trim().isEmpty()) return;
            try {
                expense.setDescription(InputValidator.validateDescription(input));
                return;
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    // ==================== 6. DELETE EXPENSE ====================

    private void deleteExpenseFlow() {
        System.out.println();
        System.out.println("---------------- DELETE EXPENSE ----------------");
        int id = readInt("Enter Expense ID: ");
        Expense expense = expenseManager.findById(id);
        if (expense == null) {
            System.out.println("Expense ID not found.");
            return;
        }

        System.out.println("Expense Details");
        System.out.println("ID: " + expense.getId());
        System.out.println("Date: " + expense.getDate());
        System.out.println("Amount: " + MoneyUtil.format(expense.getAmount()));
        System.out.println("Category: " + expense.getCategory());
        System.out.println("Description: " + expense.getDescription());

        System.out.print("Are you sure you want to delete this expense? (Y/N): ");
        String confirm = scanner.nextLine().trim();
        if (confirm.equalsIgnoreCase("Y")) {
            expenseManager.deleteExpense(id);
            dataManager.saveExpenses(expenseManager);
            System.out.println("Expense deleted successfully.");
        } else {
            System.out.println("Deletion cancelled.");
        }
    }

    // ==================== 7. CATEGORY MENU ====================

    private void categoryMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("==================================================");
            System.out.println("CATEGORY MANAGEMENT");
            System.out.println("==================================================");
            System.out.println("1. View Categories");
            System.out.println("2. Add Category");
            System.out.println("3. Rename Category");
            System.out.println("4. Delete Category");
            System.out.println("5. Back");
            System.out.println("==================================================");
            System.out.print("Enter your choice: ");
            int choice = readMenuChoice();
            switch (choice) {
                case 1: viewCategoriesFlow(); break;
                case 2: addCategoryFlow(); break;
                case 3: renameCategoryFlow(); break;
                case 4: deleteCategoryFlow(); break;
                case 5: back = true; break;
                default: System.out.println("Invalid choice. Please select a number between 1 and 5.");
            }
        }
    }

    private void viewCategoriesFlow() {
        System.out.println();
        System.out.println("---------------- CATEGORIES ----------------");
        List<Category> categories = categoryManager.getAllCategories();
        if (categories.isEmpty()) {
            System.out.println("No categories defined.");
            return;
        }
        int index = 1;
        for (Category c : categories) {
            int count = expenseManager.countByCategory(c.getName());
            System.out.println(index + ". " + c.getName() + " (" + count + " expense(s))");
            index++;
        }
    }

    private void addCategoryFlow() {
        System.out.print("Enter new category name: ");
        String name = scanner.nextLine();
        try {
            categoryManager.addCategory(name);
            dataManager.saveCategories(categoryManager);
            System.out.println("Category added successfully.");
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }

    private void renameCategoryFlow() {
        System.out.print("Enter category to rename: ");
        String oldName = scanner.nextLine();
        System.out.print("Enter new name: ");
        String newName = scanner.nextLine();
        try {
            categoryManager.renameCategory(oldName, newName);
            String canonical = categoryManager.getCanonicalName(newName);
            expenseManager.reassignCategory(oldName, canonical);
            dataManager.saveCategories(categoryManager);
            dataManager.saveExpenses(expenseManager);
            System.out.println("Category renamed successfully.");
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }

    private void deleteCategoryFlow() {
        System.out.print("Enter category to delete: ");
        String name = scanner.nextLine();

        if (!categoryManager.exists(name)) {
            System.out.println("Category '" + name.trim() + "' does not exist.");
            return;
        }

        int usageCount = expenseManager.countByCategory(name.trim());
        if (usageCount > 0) {
            System.out.println("Cannot delete category '" + name.trim() + "' because " + usageCount
                    + " expense(s) are associated with it.");
            System.out.print("Would you like to reassign these expenses to another category first? (Y/N): ");
            String choice = scanner.nextLine().trim();
            if (!choice.equalsIgnoreCase("Y")) {
                System.out.println("Deletion cancelled.");
                return;
            }
            System.out.print("Enter the category to reassign these expenses to: ");
            String targetName = scanner.nextLine();
            if (!categoryManager.exists(targetName) || targetName.trim().equalsIgnoreCase(name.trim())) {
                System.out.println("Invalid target category. Deletion cancelled.");
                return;
            }
            String canonicalTarget = categoryManager.getCanonicalName(targetName);
            expenseManager.reassignCategory(name, canonicalTarget);
            dataManager.saveExpenses(expenseManager);
        }

        try {
            categoryManager.deleteCategory(name);
            dataManager.saveCategories(categoryManager);
            System.out.println("Category deleted successfully.");
        } catch (ValidationException e) {
            System.out.println(e.getMessage());
        }
    }

    // ==================== 8. REPORT MENU ====================

    private void reportMenu() {
        boolean back = false;
        while (!back) {
            System.out.println();
            System.out.println("==================================================");
            System.out.println("REPORTS");
            System.out.println("==================================================");
            System.out.println("1. Monthly Expense Report");
            System.out.println("2. Category-wise Report");
            System.out.println("3. Yearly Expense Report");
            System.out.println("4. Back");
            System.out.println("==================================================");
            System.out.print("Enter your choice: ");
            int choice = readMenuChoice();
            switch (choice) {
                case 1: monthlyReportFlow(); break;
                case 2: reportGenerator.printCategoryWiseReport(); break;
                case 3: yearlyReportFlow(); break;
                case 4: back = true; break;
                default: System.out.println("Invalid choice. Please select a number between 1 and 4.");
            }
        }
    }

    private void monthlyReportFlow() {
        int year = readYear("Enter year: ");
        int month = readMonth("Enter month: ");
        reportGenerator.printMonthlyReport(year, month);
    }

    private void yearlyReportFlow() {
        int year = readYear("Enter year: ");
        reportGenerator.printYearlyReport(year);
    }

    // ==================== INPUT HELPERS ====================

    private int readMenuChoice() {
        String input = scanner.nextLine().trim();
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Invalid input. Please enter a valid number.");
            return -1;
        }
    }

    private int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a valid number.");
            }
        }
    }

    private int readYear(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return InputValidator.validateYear(input);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private int readMonth(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return InputValidator.validateMonth(input);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private LocalDate readRequiredDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return InputValidator.parseDate(input);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private LocalDate readOptionalDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return null;
            try {
                return InputValidator.parseDate(input);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private LocalDate readValidatedDateRangeEnd(LocalDate start) {
        while (true) {
            LocalDate end = readRequiredDate("End Date (YYYY-MM-DD): ");
            try {
                InputValidator.validateDateRange(start, end);
                return end;
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private BigDecimal readRequiredAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return InputValidator.parseAmount(input);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private BigDecimal readOptionalAmount(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return null;
            try {
                return InputValidator.parseAmount(input);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private BigDecimal readValidatedAmountRangeMax(BigDecimal min) {
        while (true) {
            BigDecimal max = readRequiredAmount("Maximum Amount: ");
            try {
                InputValidator.validateAmountRange(min, max);
                return max;
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    private String readRequiredCategory() {
        while (true) {
            List<Category> categories = categoryManager.getAllCategories();
            System.out.println("Available categories: " + categories);
            System.out.print("Enter Category: ");
            String input = scanner.nextLine();
            if (input.trim().isEmpty()) {
                System.out.println("Category cannot be empty.");
                continue;
            }
            if (!categoryManager.exists(input)) {
                System.out.println("Category '" + input.trim() + "' does not exist. Please choose from the list above.");
                continue;
            }
            return categoryManager.getCanonicalName(input);
        }
    }

    private String readOptionalCategory(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) return null;
            if (!categoryManager.exists(input)) {
                System.out.println("Category '" + input + "' does not exist.");
                continue;
            }
            return categoryManager.getCanonicalName(input);
        }
    }

    private String readRequiredDescription(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine();
            try {
                return InputValidator.validateDescription(input);
            } catch (ValidationException e) {
                System.out.println(e.getMessage());
            }
        }
    }
}
