package PollardRho;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigInteger;
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
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        
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


        solveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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

            ArrayList<PollardState> history = new ArrayList<>();
            Map<BigInteger, Integer> zMap = new HashMap<>(); 

            PollardState currentState = new PollardState(BigInteger.ONE, BigInteger.ZERO, BigInteger.ZERO);
            history.add(currentState);
            zMap.put(currentState.z, 0);

            int k = 1; 
            while (true) {
                PollardState nextState = nextStep(currentState);

                if (zMap.containsKey(nextState.z)) {
                    int j = zMap.get(nextState.z);
                    history.add(nextState); 

                    sb.append(String.format("%-4s | %-5s | %-5s | %-5s\n", "i", "u_i", "v_i", "z_i"));
                    sb.append("---------------------------\n");
                    for (int i = 0; i < history.size(); i++) {
                        PollardState s = history.get(i);
                        sb.append(String.format("%-4d | %-5s | %-5s | %-5s\n", i, s.u, s.v, s.z));
                    }
                    sb.append("\n");

                    sb.append(String.format("Коллизия найдена: z_%d = z_%d = %s\n", j, k, nextState.z));
                    
                    PollardState state_j = history.get(j); 
                    PollardState state_k = nextState;      

                    BigInteger u_j = state_j.u;
                    BigInteger v_j = state_j.v;
                    BigInteger u_k = state_k.u;
                    BigInteger v_k = state_k.v;
                    
                    sb.append(String.format("i = %d, k = %d\n", k, j));
                    sb.append(String.format("u_%d = %s, v_%d = %s\n", k, u_k, k, v_k));
                    sb.append(String.format("u_%d = %s, v_%d = %s\n", j, u_j, j, v_j));
                    sb.append("\n");

                    BigInteger u_diff = u_j.subtract(u_k).mod(pMinus1); 
                    BigInteger v_diff = v_k.subtract(v_j).mod(pMinus1); 

                    sb.append(String.format("x ≡ (u_%d - u_%d)⁻¹ * (v_%d - v_%d) mod (%s - 1)\n", j, k, k, j, p));
                    sb.append(String.format("x ≡ (%s - %s)⁻¹ * (%s - %s) mod %s\n", u_j, u_k, v_k, v_j, pMinus1));
                    sb.append(String.format("x ≡ (%s)⁻¹ * (%s) mod %s\n", u_diff, v_diff, pMinus1));
                    
                    if (!u_diff.gcd(pMinus1).equals(BigInteger.ONE)) {
                         sb.append("\nОшибка: не существует обратного элемента, т.к.\n");
                         sb.append(String.format("НОД(u_j - u_k, p-1) = НОД(%s, %s) = %s ≠ 1\n",
                                 u_diff, pMinus1, u_diff.gcd(pMinus1)));
                         return sb.toString();
                    }
                    
                    BigInteger u_diff_inv = u_diff.modInverse(pMinus1);
                    sb.append(String.format("x ≡ %s * %s mod %s\n", u_diff_inv, v_diff, pMinus1));
                    
                    BigInteger x = u_diff_inv.multiply(v_diff).mod(pMinus1);
                    sb.append(String.format("x = %s\n\n", x));
                    
                    BigInteger check = a.modPow(x, p);
                    sb.append("Проверка: " + a + "^" + x + " mod " + p + " = " + check);
                    if (check.equals(b)) {
                        sb.append(" (Верно!)");
                    } else {
                        sb.append(" (Неверно!)");
                    }
                    
                    if (a.equals(new BigInteger("3")) && b.equals(new BigInteger("12")) && p.equals(new BigInteger("17"))) {
                         sb.append("\n(Ответ в примере: 13)");
                    }

                    return sb.toString(); 
                }

                history.add(nextState);
                zMap.put(nextState.z, k);
                currentState = nextState;
                k++;

                if (k > p.intValue() * 2) {
                    throw new ArithmeticException("Коллизия не найдена (превышен лимит итераций)");
                }
            }
        }

        @Override
        protected void done() {
            try {

                String result = get(); 
                resultArea.setText(result);
                
            } catch (InterruptedException | ExecutionException e) {
                resultArea.setText("Ошибка вычисления: \n" + e.getCause().getMessage());
            }
            
            solveButton.setEnabled(true); 
        }

        private PollardState nextStep(PollardState currentState) {
            BigInteger z = currentState.z;
            BigInteger u = currentState.u;
            BigInteger v = currentState.v;
            BigInteger pMinus1 = p.subtract(BigInteger.ONE);

            BigInteger p_div_3 = p.divide(BigInteger.valueOf(3));
            BigInteger p_mul_2_div_3 = p.multiply(BigInteger.valueOf(2)).divide(BigInteger.valueOf(3));

            BigInteger nextZ = null;
            BigInteger nextU = null;
            BigInteger nextV = null;

            if (z.compareTo(BigInteger.ZERO) > 0 && z.compareTo(p_div_3) <= 0) {
                nextZ = b.multiply(z).mod(p);                 
                nextU = u.add(BigInteger.ONE).mod(pMinus1);   
                nextV = v.mod(pMinus1);                       
            }
            else if (z.compareTo(p_div_3) > 0 && z.compareTo(p_mul_2_div_3) <= 0) {
                nextZ = z.modPow(BigInteger.TWO, p);          
                nextU = u.multiply(BigInteger.TWO).mod(pMinus1); 
                nextV = v.multiply(BigInteger.TWO).mod(pMinus1); 
            }
            else {
                nextZ = a.multiply(z).mod(p);                 
                nextU = u.mod(pMinus1);                       
                nextV = v.add(BigInteger.ONE).mod(pMinus1);   
            }
            
            return new PollardState(nextZ, nextU, nextV);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new PollardRhoGUI().setVisible(true);
            }
        });
    }
}