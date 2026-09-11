import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the full lifecycle of expense records: creation, update, deletion,
 * searching, filtering and aggregate calculations. Unique IDs are generated
 * sequentially and never reused, even after deletions or an application restart.
 */
public class ExpenseManager {

    private static final int STARTING_ID = 1001;

    private final List<Expense> expenses = new ArrayList<>();
    private int nextId = STARTING_ID;

    /**
     * Adds a brand new expense, generating the next available unique ID.
     */
    public Expense addExpense(LocalDate date, BigDecimal amount, String category, String description) {
        Expense expense = new Expense(nextId, date, amount, category, description);
        expenses.add(expense);
        nextId++;
        return expense;
    }

    /**
     * Restores an expense with an explicit ID, used only while loading
     * previously persisted data. Also advances the ID counter so future
     * new expenses never collide with restored ones.
     */
    public void restoreExpense(int id, LocalDate date, BigDecimal amount, String category, String description) {
        expenses.add(new Expense(id, date, amount, category, description));
        if (id >= nextId) {
            nextId = id + 1;
        }
    }

    public Expense findById(int id) {
        for (Expense e : expenses) {
            if (e.getId() == id) {
                return e;
            }
        }
        return null;
    }

    public boolean deleteExpense(int id) {
        return expenses.removeIf(e -> e.getId() == id);
    }

    public int countByCategory(String category) {
        int count = 0;
        for (Expense e : expenses) {
            if (e.getCategory().equalsIgnoreCase(category)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Reassigns every expense currently under oldCategory to newCategory.
     * Used to support safe category renaming/reassignment.
     */
    public void reassignCategory(String oldCategory, String newCategory) {
        for (Expense e : expenses) {
            if (e.getCategory().equalsIgnoreCase(oldCategory)) {
                e.setCategory(newCategory);
            }
        }
    }

    public boolean isEmpty() {
        return expenses.isEmpty();
    }

    public int size() {
        return expenses.size();
    }

    /**
     * Returns all expenses sorted by date descending (newest first);
     * ties are broken by ID ascending.
     */
    public List<Expense> getAllExpensesSorted() {
        List<Expense> copy = new ArrayList<>(expenses);
        sortDefault(copy);
        return copy;
    }

    private void sortDefault(List<Expense> list) {
        list.sort(Comparator.comparing(Expense::getDate).reversed()
                .thenComparing(Expense::getId));
    }

    // ---------- Filtering (never mutates the underlying list) ----------

    public List<Expense> filterByDateRange(LocalDate start, LocalDate end) {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (!e.getDate().isBefore(start) && !e.getDate().isAfter(end)) {
                result.add(e);
            }
        }
        sortDefault(result);
        return result;
    }

    public List<Expense> filterByCategory(String category) {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.getCategory().equalsIgnoreCase(category)) {
                result.add(e);
            }
        }
        sortDefault(result);
        return result;
    }

    public List<Expense> filterByAmountRange(BigDecimal min, BigDecimal max) {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.getAmount().compareTo(min) >= 0 && e.getAmount().compareTo(max) <= 0) {
                result.add(e);
            }
        }
        sortDefault(result);
        return result;
    }

    /**
     * Combined filter where any of the criteria may be null (meaning
     * "no restriction on this field").
     */
    public List<Expense> combinedFilter(LocalDate start, LocalDate end, String category,
                                         BigDecimal min, BigDecimal max) {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (start != null && e.getDate().isBefore(start)) continue;
            if (end != null && e.getDate().isAfter(end)) continue;
            if (category != null && !e.getCategory().equalsIgnoreCase(category)) continue;
            if (min != null && e.getAmount().compareTo(min) < 0) continue;
            if (max != null && e.getAmount().compareTo(max) > 0) continue;
            result.add(e);
        }
        sortDefault(result);
        return result;
    }

    // ---------- Aggregate calculations ----------

    public BigDecimal getTotalAmount(List<Expense> list) {
        BigDecimal total = BigDecimal.ZERO;
        for (Expense e : list) {
            total = total.add(e.getAmount());
        }
        return total;
    }

    public BigDecimal getAverageAmount(List<Expense> list) {
        if (list.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = getTotalAmount(list);
        return total.divide(BigDecimal.valueOf(list.size()), 2, RoundingMode.HALF_UP);
    }

    public Expense getHighestExpense(List<Expense> list) {
        Expense highest = null;
        for (Expense e : list) {
            if (highest == null || e.getAmount().compareTo(highest.getAmount()) > 0) {
                highest = e;
            }
        }
        return highest;
    }

    public Expense getLowestExpense(List<Expense> list) {
        Expense lowest = null;
        for (Expense e : list) {
            if (lowest == null || e.getAmount().compareTo(lowest.getAmount()) < 0) {
                lowest = e;
            }
        }
        return lowest;
    }

    /**
     * Returns totals grouped by category, preserving first-seen order,
     * computed from the full expense list.
     */
    public Map<String, BigDecimal> getTotalsByCategory() {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Expense e : expenses) {
            totals.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        return totals;
    }

    public Map<String, BigDecimal> getTotalsByCategory(List<Expense> list) {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (Expense e : list) {
            totals.merge(e.getCategory(), e.getAmount(), BigDecimal::add);
        }
        return totals;
    }

    public List<Expense> getExpensesForMonth(int year, int month) {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.getDate().getYear() == year && e.getDate().getMonthValue() == month) {
                result.add(e);
            }
        }
        sortDefault(result);
        return result;
    }

    public List<Expense> getExpensesForYear(int year) {
        List<Expense> result = new ArrayList<>();
        for (Expense e : expenses) {
            if (e.getDate().getYear() == year) {
                result.add(e);
            }
        }
        sortDefault(result);
        return result;
    }

    public List<Expense> getAllExpensesRaw() {
        return expenses;
    }
}
