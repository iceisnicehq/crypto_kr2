// Файл: ReportGenerator.java (НОВАЯ ИСПРАВЛЕННАЯ ВЕРСИЯ)
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ReportGenerator1 {

    private final StringBuilder report = new StringBuilder();
    private final int characteristic;
    private int stepCounter = 1;

    // Компаратор для красивой сортировки итоговых множителей
    private final Comparator<Polynomial1> polynomialComparator = Comparator
            .comparingInt(Polynomial1::degree).reversed()
            .thenComparing(Polynomial1::toString);

    public ReportGenerator1(int characteristic) {
        this.characteristic = characteristic;
    }

    public String generateReport(Polynomial1 p) {
        report.append("## Алгоритм факторизации многочлена PSQFF (по лекции)\n\n");
        report.append("Факторизуем p(x) = ").append(p).append(" в поле Z_").append(characteristic).append("[x]\n");

        // Рекурсивно раскладываем многочлен и собираем множители
        Map<Polynomial1, Integer> finalFactors = decomposeAndFactor(p);

        report.append("\n--- Шаг ").append(stepCounter++).append(". Формирование ответа ---\n");
        String finalAnswer = formatFinalAnswer(finalFactors);
        report.append("Объединяем результаты всех шагов:\n");
        report.append("p(x) = ").append(p).append(" = ").append(finalAnswer);

        report.append("\n\n**Итоговый ответ:** p(x) = ").append(p).append(" = ").append(finalAnswer);

        return report.toString();
    }
    
    /**
     * Главная рекурсивная функция, которая реализует пошаговый алгоритм из лекции.
     * @param p Многочлен для разложения на текущем шаге.
     * @return Карта "множитель -> его кратность".
     */
    private Map<Polynomial1, Integer> decomposeAndFactor(Polynomial1 p) {
        // Базовый случай: многочлен - константа, раскладывать нечего.
        if (p.isConstant()) {
            return new TreeMap<>(polynomialComparator);
        }

        report.append("\n--- Шаг ").append(stepCounter++).append(" ---\n");
        report.append("1) p(x) = ").append(p).append("\n");

        Polynomial1 derivative = p.derivative();
        report.append("2) g(x) = p'(x) = ").append(derivative).append("\n");

        // Случай 3 из лекции (слайд 18): производная равна нулю
        if (derivative.isZero()) {
            report.append("3) g(x) = 0. Для p(x) имеет место условие v(x^p) = [v(x)]^p.\n");
            Polynomial1 r = p.pthRoot();
            report.append("   Извлекаем корень степени p = ").append(characteristic).append(": r(x) = ").append(r).append("\n");
            
            // Факторизуем этот корень r(x). Его множители - это и есть множители p(x), но в степени p.
            // Вместо рекурсии здесь можно сразу найти его факторы, т.к. он уже будет square-free
            List<Polynomial1> irreducibleFactors = factorSquareFree(r);
            report.append("   Ответ для этого шага: ").append(p).append(" = ");

            Map<Polynomial1, Integer> factors = new TreeMap<>(polynomialComparator);
            for(Polynomial1 factor : irreducibleFactors) {
                factors.put(factor.normalize(), characteristic);
            }
            report.append(formatFinalAnswer(factors)).append("\n");
            return factors;
        }

        report.append("3) g(x) != 0\n");
        
        Polynomial1 d = Polynomial1.gcd(p, derivative, null); // Убираем подробный лог НОД, чтобы не загромождать
        report.append("4) Вычислить d(x) = gcd(p(x), g(x)) = ").append(d).append("\n");

        Polynomial1 h = Polynomial1.divide(p, d, null)[0];
        report.append("5) Получить разложение p(x) = d(x) * h(x) = (").append(d).append(") * (").append(h).append(")\n");
        
        // Теперь рекурсивно обрабатываем каждую часть
        // h(x) - это часть, свободная от квадратов
        // d(x) - это часть с оставшимися кратными множителями
        
        Map<Polynomial1, Integer> finalFactors = new TreeMap<>(polynomialComparator);
        
        // Факторы из h(x) будут иметь кратность 1 (относительно этого шага)
        if (!h.isConstant()) {
            report.append("   -> Часть h(x) = ").append(h).append(" свободна от квадратов. Её множители войдут в ответ в 1-й степени.\n");
            List<Polynomial1> h_factors = factorSquareFree(h);
             for(Polynomial1 factor : h_factors) {
                 finalFactors.merge(factor.normalize(), 1, Integer::sum);
             }
        }

        // Факторы из d(x) получим рекурсивно, и их кратности увеличатся на 1
        if (!d.isConstant()) {
            report.append("   -> Часть d(x) = ").append(d).append(" содержит кратные множители. Переходим к следующему шагу для её разложения.\n");
            Map<Polynomial1, Integer> d_factors = decomposeAndFactor(d);
            for (Map.Entry<Polynomial1, Integer> entry : d_factors.entrySet()) {
                finalFactors.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }
        
        return finalFactors;
    }

    // Простой метод факторизации для свободных от квадратов многочленов
    // (находит только линейные множители)
    private List<Polynomial1> factorSquareFree(Polynomial1 p) {
        List<Polynomial1> factors = new ArrayList<>();
        Polynomial1 remainder = new Polynomial1(p);
        
        if (remainder.degree() < 1) return factors;

        for (long a = 0; a < characteristic; a++) {
            Polynomial1 linearFactor = new Polynomial1(new long[]{(characteristic - a) % characteristic, 1}, characteristic);
            if (!remainder.isConstant() && Polynomial1.divide(remainder, linearFactor, null)[1].isZero()) {
                factors.add(linearFactor);
                remainder = Polynomial1.divide(remainder, linearFactor, null)[0];
            }
        }
        
        if (!remainder.isConstant()) {
            factors.add(remainder);
        }

        if (factors.isEmpty() && !p.isConstant()) {
            factors.add(p);
        }
        return factors;
    }

    private String formatFinalAnswer(Map<Polynomial1, Integer> factors) {
        if (factors.isEmpty()) return "1";

        return factors.entrySet().stream()
                .map(entry -> {
                    String base = "(" + entry.getKey().toString() + ")";
                    int exp = entry.getValue();
                    return exp > 1 ? base + "^" + exp : base;
                })
                .collect(Collectors.joining(" * "));
    }
}