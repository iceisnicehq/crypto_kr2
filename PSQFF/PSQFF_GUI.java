import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

public class PSQFF_GUI extends JFrame {

    private final JTextField polynomialField;
    private final JTextField modulusField;
    private final JTextArea resultArea;

    public PSQFF_GUI() {
    
        setTitle("Алгоритм факторизации многочленов PSQFF");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);

        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.X_AXIS));
        
        polynomialField = new JTextField("x^7 + x^6 + x^5 + x^4 + x^2 + 1");
        modulusField = new JTextField("2", 3);
        JButton factorButton = new JButton("Факторизовать");

        Font inputFont = new Font("SansSerif", Font.PLAIN, 20);
        polynomialField.setFont(inputFont);
        modulusField.setFont(inputFont);

        
        inputPanel.add(new JLabel("p(x) = "));
        inputPanel.add(polynomialField);
        inputPanel.add(Box.createHorizontalStrut(10));
        inputPanel.add(new JLabel("   mod "));
        inputPanel.add(modulusField);
        inputPanel.add(Box.createHorizontalStrut(10));
        inputPanel.add(factorButton);
        
        mainPanel.add(inputPanel, BorderLayout.NORTH);

        
        resultArea = new JTextArea();
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setEditable(false);
        resultArea.setLineWrap(true);
        resultArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(resultArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

       
        factorButton.addActionListener(this::performFactorization);
        
        
        add(mainPanel);
        setVisible(true);
    }

    private void performFactorization(ActionEvent e) {
        try {
            String polyString = polynomialField.getText();
            int modulus = Integer.parseInt(modulusField.getText());

            
            if (modulus <= 1) {
                JOptionPane.showMessageDialog(this, "Модуль 'p' должен быть простым числом > 1.", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

          
            ReportGenerator generator = new ReportGenerator(modulus);
            Polynomial p = Polynomial.fromString(polyString);
            
            String report = generator.generateReport(p);
            resultArea.setText(report);
            resultArea.setCaretPosition(0); 

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Неверный формат модуля. Введите целое число.", "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Произошла ошибка: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        
        SwingUtilities.invokeLater(PSQFF_GUI::new);
    }
}