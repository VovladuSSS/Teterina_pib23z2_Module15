package academy.tochkavhoda.competition.service;

/**
 * Central place for input validation rules.
 */
public final class InputChecker {
    public static final int LOGIN_MIN_CHARS = 3;
    public static final int SECRET_MIN_CHARS = 6;
    public static final int NAME_MIN_CHARS = 2;
    public static final int ORG_MIN_CHARS = 2;
    public static final int TITLE_MIN_CHARS = 3;
    public static final int DESC_MIN_CHARS = 10;

    private InputChecker() {
    }

    public static void requireNonBlank(String text, String label) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(label + " не может быть пустым");
        }
    }

    public static void requireMinLength(String text, int min, String label) {
        requireNonBlank(text, label);
        if (text.trim().length() < min) {
            throw new IllegalArgumentException(label + " должен содержать не менее " + min + " символов");
        }
    }

    public static void checkSecretStrength(String secret) {
        requireMinLength(secret, SECRET_MIN_CHARS, "Пароль");
        boolean containsNonLetter = secret.chars().anyMatch(ch -> !Character.isLetter(ch));
        if (!containsNonLetter) {
            throw new IllegalArgumentException("Пароль должен содержать хотя бы одну цифру или спецсимвол");
        }
    }

    public static void checkLoginFormat(String login) {
        requireMinLength(login, LOGIN_MIN_CHARS, "Логин");
        if (!login.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Логин может содержать только буквы, цифры и знак подчеркивания");
        }
    }

    public static void checkScoreRange(int value) {
        if (value < 1 || value > 5) {
            throw new IllegalArgumentException("Оценка должна быть в диапазоне от 1 до 5");
        }
    }

    public static void checkPositiveSum(long sum) {
        if (sum <= 0) {
            throw new IllegalArgumentException("Сумма гранта должна быть положительной");
        }
    }
}
