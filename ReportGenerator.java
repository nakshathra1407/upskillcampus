import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Produces formatted monthly, category-wise and yearly reports, as well as
 * general spending insights, from data held in an ExpenseManager.
 */
public class ReportGenerator {

    private final ExpenseManager expenseManager;

    public ReportGenerator(ExpenseManager expenseManager) {
        this.expenseManager = expenseManager;
    }

    public void printMonthlyReport(int year, int month) {
        List<Expense> monthExpenses = expenseManager.getExpensesForMonth(year, month);
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        System.out.println("========================================");
        System.out.println("MONTHLY EXPENSE REPORT");
        System.out.println(monthName + " " + year);
        System.out.println("========================================");

        if (monthExpenses.isEmpty()) {
            System.out.println("No expenses recorded for this period.");
            System.out.println("========================================");
            return;
        }

        BigDecimal total = expenseManager.getTotalAmount(monthExpenses);
        BigDecimal average = expenseManager.getAverageAmount(monthExpenses);

        System.out.println("Total Expenses: " + MoneyUtil.format(total));
        System.out.println("Number of Transactions: " + monthExpenses.size());
        System.out.println("Average Expense: " + MoneyUtil.format(average));
        System.out.println();
        System.out.println("Category Breakdown:");
        Map<String, BigDecimal> byCategory = expenseManager.getTotalsByCategory(monthExpenses);
        for (Map.Entry<String, BigDecimal> entry : byCategory.entrySet()) {
            System.out.printf("%-16s %s%n", entry.getKey(), MoneyUtil.format(entry.getValue()));
        }
        System.out.println("========================================");
    }

    public void printCategoryWiseReport() {
        System.out.println("========================================");
        System.out.println("CATEGORY-WISE REPORT");
        System.out.println("========================================");

        if (expenseManager.isEmpty()) {
            System.out.println("No expenses recorded yet.");
            System.out.println("========================================");
            return;
        }

        Map<String, BigDecimal> totalsByCategory = expenseManager.getTotalsByCategory();
        for (Map.Entry<String, BigDecimal> entry : totalsByCategory.entrySet()) {
            String category = entry.getKey();
            List<Expense> categoryExpenses = expenseManager.filterByCategory(category);
            BigDecimal total = entry.getValue();
            BigDecimal average = expenseManager.getAverageAmount(categoryExpenses);

            System.out.println();
            System.out.println(category.toUpperCase());
            System.out.println("Transactions: " + categoryExpenses.size());
            System.out.println("Total: " + MoneyUtil.format(total));
            System.out.println("Average: " + MoneyUtil.format(average));
        }
        System.out.println("========================================");
    }

    public void printYearlyReport(int year) {
        List<Expense> yearExpenses = expenseManager.getExpensesForYear(year);

        System.out.println("========================================");
        System.out.println("YEARLY EXPENSE REPORT - " + year);
        System.out.println("========================================");

        if (yearExpenses.isEmpty()) {
            System.out.println("No expenses recorded for this year.");
            System.out.println("========================================");
            return;
        }

        System.out.println("Monthly Breakdown:");
        BigDecimal annualTotal = BigDecimal.ZERO;
        for (int m = 1; m <= 12; m++) {
            List<Expense> monthList = expenseManager.getExpensesForMonth(year, m);
            BigDecimal monthTotal = expenseManager.getTotalAmount(monthList);
            annualTotal = annualTotal.add(monthTotal);
            String monthName = Month.of(m).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            System.out.printf("%-12s %s%n", monthName, MoneyUtil.format(monthTotal));
        }

        System.out.println();
        System.out.println("Category Breakdown:");
        Map<String, BigDecimal> byCategory = expenseManager.getTotalsByCategory(yearExpenses);
        for (Map.Entry<String, BigDecimal> entry : byCategory.entrySet()) {
            System.out.printf("%-16s %s%n", entry.getKey(), MoneyUtil.format(entry.getValue()));
        }

        System.out.println();
        System.out.println("Annual Total: " + MoneyUtil.format(annualTotal));
        System.out.println("========================================");
    }

    public void printSpendingInsights() {
        System.out.println("========================================");
        System.out.println("SPENDING INSIGHTS");
        System.out.println("========================================");

        if (expenseManager.isEmpty()) {
            System.out.println("Not enough data to generate insights yet. Add some expenses first.");
            System.out.println("========================================");
            return;
        }

        List<Expense> all = expenseManager.getAllExpensesSorted();
        BigDecimal grandTotal = expenseManager.getTotalAmount(all);

        Map<String, BigDecimal> byCategory = expenseManager.getTotalsByCategory();
        String topCategory = null;
        BigDecimal topCategoryAmount = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : byCategory.entrySet()) {
            if (entry.getValue().compareTo(topCategoryAmount) > 0) {
                topCategory = entry.getKey();
                topCategoryAmount = entry.getValue();
            }
        }

        Expense highest = expenseManager.getHighestExpense(all);
        Expense lowest = expenseManager.getLowestExpense(all);

        // Average monthly spending: total spent divided by number of distinct
        // calendar months that have at least one expense recorded.
        long distinctMonths = all.stream()
                .map(e -> e.getDate().getYear() + "-" + e.getDate().getMonthValue())
                .distinct()
                .count();
        BigDecimal avgMonthly = distinctMonths == 0 ? BigDecimal.ZERO :
                grandTotal.divide(BigDecimal.valueOf(distinctMonths), 2, RoundingMode.HALF_UP);

        System.out.println();
        System.out.println("Highest Spending Category: " + topCategory);
        System.out.println("Amount: " + MoneyUtil.format(topCategoryAmount));

        System.out.println();
        System.out.println("Highest Individual Expense:");
        System.out.println(MoneyUtil.format(highest.getAmount()) + " - " + highest.getCategory()
                + " (" + highest.getDescription() + ")");

        System.out.println();
        System.out.println("Lowest Individual Expense:");
        System.out.println(MoneyUtil.format(lowest.getAmount()) + " - " + lowest.getCategory()
                + " (" + lowest.getDescription() + ")");

        System.out.println();
        System.out.println("Average Monthly Spending:");
        System.out.println(MoneyUtil.format(avgMonthly));

        System.out.println();
        System.out.println("Number of Transactions: " + all.size());

        System.out.println();
        for (Map.Entry<String, BigDecimal> entry : byCategory.entrySet()) {
            double percentage = grandTotal.signum() == 0 ? 0.0 :
                    entry.getValue().multiply(BigDecimal.valueOf(100))
                            .divide(grandTotal, 2, RoundingMode.HALF_UP).doubleValue();
            System.out.printf("%s accounts for %.2f%% of total spending.%n", entry.getKey(), percentage);
        }
        System.out.println("========================================");
    }
}
