import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportGenerator {

    private final StringBuilder report = new StringBuilder();
    private int stepCounter = 1;
    private final int characteristic;

    public ReportGenerator(int characteristic) {
        this.characteristic = characteristic;
        Polynomial.P = characteristic;
    }

    public String generateReport(Polynomial p) {
        Map<Polynomial, Integer> finalFactors = generateRecursiveSteps(p);
        
        report.append("## Шаг ").append(stepCounter).append(". Формирование ответа\n\n");
        String finalAnswer = formatFinalAnswer(finalFactors);
        report.append("p(x) = ").append(p.toString()).append(" = ").append(finalAnswer).append("\n\n");
        report.append("**Ответ:** ").append(p.toString()).append(" = ").append(finalAnswer);
        
        return report.toString();
    }

    private Map<Polynomial, Integer> generateRecursiveSteps(Polynomial p) {
        if (p.isConstant()) {
            return new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());
        }

        report.append("## Шаг ").append(stepCounter++).append("\n\n");
        report.append("1) p(x) = ").append(p).append("\n");

        Polynomial derivative = p.derivative();
        report.append("2) g(x) = p'(x) = ").append(derivative).append("\n");

        if (derivative.isZero()) {
            report.append("3) g(x) = 0\n");
            Polynomial pthRoot = p.pthRoot();
            report.append("   Для p(x) имеет место условие v(x^p) = [v(x)]^p, т.е. ")
                  .append(p).append(" = (").append(pthRoot).append(")^")
                  .append(characteristic).append("\n\n---\n\n");
            
            Map<Polynomial, Integer> rootFactors = generateRecursiveSteps(pthRoot);
            Map<Polynomial, Integer> finalFactors = new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());
            for (Map.Entry<Polynomial, Integer> entry : rootFactors.entrySet()) {
                finalFactors.put(entry.getKey(), entry.getValue() * characteristic);
            }
            return finalFactors;

        } else {
            report.append("3) g(x) ≠ 0\n");

            report.append("4) Вычислить d(x) = gcd(p(x), g(x)):\n\n");
            StringBuilder gcdTracer = new StringBuilder();
            Polynomial d = Polynomial.gcd(p, derivative, gcdTracer);

            
            for (String line : gcdTracer.toString().split("\n")) {
                report.append("   ").append(line).append("\n");
            }
            report.append("\n   **Результат НОД:** d(x) = ").append(d).append("\n");
            

            if (d.isConstant()) {
                report.append("5) Получить разложение: d(x) = 1, т.е. многочлен свободен от квадратов в Z")
                      .append(characteristic).append("[x].\n\n---\n\n");
                Map<Polynomial, Integer> result = new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());
                result.put(p, 1);
                return result;
            } else {
                Polynomial h = Polynomial.divide(p, d)[0];
                report.append("\n5) Получить разложение p(x) = d(x) · h(x) = (").append(d)
                      .append(") · (").append(h).append(")\n\n---\n\n");

                Map<Polynomial, Integer> hFactors = generateRecursiveSteps(h);
                Map<Polynomial, Integer> dFactors = generateRecursiveSteps(d);
                
                Map<Polynomial, Integer> combinedFactors = new TreeMap<>((p1, p2) -> p2.degree() - p1.degree());
                combinedFactors.putAll(hFactors);
                
                for (Map.Entry<Polynomial, Integer> entry : dFactors.entrySet()) {
                    Polynomial key = entry.getKey();
                    int currentExp = combinedFactors.getOrDefault(key, 0);
                    combinedFactors.put(key, currentExp + entry.getValue());
                }
                
                 for(Polynomial key : hFactors.keySet()){
                    combinedFactors.remove(key);
                }

                for(Map.Entry<Polynomial,Integer> entry : dFactors.entrySet()){
                     combinedFactors.merge(entry.getKey(), 1, Integer::sum);
                }

                combinedFactors.putAll(hFactors);
                 for (Polynomial poly : hFactors.keySet()) {
                    if (poly.isConstant()) {
                        combinedFactors.remove(poly);
                    }
                }

                return combinedFactors;
            }
        }
    }
    
    private String formatFinalAnswer(Map<Polynomial, Integer> factors) {
        if (factors.isEmpty()) return "1";

        return factors.entrySet().stream()
                .filter(entry -> !entry.getKey().isConstant() || entry.getKey().getConstantValue() != 1)
                .map(entry -> {
                    String base = "(" + entry.getKey().toString() + ")";
                    int exp = entry.getValue();
                    return exp > 1 ? base + "^" + exp : base;
                })
                .collect(Collectors.joining(" · "));
    }
}