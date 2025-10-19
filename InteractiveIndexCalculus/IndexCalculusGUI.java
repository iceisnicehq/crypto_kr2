package InteractiveIndexCalculus;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class IndexCalculusGUI extends JFrame {

    private final JTextField pField = new JTextField("47", 10);
    private final JTextField gField = new JTextField("10", 10);
    private final JTextField aField = new JTextField("17", 10);
    private final JSpinner tSpinner = new JSpinner(new SpinnerNumberModel(3, 2, 10, 1)); // t=3
    private final JSpinner cSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1)); // c=1
    private final JButton calculateButton = new JButton("Вычислить x");
    private final JTextArea logArea = new JTextArea(25, 60);
    private final JTextField resultField = new JTextField(15);
    private final JProgressBar progressBar = new JProgressBar();

    private IndexCalculusWorker worker;

    public IndexCalculusGUI() {
        super("Алгоритм исчисления порядка (СТРОГО по лекции, v4)");
        initUI();
    }

    private void initUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Входные данные (g^x ≡ a (mod p))"));
        inputPanel.add(new JLabel("p:"));
        inputPanel.add(pField);
        inputPanel.add(new JLabel("g:"));
        inputPanel.add(gField);
        inputPanel.add(new JLabel("a:"));
        inputPanel.add(aField);
        inputPanel.add(new JLabel("База t:"));
        inputPanel.add(tSpinner);

        cSpinner.setModel(new SpinnerNumberModel(1, 1, 30, 1));
        inputPanel.add(new JLabel("Доп. уравн. c:"));
        inputPanel.add(cSpinner);
        add(inputPanel, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Ход решения (по лекции)"));
        add(logScrollPane, BorderLayout.CENTER);

        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.add(calculateButton);
        controlPanel.add(new JLabel("Результат x:"));
        resultField.setEditable(false);
        resultField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
        controlPanel.add(resultField);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(controlPanel, BorderLayout.CENTER);
        progressBar.setStringPainted(true);
        bottomPanel.add(progressBar, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startCalculation();
            }
        });

        pack();
        setLocationRelativeTo(null);
    }

private void startCalculation() {
        try {
            BigInteger p = new BigInteger(pField.getText().trim());
            BigInteger g = new BigInteger(gField.getText().trim());
            BigInteger a = new BigInteger(aField.getText().trim());
            int t = (Integer) tSpinner.getValue();
            int c = (Integer) cSpinner.getValue();

            if (!p.isProbablePrime(50)) {
                JOptionPane.showMessageDialog(this, "p должно быть простым числом!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            logArea.setText("");
            resultField.setText("");
            calculateButton.setEnabled(false);
            calculateButton.setText("Вычисление...");
            progressBar.setValue(0);
            progressBar.setString("");

            worker = new IndexCalculusWorker(p, g, a, t, c, logArea, progressBar);
            worker.execute();

            worker.addPropertyChangeListener(evt -> {
                if ("state".equals(evt.getPropertyName()) && SwingWorker.StateValue.DONE.equals(evt.getNewValue())) {
                    try {
                        BigInteger result = worker.get();
                        if (result != null) {
                            resultField.setText(result.toString());
                            progressBar.setValue(100);
                            progressBar.setString("Готово!");
                            calculateButton.setEnabled(true);
                            calculateButton.setText("Вычислить x");
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        String errorMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                        
                        SpinnerNumberModel cModel = (SpinnerNumberModel) cSpinner.getModel();
                        int currentC = (Integer) cModel.getValue();
                        int nextC = currentC + 1;
                        Integer maxC = (Integer) cModel.getMaximum();
                        
                        if (errorMsg.contains("Не удалось решить систему") && (maxC == null || nextC <= maxC)) {
                            logArea.append(String.format("\n\n!!! АВТОПЕРЕЗАПУСК: %s !!!", errorMsg));
                            logArea.append(String.format("Увеличиваем 'c' с %d до %d и пробуем снова...", currentC, nextC));
                            cSpinner.setValue(nextC);
                            
                            Timer timer = new Timer(1500, new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent ae) {
                                    startCalculation(); 
                                }
                            });
                            timer.setRepeats(false);
                            timer.start();
                            
                        } else {
                            resultField.setText("Ошибка");
                            logArea.append(String.format("\n\n!!! КРИТИЧЕСКАЯ ОШИБКА: %s !!!", errorMsg));
                            
                            if (maxC != null && nextC > maxC) {
                                logArea.append(String.format("\nДостигнут лимит 'c' (%d). Автоповтор остановлен.", maxC));
                            }
                            
                            calculateButton.setEnabled(true); 
                            calculateButton.setText("Вычислить x");
                        }
                    }
                }
            });

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный формат числа.", "Ошибка", JOptionPane.ERROR_MESSAGE);
            calculateButton.setEnabled(true);
            calculateButton.setText("Вычислить x");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new IndexCalculusGUI().setVisible(true);
            }
        });
    }
}

