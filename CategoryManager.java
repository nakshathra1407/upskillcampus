import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the list of expense categories: adding, renaming, deleting and
 * validating category names. Category names are treated case-insensitively
 * for uniqueness checks, but the original casing typed by the user is preserved.
 */
public class CategoryManager {

    // Keyed by lower-case name to enforce case-insensitive uniqueness,
    // while the Category object retains the original display casing.
    private final Map<String, Category> categories = new LinkedHashMap<>();

    public static final String[] DEFAULT_CATEGORIES = {
            "Food", "Transportation", "Shopping", "Bills",
            "Entertainment", "Healthcare", "Education", "Travel", "Other"
    };

    /**
     * Loads the default set of categories. Used only when no persisted
     * category file exists yet (i.e. first run of the application).
     */
    public void loadDefaults() {
        categories.clear();
        for (String name : DEFAULT_CATEGORIES) {
            categories.put(name.toLowerCase(), new Category(name));
        }
    }

    public boolean exists(String name) {
        if (name == null) return false;
        return categories.containsKey(name.trim().toLowerCase());
    }

    public void addCategory(String rawName) throws ValidationException {
        String name = InputValidator.validateCategoryName(rawName);
        if (exists(name)) {
            throw new ValidationException("Category '" + name + "' already exists.");
        }
        categories.put(name.toLowerCase(), new Category(name));
    }

    /**
     * Adds a category without validation errors for duplicates - used only
     * when restoring from persisted storage, where the data is trusted.
     */
    public void addCategorySilently(String name) {
        if (name == null || name.trim().isEmpty()) return;
        String trimmed = name.trim();
        if (!categories.containsKey(trimmed.toLowerCase())) {
            categories.put(trimmed.toLowerCase(), new Category(trimmed));
        }
    }

    public void renameCategory(String oldName, String newRawName) throws ValidationException {
        if (!exists(oldName)) {
            throw new ValidationException("Category '" + oldName + "' does not exist.");
        }
        String newName = InputValidator.validateCategoryName(newRawName);
        if (!oldName.trim().equalsIgnoreCase(newName) && exists(newName)) {
            throw new ValidationException("Category '" + newName + "' already exists.");
        }
        Category category = categories.remove(oldName.trim().toLowerCase());
        category.setName(newName);
        categories.put(newName.toLowerCase(), category);
    }

    public void deleteCategory(String name) throws ValidationException {
        if (!exists(name)) {
            throw new ValidationException("Category '" + name + "' does not exist.");
        }
        categories.remove(name.trim().toLowerCase());
    }

    public List<Category> getAllCategories() {
        return new ArrayList<>(categories.values());
    }

    /**
     * Returns the canonical stored name for a category regardless of the
     * casing the user typed, so expenses always reference a consistent value.
     */
    public String getCanonicalName(String name) {
        Category category = categories.get(name.trim().toLowerCase());
        return category != null ? category.getName() : name.trim();
    }

    public boolean isEmpty() {
        return categories.isEmpty();
    }
}
