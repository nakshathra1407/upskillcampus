/**
 * Thrown when user-supplied data fails validation rules.
 * Kept as a checked exception so every call site is forced to handle
 * validation failures gracefully instead of letting them propagate as crashes.
 */
public class ValidationException extends Exception {

    public ValidationException(String message) {
        super(message);
    }
}