class IndexCalculusWorker extends SwingWorker<BigInteger, String> {

    private static final BigInteger ZERO = BigInteger.ZERO;
    private static final BigInteger ONE = BigInteger.ONE;
    private static final long MAX_ITERATIONS = 1_000_000; 

    private final BigInteger p;
    private final BigInteger g;
    private final BigInteger a;
    private final int t;
    private final int c;
    private final BigInteger pMinus1;
    private final BigInteger maxIterBI = BigInteger.valueOf(MAX_ITERATIONS);

    private final JTextArea logArea;
    private final JProgressBar progressBar;

    private List<BigInteger> factorBase;
    private List<Relation> allRelations;
    private BigInteger[] baseLogs;

    public IndexCalculusWorker(BigInteger p, BigInteger g, BigInteger a, int t, int c, JTextArea logArea, JProgressBar progressBar) {
        this.p = p;
        this.g = g;
        this.a = a;
        this.t = t;
        this.c = c;
        this.pMinus1 = p.subtract(ONE);
        this.logArea = logArea;
        this.progressBar = progressBar;
    }

    @Override
    protected BigInteger doInBackground() throws Exception {
        
        publish("--- 1. Выбираем факторную базу (t = " + t + ") ---");
        this.factorBase = generateFactorBase(t);
        publish("S = " + factorBase.toString() + "\n");
        setProgress(10, "Генерация базы...");

        int numRelationsToFind = t + c;
        publish(String.format("--- 2-3. Ищем %d 'гладких' g^k (перебор k=1, 2, 3...) ---", numRelationsToFind));
        this.allRelations = findRelations(numRelationsToFind);
        setProgress(40, "Поиск отношений...");

        publish(String.format("\n--- 4-5. Логарифмируем и решаем систему (mod %s) ---", pMinus1));
        logRelations(); 

        publish("\nРешение системы:");
        publish("Ищем первую решаемую (невырожденную) t x t подсистему...");
        this.baseLogs = solveSystemRobustly();
        
        publish("\nРешение (логарифмы базы):");
        for (int i = 0; i < t; i++) {
            publish(String.format("U_%d (log_g(%s)) = %s mod %s",
                    i + 1, factorBase.get(i), baseLogs[i], pMinus1));
        }
        setProgress(70, "Решение СЛАУ...");

        publish(String.format("\n--- 6-7. Ищем 'гладкое' a*g^s (перебор s=1, 2, 3...) ---", a));
        Relation finalRelation = findFinalRelation();
        String finalFactors = finalRelation.getFactorString(factorBase);
        
        publish(String.format("\nНайдено при s = %s:", finalRelation.k));
        publish(String.format("%s * %s^%s mod %s ≡ %s = %s",
                a, g, finalRelation.k, p, finalRelation.value, finalFactors));
        setProgress(90, "Поиск для 'a'...");

        publish("\n--- 8. Логарифмируем и вычисляем x ---");
        publish(String.format("log_g(%s * g^%s) ≡ log_g(%s)", a, finalRelation.k, finalFactors));
        publish(String.format("log_g(%s) + log_g(g^%s) ≡ log_g(%s)", a, finalRelation.k, finalFactors));
        
        BigInteger s = finalRelation.k;
        BigInteger sum = ZERO;
        StringBuilder sumLog = new StringBuilder();

        for (int i = 0; i < t; i++) {
            BigInteger pi = factorBase.get(i);
            Integer ai_int = finalRelation.exponents.get(pi);
            if (ai_int != null && ai_int > 0) {
                BigInteger ai = BigInteger.valueOf(ai_int);
                BigInteger log_pi = baseLogs[i];
                sum = sum.add(ai.multiply(log_pi)).mod(pMinus1);
                
                if (sumLog.length() > 0) sumLog.append(" + ");
                sumLog.append(String.format("%s*log_g(%s)", ai, pi));
            }
        }
        
        publish(String.format("x + %s ≡ %s (mod %s)", s, sumLog.toString(), pMinus1));
        
        BigInteger x = sum.subtract(s).mod(pMinus1);
        
        publish(String.format("x ≡ (%s) - %s ≡ %s (mod %s)", sumLog.toString(), s, x, pMinus1));
        publish(String.format("x ≡ (%s) - %s ≡ %s (mod %s)", sum, s, x, pMinus1));

        publish("\n--- ПРОВЕРКА ---");
        BigInteger check = g.modPow(x, p);
        publish(String.format("%s^%s ≡ %s (mod %s)", g, x, check, p));
        if (check.equals(a)) {
            publish(String.format("Верно! %s ≡ %s.", check, a));
            publish(String.format("\nОтвет: %s mod %s", x, p));
            return x;
        } else {
            publish(String.format("ОШИБКА! %s != %s.", check, a));
            throw new Exception("Ошибка проверки. Алгоритм дал неверный ответ.");
        }
    }

