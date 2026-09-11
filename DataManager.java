import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles all file I/O for persisting and restoring expenses and categories.
 * Uses plain CSV / text files so the application runs anywhere without
 * requiring a database. Corrupted or unreadable lines are skipped with a
 * warning instead of crashing the application.
 */
public class DataManager {

    private final String expensesFilePath;
    private final String categoriesFilePath;

    public DataManager(String expensesFilePath, String categoriesFilePath) {
        this.expensesFilePath = expensesFilePath;
        this.categoriesFilePath = categoriesFilePath;
    }

    /**
     * Loads categories from disk into the given manager. If the file does
     * not exist yet, default categories are loaded and the file is created.
     */
    public void loadCategories(CategoryManager categoryManager) {
        File file = new File(categoriesFilePath);
        if (!file.exists()) {
            categoryManager.loadDefaults();
            saveCategories(categoryManager);
            return;
        }

        boolean anyLoaded = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                categoryManager.addCategorySilently(trimmed);
                anyLoaded = true;
            }
        } catch (IOException e) {
            System.out.println("Warning: could not read categories file (" + e.getMessage()
                    + "). Loading default categories instead.");
        }

        if (!anyLoaded) {
            categoryManager.loadDefaults();
            saveCategories(categoryManager);
        }
    }

    public void saveCategories(CategoryManager categoryManager) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(categoriesFilePath))) {
            for (Category c : categoryManager.getAllCategories()) {
                writer.write(c.getName());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Warning: could not save categories to disk: " + e.getMessage());
        }
    }

    /**
     * Loads expenses from disk into the given manager. Malformed lines are
     * skipped individually with a warning so one bad row does not prevent
     * the rest of the data from loading.
     */
    public void loadExpenses(ExpenseManager expenseManager) {
        File file = new File(expensesFilePath);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty()) continue;
                try {
                    List<String> fields = parseCsvLine(line);
                    if (fields.size() != 5) {
                        throw new IllegalArgumentException("expected 5 fields, found " + fields.size());
                    }
                    int id = Integer.parseInt(fields.get(0).trim());
                    LocalDate date = LocalDate.parse(fields.get(1).trim());
                    BigDecimal amount = new BigDecimal(fields.get(2).trim());
                    String category = fields.get(3);
                    String description = fields.get(4);
                    expenseManager.restoreExpense(id, date, amount, category, description);
                } catch (Exception ex) {
                    System.out.println("Warning: skipping corrupted expense data at line "
                            + lineNumber + " (" + ex.getMessage() + ").");
                }
            }
        } catch (IOException e) {
            System.out.println("Warning: could not read expenses file: " + e.getMessage());
        }
    }

    public void saveExpenses(ExpenseManager expenseManager) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(expensesFilePath))) {
            for (Expense e : expenseManager.getAllExpensesRaw()) {
                writer.write(e.toCsvLine());
                writer.newLine();
            }
        } catch (IOException e) {
            System.out.println("Warning: could not save expenses to disk: " + e.getMessage());
        }
    }

    /**
     * Minimal CSV parser that understands double-quoted fields with escaped
     * quotes ("") inside them, which is how Expense#toCsvLine writes
     * category and description fields.
     */
    private List<String> parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;
        int len = line.length();

        while (i < len) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < len && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i += 2;
                        continue;
                    } else {
                        inQuotes = false;
                        i++;
                        continue;
                    }
                } else {
                    current.append(c);
                    i++;
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    i++;
                } else if (c == ',') {
                    fields.add(current.toString());
                    current.setLength(0);
                    i++;
                } else {
                    current.append(c);
                    i++;
                }
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
