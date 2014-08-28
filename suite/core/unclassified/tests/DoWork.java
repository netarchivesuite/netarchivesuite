import java.util.ArrayList;
import java.util.List;


public class DoWork {

    /**
     * @param args
     */
    public static void main(String[] args) {
        while (true) {
            Calculate();
        }

    }

    private static void Calculate() {
        List<Integer> primes = new ArrayList<Integer>();
        int quantity = 2000000;
        int numPrimes = 0;
        // candidate: the number that might be prime
        int candidate = 2;
        while (numPrimes < quantity) {
            if (isPrime(candidate)) {
                primes.add(candidate);
                numPrimes++;
            }
            candidate++;
        }
    }

    public static boolean isPrime(int checkNumber) {
        double root = Math.sqrt(checkNumber);
        for (int i = 2; i <= root; i++) {
            if (checkNumber % i == 0)
                return false;
        }
        return true;
    }

}
