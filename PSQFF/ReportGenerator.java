import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Collections;
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
        
        report.append("\n## Промежуточный результат (после PSQFF)\n\n");
        String intermediateAnswer = formatFinalAnswer(ps_qffFactors);
        report.append("p(x) = ").append(p.toString()).append(" = ").append(intermediateAnswer).append("\n");

        report.append("\n## Формирование ответа: Разложение до неприводимых множителей\n\n");
        Map<Polynomial, Integer> finalFactors = new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());
        
        for (Map.Entry<Polynomial, Integer> entry : ps_qffFactors.entrySet()) {
            Polynomial polyToFactor = entry.getKey();
            int exponent = entry.getValue();
            
            report.append("Обрабатываем множитель: ").append(polyToFactor).append("\n");
            
            List<Polynomial> irreducibleFactors = factorByTrialDivision(polyToFactor);
            for (Polynomial irreducible : irreducibleFactors) {
                finalFactors.merge(irreducible, exponent, Integer::sum);
            }
        }

        report.append("\n## Итоговое разложение\n\n");
        String finalAnswer = formatFinalAnswer(finalFactors);
        report.append("p(x) = ").append(p.toString()).append(" = ").append(finalAnswer).append("\n\n");
        report.append("**Ответ:** ").append(p.toString()).append(" = ").append(finalAnswer);
        
        return report.toString();
    }

    private Map<Polynomial, Integer> generateRecursiveSteps(Polynomial p) {
        if (p.isConstant() || p.degree() < 1) return new TreeMap<>();
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
    
    private List<Polynomial> factorByTrialDivision(Polynomial p) {
        List<Polynomial> factors = new ArrayList<>();
        Polynomial remainder = new Polynomial(p);
        if (remainder.degree() < 1) return factors;
        
        long constantFactor = remainder.normalize();
        if (constantFactor != 1) {
            factors.add(new Polynomial(new long[]{constantFactor}));
        }

        for (long i = 0; i < characteristic; i++) {
            Polynomial linearFactor = new Polynomial(new long[]{ (i == 0 ? 0 : characteristic - i), 1});
            while (remainder.degree() > 0 && Polynomial.divide(remainder, linearFactor, null)[1].isZero()) {
                factors.add(linearFactor);
                remainder = Polynomial.divide(remainder, linearFactor, null)[0];
                report.append("   - Найден линейный множитель: ").append(linearFactor).append(", текущий остаток: ").append(remainder).append("\n");
            }
        }

        if (remainder.degree() > 0) {
            factors.add(remainder);
        }

        if (factors.isEmpty()) {
            report.append("   - Множитель ").append(p).append(" не имеет линейных корней.\n");
            factors.add(p);
        }
        return factors;
    }

    private String formatFinalAnswer(Map<Polynomial, Integer> factors) {
        if (factors.isEmpty()) return "1";
        
        List<Map.Entry<Polynomial, Integer>> sortedFactors = new ArrayList<>(factors.entrySet());
        Collections.sort(sortedFactors, (a, b) -> b.getKey().degree() - a.getKey().degree());

        StringBuilder result = new StringBuilder();
        
        for (Map.Entry<Polynomial, Integer> entry : sortedFactors) {
             if (result.length() > 0) result.append(" * ");
            Polynomial currentPoly = entry.getKey();
            int exp = entry.getValue();

            if (currentPoly.isConstant()) {
                result.append(currentPoly.getConstantValue());
            } else {
                String base = "(" + currentPoly.toString() + ")";
                result.append(exp > 1 ? base + "^" + exp : base);
            }
        }
        return result.toString();
    }
    
    private static long power(long base, long exp) {
        long res = 1;
        base %= Polynomial.P;
        while (exp > 0) {
            if (exp % 2 == 1) res = (res * base) % Polynomial.P;
            base = (base * base) % Polynomial.P;
            exp /= 2;
        }
        return res;
    }
}