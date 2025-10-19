import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PollardRhoGUI extends JFrame {

    private final JTextField aField = new JTextField(20);
    private final JTextField bField = new JTextField(20);
    private final JTextField pField = new JTextField(20);
    private final JButton solveButton = new JButton("Найти x");
    private final JTextArea resultArea = new JTextArea(10, 40);

    private static class PollardState {
        final BigInteger z, u, v;

        PollardState(BigInteger z, BigInteger u, BigInteger v) {
            this.z = z;
            this.u = u;
            this.v = v;
        }
    }

    public PollardRhoGUI() {
        super("ρ-метод Полларда для дискретного логарифма");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); 

        gbc.anchor = GridBagConstraints.EAST; 
        gbc.fill = GridBagConstraints.NONE;
        
        gbc.gridx = 0; gbc.gridy = 0; add(new JLabel("a (основание):"), gbc);
        gbc.gridx = 0; gbc.gridy = 1; add(new JLabel("b (результат):"), gbc);
        gbc.gridx = 0; gbc.gridy = 2; add(new JLabel("p (модуль):"), gbc);

        gbc.anchor = GridBagConstraints.WEST; 
        gbc.fill = GridBagConstraints.HORIZONTAL; 
        gbc.weightx = 1.0; 
        
        gbc.gridx = 1; gbc.gridy = 0; add(aField, gbc);
        gbc.gridx = 1; gbc.gridy = 1; add(bField, gbc);
        gbc.gridx = 1; gbc.gridy = 2; add(pField, gbc);

        gbc.anchor = GridBagConstraints.CENTER; 
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        gbc.gridwidth = 2; 
        gbc.gridx = 0; gbc.gridy = 3;
        add(solveButton, gbc);

        gbc.fill = GridBagConstraints.BOTH; 
        gbc.weightx = 1.0; 
        gbc.weighty = 1.0; 
        gbc.gridx = 0; gbc.gridy = 4;
        
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        add(new JScrollPane(resultArea), gbc);

        aField.setText("3");
        bField.setText("12");
        pField.setText("17");

        solveButton.addActionListener(e -> {
            try {
                BigInteger a = new BigInteger(aField.getText().trim());
                BigInteger b = new BigInteger(bField.getText().trim());
                BigInteger p = new BigInteger(pField.getText().trim());

                resultArea.setText("Генерирую таблицу и ищу коллизию...\n" + 
                                   a + "^x ≡ " + b + " (mod " + p + ")\n");
                solveButton.setEnabled(false); 
                new SolverTask(a, b, p).execute(); 

            } catch (NumberFormatException ex) {
                resultArea.setText("Ошибка: Введите корректные целые числа.");
                solveButton.setEnabled(true);
            }
        });

        setMinimumSize(new Dimension(1000, 800)); 
        pack();
        setLocationRelativeTo(null); 
    }

    private class SolverTask extends SwingWorker<String, Void> {
        private final BigInteger a, b, p;

        SolverTask(BigInteger a, BigInteger b, BigInteger p) {
            this.a = a;
            this.b = b;
            this.p = p;
        }

        @Override
        protected String doInBackground() throws Exception {
            StringBuilder sb = new StringBuilder(); 
            BigInteger pMinus1 = p.subtract(BigInteger.ONE);

            sb.append("Определяем интервалы для z_i:\n");
            BigDecimal p_dec = new BigDecimal(p);
            BigDecimal p_div_3_dec = p_dec.divide(new BigDecimal(3), 2, RoundingMode.FLOOR);
            BigDecimal p_mul_2_div_3_dec = p_dec.multiply(new BigDecimal(2)).divide(new BigDecimal(3), 2, RoundingMode.FLOOR);
            sb.append(String.format("T1:    0        < z_i <= %s (%s/3)\n", p_div_3_dec, p));
            sb.append(String.format("T2:    %s  < z_i <= %s (2*%s/3)\n", p_div_3_dec, p_mul_2_div_3_dec, p));
            sb.append(String.format("T3:    %s < z_i <    %s\n\n", p_mul_2_div_3_dec, p));

            ArrayList<PollardState> history = new ArrayList<>();
            Map<BigInteger, Integer> zMap = new HashMap<>(); 

            PollardState currentState = new PollardState(BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO);
            history.add(currentState);
            zMap.put(currentState.z, 0);

            // ИЗМЕНЕНИЕ: k -> j (индекс текущей, большей итерации)
            int j = 1; 
            while (true) {
                PollardState nextState = nextStep(currentState);

                if (zMap.containsKey(nextState.z)) {
                    // ИЗМЕНЕНИЕ: j -> k (индекс прошлой, меньшей итерации)
                    int k = zMap.get(nextState.z);
                    history.add(nextState); 

                    sb.append(String.format("%-4s | %-5s | %-5s | %-5s\n", "i", "u_i", "v_i", "z_i"));
                    sb.append("---------------------------\n");
                    for (int i = 0; i < history.size(); i++) {
                        PollardState s = history.get(i);
                        sb.append(String.format("%-4d | %-5s | %-5s | %-5s\n", i, s.u, s.v, s.z));
                    }
                    sb.append("\n");

                    // ИЗМЕНЕНИЕ: Правильный порядок k и j
                    sb.append(String.format("Коллизия найдена: z_%d = z_%d = %s\n", k, j, nextState.z));
                    
                    PollardState state_k = history.get(k); // Состояние с меньшим индексом
                    PollardState state_j = nextState;      // Состояние с большим индексом

                    BigInteger u_k = state_k.u; BigInteger v_k = state_k.v;
                    BigInteger u_j = state_j.u; BigInteger v_j = state_j.v;
                    
                    // ИЗМЕНЕНИЕ: Правильный порядок j и k
                    sb.append(String.format("j = %d, k = %d\n", j, k));
                    sb.append(String.format("u_%d = %s, v_%d = %s\n", j, u_j, j, v_j));
                    sb.append(String.format("u_%d = %s, v_%d = %s\n", k, u_k, k, v_k));
                    sb.append("\n");
                    
                    // ИЗМЕНЕНИЕ: Используем правильную формулу (u_j - u_k) и (v_k - v_j)
                    BigInteger u_diff_raw = u_j.subtract(u_k);
                    BigInteger v_diff_raw = v_k.subtract(v_j);

                    sb.append(String.format("x ≡ (u_%d - u_%d)⁻¹ * (v_%d - v_%d) mod (%s - 1)\n", j, k, k, j, p));
                    sb.append(String.format("x ≡ (%s - %s)⁻¹ * (%s - %s) mod %s\n", u_j, u_k, v_k, v_j, pMinus1));
                    sb.append(String.format("x ≡ (%s)⁻¹ * (%s) mod %s\n", u_diff_raw, v_diff_raw, pMinus1));
                    
                    BigInteger u_diff_mod = u_diff_raw.mod(pMinus1);
                    if (!u_diff_mod.gcd(pMinus1).equals(BigInteger.ONE)) {
                         sb.append("\nОшибка: не существует обратного элемента, т.к.\n");
                         sb.append(String.format("НОД(u_j - u_k, p-1) = НОД(%s, %s) = %s ≠ 1\n",
                                 u_diff_mod, pMinus1, u_diff_mod.gcd(pMinus1)));
                         return sb.toString();
                    }
                    
                    BigInteger u_diff_inv = u_diff_mod.modInverse(pMinus1);
                    BigInteger v_diff_mod = v_diff_raw.mod(pMinus1);
                    sb.append(String.format("x ≡ %s * %s mod %s\n", u_diff_inv, v_diff_mod, pMinus1));
                    
                    BigInteger x = u_diff_inv.multiply(v_diff_mod).mod(pMinus1);
                    sb.append(String.format("x = %s mod %s\n\n", x, pMinus1));
                    
                    BigInteger check = a.modPow(x, p);
                    sb.append("Проверка: " + a + "^" + x + " mod " + p + " = " + check);
                    if (check.equals(b)) {
                        sb.append(" (Верно!)");
                    } else {
                        sb.append(" (Неверно!)");
                    }
                    
                    sb.append(String.format("\n\nОтвет: %s mod %s", x, pMinus1));

                    return sb.toString(); 
                }

                history.add(nextState);
                zMap.put(nextState.z, j);
                currentState = nextState;
                j++; // ИЗМЕНЕНИЕ: k++ -> j++

                if (j > p.intValue() * 2) { // ИЗМЕНЕНИЕ: k -> j
                    throw new ArithmeticException("Коллизия не найдена (превышен лимит итераций)");
                }
            }
        }

        @Override
        protected void done() {
            try {
                resultArea.setText(get());
            } catch (InterruptedException | ExecutionException e) {
                resultArea.setText("Ошибка вычисления: \n" + e.getCause().getMessage());
            } finally {
                solveButton.setEnabled(true); 
            }
        }

        private PollardState nextStep(PollardState currentState) {
            BigInteger z = currentState.z; BigInteger u = currentState.u; BigInteger v = currentState.v;
            BigInteger pMinus1 = p.subtract(BigInteger.ONE);
            BigInteger p_div_3 = p.divide(BigInteger.valueOf(3));
            BigInteger p_mul_2_div_3 = p.multiply(BigInteger.valueOf(2)).divide(BigInteger.valueOf(3));
            if (z.compareTo(p_div_3) <= 0) {
                return new PollardState(b.multiply(z).mod(p), u.add(BigInteger.ONE).mod(pMinus1), v);
            } else if (z.compareTo(p_mul_2_div_3) <= 0) {
                return new PollardState(z.modPow(BigInteger.TWO, p), u.multiply(BigInteger.TWO).mod(pMinus1), v.multiply(BigInteger.TWO).mod(pMinus1));
            } else {
                return new PollardState(a.multiply(z).mod(p), u, v.add(BigInteger.ONE).mod(pMinus1));
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PollardRhoGUI().setVisible(true));
    }
}