import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

public class ReportGenerator {

    private final StringBuilder report = new StringBuilder();
    private int stepCounter = 1;
    private final int characteristic;

    public ReportGenerator(int characteristic) {
        this.characteristic = characteristic;
        Polynomial.P = characteristic;
    }

    public String generateReport(Polynomial p) {
        report.append("## Основная часть: Алгоритм PSQFF\n\n");
        Map<Polynomial, Integer> ps_qffFactors = generateRecursiveSteps(p);
        
        report.append("\n## Формирование ответа: Разложение бесквадратных множителей на неприводимые\n\n");
        Map<Polynomial, Integer> finalFactors = new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());

        for (Map.Entry<Polynomial, Integer> entry : ps_qffFactors.entrySet()) {
            Polynomial polyToFactor = entry.getKey();
            int exponent = entry.getValue();
            
            if (polyToFactor.degree() > 1) {
                report.append("Разлагаем множитель: ").append(polyToFactor).append("\n");
                List<Polynomial> irreducibleFactors = distinctDegreeFactorization(polyToFactor);
                for (Polynomial irreducible : irreducibleFactors) {
                    finalFactors.merge(irreducible, exponent, Integer::sum);
                }
            } else {
                finalFactors.merge(polyToFactor, exponent, Integer::sum);
            }
        }

        report.append("\n## Итоговое разложение\n\n");
        String finalAnswer = formatFinalAnswer(finalFactors);
        report.append("p(x) = ").append(p.toString()).append(" = ").append(finalAnswer).append("\n\n");
        report.append("**Ответ:** ").append(p.toString()).append(" = ").append(finalAnswer);
        
        return report.toString();
    }

    private Map<Polynomial, Integer> generateRecursiveSteps(Polynomial p) {
        if (p.isConstant() || p.degree() < 1) {
            return new TreeMap<>();
        }
        report.append("--- Шаг ").append(stepCounter++).append(". Обработка p(x) = ").append(p).append(" ---\n");
        Polynomial derivative = p.derivative();
        report.append("g(x) = p'(x) = ").append(derivative).append("\n");

        if (derivative.isZero()) {
            Polynomial pthRoot = p.pthRoot();
            report.append("g(x) = 0 => p(x) = (").append(pthRoot).append(")^").append(characteristic).append("\n\n");
            Map<Polynomial, Integer> rootFactors = generateRecursiveSteps(pthRoot);
            Map<Polynomial, Integer> finalFactors = new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());
            for (Map.Entry<Polynomial, Integer> entry : rootFactors.entrySet()) {
                finalFactors.put(entry.getKey(), entry.getValue() * characteristic);
            }
            return finalFactors;
        } else {
            Polynomial d = Polynomial.gcd(p, derivative);
            report.append("d(x) = gcd(p, g) = ").append(d).append("\n");
            if (d.isConstant()) {
                report.append("d(x) = 1 => многочлен свободен от квадратов.\n\n");
                Map<Polynomial, Integer> result = new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());
                result.put(p, 1);
                return result;
            } else {
                Polynomial h = Polynomial.divide(p, d, null)[0];
                report.append("p(x) = d(x) * h(x) = (").append(d).append(") * (").append(h).append(")\n\n");
                Map<Polynomial, Integer> hFactors = generateRecursiveSteps(h);
                Map<Polynomial, Integer> dFactors = generateRecursiveSteps(d);
                Map<Polynomial, Integer> combinedFactors = new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());
                combinedFactors.putAll(hFactors);
                dFactors.forEach((poly, exp) -> combinedFactors.merge(poly, exp, Integer::sum));
                return combinedFactors;
            }
        }
    }
    
    private List<Polynomial> distinctDegreeFactorization(Polynomial p) {
        List<Polynomial> factors = new ArrayList<>();
        Polynomial f = new Polynomial(p);
        Polynomial x = new Polynomial(new long[]{0, 1});
        int d = 1;
        while (f.degree() >= 2 * d) {
            report.append("   - Ищем множители степени d = ").append(d).append("\n");
            Polynomial x_pow_p_d = x.pow(power(characteristic, d), f);
            Polynomial g = Polynomial.gcd(x_pow_p_d.subtract(x), f);
            if (!g.isConstant()) {
                report.append("     Найден(ы) множитель(и) степени ").append(d).append(": ").append(g).append("\n");
                factors.add(g);
                f = Polynomial.divide(f, g, null)[0];
            }
            d++;
        }
        if (f.degree() > 0) {
            factors.add(f);
        }
        return factors;
    }

    private String formatFinalAnswer(Map<Polynomial, Integer> factors) {
        if (factors.isEmpty()) return "1";
        return factors.entrySet().stream()
                .map(entry -> {
                    String base = "(" + entry.getKey().toString() + ")";
                    int exp = entry.getValue();
                    return exp > 1 ? base + "^" + exp : base;
                })
                .collect(Collectors.joining(" * "));
    }
    
    private static long power(long base, long exp) {
        long res = 1;
        for (int i = 0; i < exp; i++) {
            res *= base;
        }
        return res;
    }
}