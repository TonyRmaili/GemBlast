package gemblast;

import java.util.Locale;

/**
 * The player's balance. Pure logic, no graphics.
 *
 * Money is stored as a whole number of CENTS in a long, never as a double.
 * Doubles can't represent most decimals exactly: 0.1 + 0.2 = 0.30000000000000004.
 * After thousands of spins those tiny errors add up, so balances show 999.9999997.
 * Whole cents never have that problem: 10 + 20 = 30, always.
 */
public final class Wallet {

    public static final long START_BALANCE = 1000_00;   // 1000.00 units (the _ is just for readability)

    private long balance = START_BALANCE;

    public long getBalance() {
        return balance;
    }

    public boolean canAfford(long amount) {
        return balance >= amount;
    }

    /** Takes money out (the bet). Refuses instead of going negative. */
    public void debit(long amount) {
        if (!canAfford(amount)) {
            throw new IllegalStateException("Insufficient balance: " + format(balance) + " < " + format(amount));
        }
        balance -= amount;
    }

    /** Puts money in (a win). */
    public void credit(long amount) {
        balance += amount;
    }

    public void reset() {
        balance = START_BALANCE;
    }

    /** 123456 cents -> "1234.56" */
    public static String format(long cents) {
        return String.format(Locale.US, "%d.%02d", cents / 100, cents % 100);
    }
}