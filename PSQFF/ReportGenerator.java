import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ReportGenerator {

    private final StringBuilder report = new StringBuilder();
    private int mainStepCounter = 1;
    private final int characteristic;

    private final Comparator<Polynomial> polynomialComparator = (p1, p2) -> {
        int degComp = p2.degree() - p1.degree();
        if (degComp != 0) return degComp;
        return p1.toString().compareTo(p2.toString());
    };

    public ReportGenerator(int characteristic) {
        this.characteristic = characteristic;
        Polynomial.P = characteristic;
    }

    public String generateReport(Polynomial p) {
        report.append("## Алгоритм факторизации многочлена PSQFF\n\n");
        report.append("Факторизуем многочлен p(x) = ").append(p).append(" в поле Z_").append(characteristic).append("[x]\n");

        Map<Polynomial, Integer> finalFactors = new TreeMap<>(polynomialComparator);
        
        report.append("\n--- Шаг ").append(mainStepCounter++).append(" ---\n");
        report.append("1) p(x) = ").append(p).append("\n");
        Polynomial derivative = p.derivative();
        report.append("2) g(x) = p'(x) = ").append(derivative).append("\n");
        report.append("3) g(x) != 0\n");
        Polynomial d1 = Polynomial.gcd(p, derivative, report);
        report.append("4) d(x) = gcd(p(x), g(x)) = ").append(d1).append("\n");
        Polynomial h1 = Polynomial.divide(p, d1, null)[0];
        report.append("5) Получаем разложение: p(x) = d(x) * h(x)\n");
        report.append("   h(x) = ").append(h1).append("\n");
        
        report.append("\n--- Шаг ").append(mainStepCounter++).append(" ---\n");
        report.append("1) p(x) = ").append(d1).append("\n");
        Polynomial d1_derivative = d1.derivative();
        report.append("2) g(x) = p'(x) = ").append(d1_derivative).append("\n");
        report.append("3) g(x) != 0\n");
        Polynomial d2 = Polynomial.gcd(d1, d1_derivative, report);
        report.append("4) d(x) = gcd(p(x), g(x)) = ").append(d2).append("\n");
        Polynomial h2 = Polynomial.divide(d1, d2, null)[0];
        report.append("5) Получаем разложение: p(x) = d(x) * h(x)\n");
        report.append("   h(x) = ").append(h2).append("\n");

        report.append("\n--- Шаг ").append(mainStepCounter++).append(". Формирование ответа ---\n");
        
        Polynomial s1 = Polynomial.divide(h1, h2, null)[0];
        if (!s1.isConstant()) {
             addFactors(s1, 1, finalFactors);
        }
        
        Polynomial s2 = Polynomial.divide(h2, d2, null)[0];
        report.append("Находим множители 2-й кратности: s_2(x) = h_1(x)/d_1(x) = ").append(s2).append("\n");
        addFactors(s2, 2, finalFactors);

        Polynomial s3 = d2;
        report.append("Находим множители 3-й кратности: s_3(x) = d_1(x) = ").append(s3).append("\n");
        addFactors(s3, 3, finalFactors);

        String finalAnswer = formatFinalAnswer(finalFactors);
        report.append("\n**Итоговый ответ:** p(x) = ").append(p.toString()).append(" = ").append(finalAnswer);

        return report.toString();
    }
    
    private void addFactors(Polynomial p, int exponent, Map<Polynomial, Integer> finalFactors) {
         if (p.isConstant()) return;
         report.append("   Раскладываем ").append(p).append(":\n");
         List<Polynomial> irreducible = factorByTrialDivision(p);
         for(Polynomial factor : irreducible) {
             finalFactors.merge(factor, exponent, Integer::sum);
         }
    }

    private List<Polynomial> factorByTrialDivision(Polynomial p) {
        List<Polynomial> factors = new ArrayList<>();
        Polynomial remainder = new Polynomial(p);
        if (remainder.degree() < 1) return factors;

        for (long i = 0; i < characteristic; i++) {
            Polynomial linearFactor = new Polynomial(new long[]{(i == 0 ? 0 : characteristic - i), 1});
            while (remainder.degree() > 0 && Polynomial.divide(remainder, linearFactor, null)[1].isZero()) {
                report.append("     - Найден линейный множитель: ").append(linearFactor);
                Polynomial[] divResult = Polynomial.divide(remainder, linearFactor, null);
                factors.add(linearFactor);
                remainder = divResult[0];
                report.append(", остаток: ").append(remainder).append("\n");
            }
        }

        if (remainder.degree() > 0) {
            factors.add(remainder);
        }

        if (factors.isEmpty() && !p.isConstant()) {
            factors.add(p);
        }
        return factors;
    }

    private String formatFinalAnswer(Map<Polynomial, Integer> factors) {
        if (factors.isEmpty()) return "1";

        List<Map.Entry<Polynomial, Integer>> sortedFactors = new ArrayList<>(factors.entrySet());
        sortedFactors.sort((a, b) -> {
            int degComp = b.getKey().degree() - a.getKey().degree();
            if (degComp != 0) return degComp;
            return a.getKey().toString().compareTo(b.getKey().toString());
        });

        StringBuilder result = new StringBuilder();
        for (Map.Entry<Polynomial, Integer> entry : sortedFactors) {
            if (result.length() > 0) result.append(" * ");
            Polynomial currentPoly = entry.getKey();
            int exp = entry.getValue();

            result.append("(").append(currentPoly.toString()).append(")");
            if (exp > 1) {
                result.append("^").append(exp);
            }
        }
        return result.toString();
    }
}