    private List<BigInteger> generateFactorBase(int size) {
        List<BigInteger> base = new ArrayList<>(size);
        BigInteger currentPrime = BigInteger.valueOf(2);
        while (base.size() < size) {
            base.add(currentPrime);
            currentPrime = currentPrime.nextProbablePrime();
        }
        return base;
    }

    private List<Relation> findRelations(int num) throws Exception {
        List<Relation> foundRelations = new ArrayList<>();
        int foundCount = 0;
        
        for (BigInteger k = ONE; k.compareTo(maxIterBI) <= 0; k = k.add(ONE)) {
            BigInteger gk = g.modPow(k, p);
            
            String logMsg = String.format("k = %-4s: %s^%s mod %s ≡ %-4s", k, g, k, p, gk);

            Map<BigInteger, Integer> exponents = new HashMap<>();
            String factorString = trialFactor(gk, exponents);

            if (factorString != null) {
                Relation rel = new Relation(k, gk, exponents);
                foundRelations.add(rel);
                foundCount++;
                publish(String.format("%s = %s (Найдено %d/%d)",
                        logMsg, factorString, foundCount, num));
            } else {
                if (k.longValue() < 15 || k.longValue() % 100 == 0) {
                     publish(String.format("%s (Пропуск, не раскладывается)", logMsg));
                }
            }
            
            if (foundCount >= num) {
                return foundRelations;
            }
        }
        throw new Exception("Не удалось найти " + num + " отношений за " + MAX_ITERATIONS + " итераций. Увеличьте 't'.");
    }

    private void logRelations() {
        for (int i = 0; i < allRelations.size(); i++) {
            Relation rel = allRelations.get(i);
            StringBuilder eq = new StringBuilder();
            for (int j = 0; j < t; j++) {
                BigInteger pj = factorBase.get(j);
                int exp = rel.exponents.getOrDefault(pj, 0);
                if (exp > 0) {
                    if (eq.length() > 0) eq.append(" + ");
                    String U_i = String.format("U_%d", j + 1);
                    eq.append(exp == 1 ? U_i : String.format("%d*%s", exp, U_i));
                }
            }
            publish(String.format("Уравн. %-2d: %s ≡ %s", (i + 1), eq.toString(), rel.k));
        }
    }

    private BigInteger[] solveSystemRobustly() throws Exception {
        if (allRelations.size() < t) {
            throw new Exception("Недостаточно уравнений (" + allRelations.size() + "<" + t + ")");
        }

        int n = allRelations.size();
        int[] indices = new int[t];

        return findCombinations(0, 0, n, indices);
    }
    
    private BigInteger[] findCombinations(int startIndex, int combinationIndex, int n, int[] indices) throws Exception {
        if (combinationIndex == t) {
            
            return trySolvingCombination(indices);
        }

        if (startIndex >= n) {
            return null; 
        }

        for (int i = startIndex; i < n; i++) {
            indices[combinationIndex] = i;
            BigInteger[] solution = findCombinations(i + 1, combinationIndex + 1, n, indices);
            if (solution != null) {
                return solution; 
            }
        }
        
        if(startIndex == 0) {
             throw new Exception("Не удалось решить систему. Все " + (c+1) + " подсистем вырождены. Увеличьте 'c'.");
        }
        return null;
    }
    
private BigInteger[] trySolvingCombination(int[] indices) {
        StringBuilder sb = new StringBuilder("{"); 
        for (int i = 0; i < indices.length; i++) {
            sb.append(indices[i] + 1); 
            if (i < indices.length - 1) {
                sb.append(", "); 
            }
        }
        sb.append("}"); 
        
        publish("...попытка решения с уравнениями " + sb.toString());
        
        BigInteger[][] A = new BigInteger[t][t];

        BigInteger[] b = new BigInteger[t];

        for (int i = 0; i < t; i++) {
            Relation rel = allRelations.get(indices[i]);
            b[i] = rel.k.mod(pMinus1);
            for (int j = 0; j < t; j++) {
                BigInteger pj = factorBase.get(j);
                A[i][j] = BigInteger.valueOf(rel.exponents.getOrDefault(pj, 0)).mod(pMinus1);
            }
        }

        try {
            BigInteger[] solution = solveLinearSystem(A, b);
            publish("Успех! Система " + sb.toString() + " решена.");
            return solution;
        } catch (Exception e) {
            publish("...неудача: " + e.getMessage() + ". Пробуем след. комбинацию.");
            return null;
        }
    }
    
