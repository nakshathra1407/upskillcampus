import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Centralized validation logic used across the application so that
 * every field is validated consistently and error messages stay uniform.
 */
public final class InputValidator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private InputValidator() {
        // utility class, no instances
    }

    public static LocalDate parseDate(String text) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Date cannot be empty.");
        }
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date. Please use YYYY-MM-DD.");
        }
    }

    public static BigDecimal parseAmount(String text) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Amount cannot be empty.");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid input. Please enter a valid number.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        return amount;
    }

    public static String validateDescription(String text) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Description cannot be empty.");
        }
        return text.trim().replaceAll("\\s+", " ");
    }

    public static String validateCategoryName(String text) throws ValidationException {
        if (text == null || text.trim().isEmpty()) {
            throw new ValidationException("Category name cannot be empty.");
        }
        return text.trim();
    }

    public static void validateDateRange(LocalDate start, LocalDate end) throws ValidationException {
        if (start != null && end != null && end.isBefore(start)) {
            throw new ValidationException("End date cannot be before start date.");
        }
    }

    public static void validateAmountRange(BigDecimal min, BigDecimal max) throws ValidationException {
        if (min != null && max != null && max.compareTo(min) < 0) {
            throw new ValidationException("Maximum amount cannot be less than minimum amount.");
        }
    }

    public static int validateYear(String text) throws ValidationException {
        int year;
        try {
            year = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid year. Please enter a valid number.");
        }
        if (year < 1900 || year > 2200) {
            throw new ValidationException("Invalid year. Please enter a realistic year.");
        }
        return year;
    }

    public static int validateMonth(String text) throws ValidationException {
        int month;
        try {
            month = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid month. Please enter a number between 1 and 12.");
        }
        if (month < 1 || month > 12) {
            throw new ValidationException("Invalid month. Please enter a number between 1 and 12.");
        }
        return month;
    }
}
