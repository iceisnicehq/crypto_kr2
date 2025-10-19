// Файл: Polynomial.java (ИСПРАВЛЕННАЯ ВЕРСИЯ)
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.math.BigInteger; // Используем для modInverse

public class Polynomial {

    private final long[] coefficients;
    // ВАЖНО: Убрали static P, теперь модуль - это свойство каждого объекта
    private final int modulus;

    // Конструктор теперь требует модуль
    public Polynomial(long[] coeffs, int modulus) {
        this.modulus = modulus;
        int degree = coeffs.length - 1;
        while (degree > 0 && mod(coeffs[degree]) == 0) {
            degree--;
        }
        long[] newCoeffs = new long[degree + 1];
        for (int i = 0; i < newCoeffs.length; i++) {
            newCoeffs[i] = mod(coeffs[i]);
        }
        this.coefficients = newCoeffs;
    }

    // Конструктор копирования
    public Polynomial(Polynomial other) {
        this.coefficients = Arrays.copyOf(other.coefficients, other.coefficients.length);
        this.modulus = other.modulus;
    }

    // Конструктор для создания нулевого многочлена
    public Polynomial(int modulus) {
        this.coefficients = new long[]{0};
        this.modulus = modulus;
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

    // ИЗМЕНЕНИЕ: mod теперь не статический метод
    private long mod(long a) {
        return (a % modulus + modulus) % modulus;
    }
    
    public Polynomial normalize() {
        if (isZero()) return this;
        long leadCoeff = getLeadingCoefficient();
        if (leadCoeff == 1) return this;

        long inv = modInverse(leadCoeff, this.modulus);
        long[] newCoeffs = new long[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            newCoeffs[i] = mod(coefficients[i] * inv);
        }
        return new Polynomial(newCoeffs, this.modulus);
    }

    public long getLeadingCoefficient() {
        if (isZero()) return 0;
        return coefficients[degree()];
    }

    public Polynomial derivative() {
        if (degree() == 0) return new Polynomial(this.modulus);
        long[] newCoeffs = new long[degree()];
        for (int i = 1; i <= degree(); i++) {
            newCoeffs[i - 1] = mod(coefficients[i] * i);
        }
        return new Polynomial(newCoeffs, this.modulus);
    }

    public Polynomial pthRoot() {
        if (this.isZero()) return new Polynomial(this.modulus);
        long[] newCoeffs = new long[degree() / this.modulus + 1];
        for (int i = 0; i <= degree(); i++) {
            if (i % this.modulus == 0) {
                newCoeffs[i / this.modulus] = coefficients[i];
            } else if (coefficients[i] != 0) {
                // Это исключение может быть слишком строгим, лучше просто вернуть null или специальный флаг
                // Для простоты оставим как есть, но в реальной системе это нужно обработать
                throw new IllegalArgumentException("Polynomial is not a p-th power.");
            }
        }
        return new Polynomial(newCoeffs, this.modulus);
    }

    public static Polynomial gcd(Polynomial a, Polynomial b, StringBuilder tracer) {
        if (tracer != null) {
            tracer.append("\n   [Вычисление НОД]\n");
            tracer.append(String.format("   НОД(%s, %s)\n", a, b));
        }

        while (!b.isZero()) {
            Polynomial r = divide(a, b, tracer)[1];
            a = b;
            b = r;
        }
        return a;
    }
    
    public static Polynomial[] divide(Polynomial a, Polynomial b, StringBuilder tracer) {
        if (b.isZero()) throw new ArithmeticException("Division by zero.");

        Polynomial quotient = new Polynomial(a.modulus);
        Polynomial remainder = new Polynomial(a);

        if (remainder.degree() < b.degree()) {
            return new Polynomial[]{quotient, remainder};
        }

        long b_lead_inv = modInverse(b.getLeadingCoefficient(), b.modulus);
        while (!remainder.isZero() && remainder.degree() >= b.degree()) {
            int deg_diff = remainder.degree() - b.degree();
            long factor = remainder.mod(remainder.getLeadingCoefficient() * b_lead_inv);
            
            long[] term_coeffs = new long[deg_diff + 1];
            term_coeffs[deg_diff] = factor;
            Polynomial term = new Polynomial(term_coeffs, a.modulus);
            
            quotient = quotient.add(term);
            Polynomial toSubtract = term.multiply(b);
            remainder = remainder.subtract(toSubtract);
        }

        return new Polynomial[]{quotient, remainder};
    }

    // ИЗМЕНЕНИЕ: modInverse теперь статический и принимает модуль
    private static long modInverse(long n, int modulus) {
        return BigInteger.valueOf(n).modInverse(BigInteger.valueOf(modulus)).longValue();
    }
    
    public Polynomial add(Polynomial other) {
        int newDegree = Math.max(this.degree(), other.degree());
        long[] newCoeffs = new long[newDegree + 1];
        System.arraycopy(this.coefficients, 0, newCoeffs, 0, this.coefficients.length);
        for (int i = 0; i <= other.degree(); i++) {
            newCoeffs[i] = mod(newCoeffs[i] + other.coefficients[i]);
        }
        return new Polynomial(newCoeffs, this.modulus);
    }

    public Polynomial subtract(Polynomial other) {
         int newDegree = Math.max(this.degree(), other.degree());
        long[] newCoeffs = new long[newDegree + 1];
        System.arraycopy(this.coefficients, 0, newCoeffs, 0, this.coefficients.length);
        for (int i = 0; i <= other.degree(); i++) {
            newCoeffs[i] = mod(newCoeffs[i] - other.coefficients[i]);
        }
        return new Polynomial(newCoeffs, this.modulus);
    }

    public Polynomial multiply(Polynomial other) {
        if (this.isZero() || other.isZero()) return new Polynomial(this.modulus);
        int newDegree = this.degree() + other.degree();
        long[] newCoeffs = new long[newDegree + 1];
        for (int i = 0; i <= this.degree(); i++) {
            for (int j = 0; j <= other.degree(); j++) {
                newCoeffs[i + j] = mod(newCoeffs[i + j] + this.coefficients[i] * other.coefficients[j]);
            }
        }
        return new Polynomial(newCoeffs, this.modulus);
    }
    
    // ИЗМЕНЕНИЕ: fromString теперь статический и принимает модуль
    public static Polynomial fromString(String s, int modulus) {
        s = s.replace("-", "+ -").replaceAll("\\s+", "");
        if (s.startsWith("+")) s = s.substring(1);

        Map<Integer, Long> coeffMap = new TreeMap<>(Collections.reverseOrder());
        if (s.isEmpty() || s.equals("0")) {
            return new Polynomial(new long[]{0}, modulus);
        }
        
        String[] terms = s.split("\\+");
        for (String term : terms) {
            if (term.isEmpty()) continue;
            long coeff = 1;
            int degree = 0;
            if (term.contains("x")) {
                String[] parts = term.split("x", -1);
                String coeffPart = parts[0];
                String degreePart = (parts.length > 1) ? parts[1].replace("^", "") : "";
                if (coeffPart.isEmpty() || coeffPart.equals("+")) coeff = 1;
                else if (coeffPart.equals("-")) coeff = -1;
                else coeff = Long.parseLong(coeffPart);
                if (degreePart.isEmpty()) degree = 1;
                else degree = Integer.parseInt(degreePart);
            } else {
                coeff = Long.parseLong(term);
                degree = 0;
            }
            long currentCoeff = coeffMap.getOrDefault(degree, 0L);
            coeffMap.put(degree, currentCoeff + coeff);
        }
        
        int maxDegree = coeffMap.keySet().iterator().next();
        long[] coeffs = new long[maxDegree + 1];
        for (Map.Entry<Integer, Long> entry : coeffMap.entrySet()) {
            coeffs[entry.getKey()] = entry.getValue();
        }
        return new Polynomial(coeffs, modulus);
    }

    @Override
    public String toString() {
        if (isZero()) return "0";
        StringBuilder sb = new StringBuilder();
        for (int i = degree(); i >= 0; i--) {
            long coeff = coefficients[i];
            if (coeff == 0) continue;
            
            String sign = (coeff > 0) ? " + " : " - ";
            long absCoeff = Math.abs(coeff);
            
            if (sb.length() > 0) {
                sb.append(sign);
            } else if (coeff < 0) {
                 sb.append("-");
            }
            
            if (absCoeff != 1 || i == 0) {
                sb.append(absCoeff);
            }
            
            if (i > 0) {
                sb.append("x");
                if (i > 1) sb.append("^").append(i);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Polynomial that = (Polynomial) o;
        return modulus == that.modulus && Arrays.equals(coefficients, that.coefficients);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(coefficients);
        result = 31 * result + modulus;
        return result;
    }
}