    private BigInteger[] solveLinearSystem(BigInteger[][] A_in, BigInteger[] b_in) throws Exception {
        BigInteger[][] A = new BigInteger[t][];
        for(int i=0; i<t; i++) A[i] = A_in[i].clone();
        BigInteger[] b = b_in.clone();

        for (int i = 0; i < t; i++) {
            BigInteger pivot = A[i][i];
            BigInteger inv;
            try {
                inv = pivot.modInverse(pMinus1);
            } catch (ArithmeticException e) {
                int swapRow = -1;
                for (int k = i + 1; k < t; k++) {
                    if (!A[k][i].gcd(pMinus1).equals(ONE)) {
                         swapRow = k;
                         break;
                    }
                }
                
                if (swapRow != -1) {
                    BigInteger[] tempA = A[i];
                    A[i] = A[swapRow];
                    A[swapRow] = tempA;
                    BigInteger tempB = b[i];
                    b[i] = b[swapRow];
                    b[swapRow] = tempB;
                    
                    pivot = A[i][i];
                    inv = pivot.modInverse(pMinus1); 
                } else {
                     throw new Exception("Матрица вырождена (НОД(" + pivot + ", " + pMinus1 + ") != 1)");
                }
            }

            for (int j = i; j < t; j++) {
                A[i][j] = A[i][j].multiply(inv).mod(pMinus1);
            }
            b[i] = b[i].multiply(inv).mod(pMinus1);

            for (int k = 0; k < t; k++) {
                if (k == i) continue;
                BigInteger factor = A[k][i];
                if (factor.equals(ZERO)) continue;

                for (int j = i; j < t; j++) {
                    A[k][j] = A[k][j].subtract(factor.multiply(A[i][j])).mod(pMinus1);
                }
                b[k] = b[k].subtract(factor.multiply(b[i])).mod(pMinus1);
            }
        }
        return b;
    }

    private Relation findFinalRelation() throws Exception {
        for (BigInteger s = ONE; s.compareTo(maxIterBI) <= 0; s = s.add(ONE)) {
            BigInteger ags = a.multiply(g.modPow(s, p)).mod(p);
            
            String logMsg = String.format("s = %-4s: %s*%s^%s mod %s ≡ %-4s", s, a, g, s, p, ags);

            Map<BigInteger, Integer> exponents = new HashMap<>();
            String factorString = trialFactor(ags, exponents);
            
            if (factorString != null) {
                publish(String.format("%s = %s (Найдено!)", logMsg, factorString));
                return new Relation(s, ags, exponents);
            } else {
                if (s.longValue() < 15 || s.longValue() % 100 == 0) {
                    publish(String.format("%s (Пропуск, не раскладывается)", logMsg));
                }
            }
        }
        throw new Exception("Не удалось найти 'гладкое' a*g^s за " + MAX_ITERATIONS + " итераций. Увеличьте 't'.");
    }

    private String trialFactor(BigInteger num, Map<BigInteger, Integer> exponents) {
        BigInteger n = new BigInteger(num.toByteArray());
        StringBuilder sb = new StringBuilder();

        for (BigInteger prime : factorBase) {
            if (n.equals(ONE)) break;
            int count = 0;
            while (n.mod(prime).equals(ZERO)) {
                n = n.divide(prime);
                count++;
            }
            if (count > 0) {
                exponents.put(prime, count);
                if (sb.length() > 0) sb.append(" * ");
                sb.append(prime);
                if (count > 1) sb.append("^").append(count);
            }
        }
        
        return n.equals(ONE) ? sb.toString() : null;
    }

    @Override
    protected void process(List<String> chunks) {
        for (String message : chunks) {
            logArea.append(message + "\n");
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    private void setProgress(int value, String text) {
        progressBar.setValue(value);
        progressBar.setString(text);
    }
}

class Relation {
    final BigInteger k;
    final BigInteger value;
    final Map<BigInteger, Integer> exponents;

    Relation(BigInteger k, BigInteger value, Map<BigInteger, Integer> exponents) {
        this.k = k;
        this.value = value;
        this.exponents = exponents;
    }
    
    String getFactorString(List<BigInteger> base) {
        StringBuilder sb = new StringBuilder();
        for (BigInteger prime : base) {
            int exp = exponents.getOrDefault(prime, 0);
            if (exp > 0) {
                if (sb.length() > 0) sb.append(" * ");
                sb.append(prime);
                if (exp > 1) sb.append("^").append(exp);
            }
        }
        return sb.toString().trim();
    }
}