import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class Polynomial {

    private final long[] coefficients;
    public static int P;

    public Polynomial(long[] coeffs) {
        int degree = coeffs.length - 1;
        while (degree > 0 && coeffs[degree] == 0) {
            degree--;
        }
        this.coefficients = Arrays.copyOf(coeffs, degree + 1);
    }


    public Polynomial(Polynomial other) {
        this.coefficients = Arrays.copyOf(other.coefficients, other.coefficients.length);
    }

    public Polynomial() {
        this.coefficients = new long[]{0};
    }

    public int degree() {
        return coefficients.length - 1;
    }

    public boolean isZero() {
        return degree() == 0 && coefficients[0] == 0;
    }

    public boolean isConstant() {
        return degree() == 0;
    }

    public long getConstantValue() {
        return coefficients[0];
    }

    private static long mod(long a) {
        return (a % P + P) % P;
    }

    public Polynomial derivative() {
        if (degree() == 0) {
            return new Polynomial();
        }
        long[] newCoeffs = new long[degree()];
        for (int i = 1; i <= degree(); i++) {
            newCoeffs[i - 1] = mod(coefficients[i] * i);
        }
        return new Polynomial(newCoeffs);
    }

    public Polynomial pthRoot() {
        if (this.isZero()) return new Polynomial();
        long[] newCoeffs = new long[degree() / P + 1];
        for (int i = 0; i <= degree(); i++) {
            if (i % P == 0) {
                newCoeffs[i / P] = coefficients[i];
            } else if (coefficients[i] != 0) {
                throw new IllegalArgumentException("Polynomial is not a p-th power.");
            }
        }
        return new Polynomial(newCoeffs);
    }

    public static Polynomial gcd(Polynomial a, Polynomial b) {
        return gcd(a, b, null);
    }

    public static Polynomial[] divide(Polynomial a, Polynomial b) {
        return divide(a, b, null);
    }

    public static Polynomial gcd(Polynomial a, Polynomial b, StringBuilder tracer) {
        while (!b.isZero()) {
            Polynomial remainder = divide(a, b, tracer)[1];
            a = b;
            b = remainder;
        }
        if (!a.isZero()) {
            long leadCoeff = a.coefficients[a.degree()];
            if (leadCoeff != 0) {
                long inv = modInverse(leadCoeff);
                long[] newCoeffs = new long[a.coefficients.length];
                for (int i = 0; i < newCoeffs.length; i++) {
                    newCoeffs[i] = mod(a.coefficients[i] * inv);
                }
                return new Polynomial(newCoeffs);
            }
        }
        return a;
    }

    public static Polynomial[] divide(Polynomial a, Polynomial b, StringBuilder tracer) {
        if (b.isZero()) {
            throw new ArithmeticException("Division by zero.");
        }
        Polynomial quotient = new Polynomial();
        Polynomial remainder = new Polynomial(a.coefficients);
        if (remainder.degree() < b.degree()) {
            return new Polynomial[]{quotient, remainder};
        }
        long b_lead_inv = modInverse(b.coefficients[b.degree()]);
        while (!remainder.isZero() && remainder.degree() >= b.degree()) {
            int deg_diff = remainder.degree() - b.degree();
            long factor = mod(remainder.coefficients[remainder.degree()] * b_lead_inv);
            long[] term_coeffs = new long[deg_diff + 1];
            term_coeffs[deg_diff] = factor;
            Polynomial term = new Polynomial(term_coeffs);
            quotient = quotient.add(term);
            Polynomial toSubtract = term.multiply(b);
            remainder = remainder.subtract(toSubtract);
        }
        return new Polynomial[]{quotient, remainder};
    }

    private static long modInverse(long n) {
        return power(n, P - 2);
    }

    private static long power(long base, long exp) {
        long res = 1;
        base %= P;
        while (exp > 0) {
            if (exp % 2 == 1) res = mod(res * base);
            base = mod(base * base);
            exp /= 2;
        }
        return res;
    }

    public Polynomial add(Polynomial other) {
        int newDegree = Math.max(this.degree(), other.degree());
        long[] newCoeffs = new long[newDegree + 1];
        for (int i = 0; i <= this.degree(); i++) {
            newCoeffs[i] = mod(newCoeffs[i] + this.coefficients[i]);
        }
        for (int i = 0; i <= other.degree(); i++) {
            newCoeffs[i] = mod(newCoeffs[i] + other.coefficients[i]);
        }
        return new Polynomial(newCoeffs);
    }

    public Polynomial subtract(Polynomial other) {
        int newDegree = Math.max(this.degree(), other.degree());
        long[] newCoeffs = new long[newDegree + 1];
        for (int i = 0; i <= this.degree(); i++) {
            newCoeffs[i] = mod(newCoeffs[i] + this.coefficients[i]);
        }
        for (int i = 0; i <= other.degree(); i++) {
            newCoeffs[i] = mod(newCoeffs[i] - other.coefficients[i]);
        }
        return new Polynomial(newCoeffs);
    }

    public Polynomial multiply(Polynomial other) {
        if (this.isZero() || other.isZero()) return new Polynomial();
        int newDegree = this.degree() + other.degree();
        long[] newCoeffs = new long[newDegree + 1];
        for (int i = 0; i <= this.degree(); i++) {
            for (int j = 0; j <= other.degree(); j++) {
                newCoeffs[i + j] = mod(newCoeffs[i + j] + this.coefficients[i] * other.coefficients[j]);
            }
        }
        return new Polynomial(newCoeffs);
    }

    public Polynomial pow(long exp, Polynomial modulus) {
        Polynomial res = new Polynomial(new long[]{1});
        Polynomial base = new Polynomial(this);
        while (exp > 0) {
            if (exp % 2 == 1) {
                res = res.multiply(base);
                if (modulus != null) res = divide(res, modulus, null)[1];
            }
            base = base.multiply(base);
            if (modulus != null) base = divide(base, modulus, null)[1];
            exp /= 2;
        }
        return res;
    }

    public static Polynomial fromString(String s) {
        s = s.replace("-", "+ -").replaceAll("\\s+", "");
        String[] terms = s.split("\\+");
        Map<Integer, Long> coeffMap = new TreeMap<>(Collections.reverseOrder());
        for (String term : terms) {
            if (term.isEmpty()) continue;
            long coeff = 1; int degree = 0;
            if (term.contains("x")) {
                String[] parts = term.split("x", -1);
                String coeffPart = parts[0];
                String degreePart = (parts.length > 1) ? parts[1] : "";
                if (coeffPart.isEmpty() || coeffPart.equals("+")) coeff = 1;
                else if (coeffPart.equals("-")) coeff = -1;
                else coeff = Long.parseLong(coeffPart);
                if (degreePart.isEmpty()) degree = 1;
                else degree = Integer.parseInt(degreePart.substring(1));
            } else {
                coeff = Long.parseLong(term); degree = 0;
            }
            coeffMap.put(degree, mod(coeffMap.getOrDefault(degree, 0L) + coeff));
        }
        if (coeffMap.isEmpty()) return new Polynomial();
        int maxDegree = coeffMap.keySet().iterator().next();
        long[] coeffs = new long[maxDegree + 1];
        for (Map.Entry<Integer, Long> entry : coeffMap.entrySet()) {
            coeffs[entry.getKey()] = entry.getValue();
        }
        return new Polynomial(coeffs);
    }

    @Override
    public String toString() {
        if (isZero()) return "0";
        StringBuilder sb = new StringBuilder();
        for (int i = degree(); i >= 0; i--) {
            long coeff = coefficients[i];
            if (coeff == 0) continue;
            if (sb.length() > 0) {
                sb.append(coeff > 0 ? " + " : " - ");
                coeff = Math.abs(coeff);
            } else if (coeff < 0) {
                sb.append("-");
                coeff = Math.abs(coeff);
            }
            if (coeff != 1 || i == 0) sb.append(coeff);
            if (i > 0) {
                sb.append("x");
                if (i > 1) sb.append("^").append(i);
            }
        }
        return sb.toString();
    }
}