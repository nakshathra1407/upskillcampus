import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for consistent monetary formatting across the application.
 */
public final class MoneyUtil {

    private MoneyUtil() {
        // utility class, no instances
    }

    /**
     * Formats a BigDecimal amount as an Indian Rupee value with thousands
     * separators and exactly two decimal places, e.g. 12450.5 -> "₹12,450.50".
     */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        BigDecimal scaled = amount.setScale(2, RoundingMode.HALF_UP);
        boolean negative = scaled.signum() < 0;
        BigDecimal absValue = scaled.abs();

        String plain = absValue.toPlainString();
        String[] parts = plain.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts.length > 1 ? parts[1] : "00";

        StringBuilder grouped = new StringBuilder();
        int len = integerPart.length();
        if (len <= 3) {
            grouped.append(integerPart);
        } else {
            // Indian numbering: last 3 digits, then groups of 2
            String lastThree = integerPart.substring(len - 3);
            String remaining = integerPart.substring(0, len - 3);
            StringBuilder remGrouped = new StringBuilder();
            int remLen = remaining.length();
            int count = 0;
            for (int i = remLen - 1; i >= 0; i--) {
                remGrouped.insert(0, remaining.charAt(i));
                count++;
                if (count % 2 == 0 && i != 0) {
                    remGrouped.insert(0, ',');
                }
            }
            grouped.append(remGrouped).append(',').append(lastThree);
        }

        String sign = negative ? "-" : "";
        return sign + "\u20B9" + grouped.toString() + "." + decimalPart;
    }
}
