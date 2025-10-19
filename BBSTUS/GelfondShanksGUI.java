// package BBSTUS;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class GelfondShanksGUI extends JFrame {

    // --- Компоненты GUI ---
    private final JTextField aField = new JTextField(5);
    private final JTextField bField = new JTextField(5);
    private final JTextField pField = new JTextField(5);
    private final JTextArea outputArea = new JTextArea();

    public GelfondShanksGUI() {
        // --- Настройка главного окна ---
        setTitle("Решатель дискретного логарифма (Алгоритм Гельфонда-Шенкса)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null); // Центрировать окно
        setLayout(new BorderLayout(10, 10));

        // --- 1. Панель ввода (сверху) ---
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        inputPanel.setBorder(new TitledBorder("Уравнение"));

        inputPanel.add(aField);
        inputPanel.add(new JLabel("^x \u2261")); // ≡
        inputPanel.add(bField);
        inputPanel.add(new JLabel("mod"));
        inputPanel.add(pField);

        JButton solveButton = new JButton("Решить");
        // Размещаем кнопку справа, используя дополнительную панель
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(solveButton);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.EAST);


        // --- 2. Панель со справкой и выводом (центр) ---
        JPanel centerPanel = new JPanel(new BorderLayout(10, 10));
        centerPanel.setBorder(new EmptyBorder(0, 10, 10, 10));

        // Справка
        JTextArea helpArea = new JTextArea();
        helpArea.setEditable(false);
        helpArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        helpArea.setBorder(new TitledBorder("Справка"));
        helpArea.setText(
            "a^x \u2261 b mod p\n" +
            "m = [\u221Ap] + 1\n" +
            "Представляем x как x = i*m - j:\n" +
            "a^(i*m - j) \u2261 b mod p\n" +
            "a^(i*m) * a^(-j) \u2261 b mod p\n" +
            "a^(i*m) \u2261 b * a^j mod p"
        );
        helpArea.setBackground(this.getBackground()); // Цвет фона как у окна

        // Область вывода с прокруткой
        outputArea.setEditable(false);
        outputArea.setFont(new Font("Monospaced", Font.BOLD, 14));
        outputArea.setBorder(new EmptyBorder(5, 5, 5, 5));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setBorder(new TitledBorder("Вывод решения"));

        centerPanel.add(helpArea, BorderLayout.NORTH);
        centerPanel.add(scrollPane, BorderLayout.CENTER);


        // --- Добавление панелей в главное окно ---
        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);

        // --- Установка значений по умолчанию для примера ---
        aField.setText("7");
        bField.setText("2");
        pField.setText("41");

        // --- Назначение действия для кнопки "Решить" ---
        solveButton.addActionListener(e -> solveAndDisplay());
    }

    private void solveAndDisplay() {
        try {
            BigInteger a = new BigInteger(aField.getText().trim());
            BigInteger b = new BigInteger(bField.getText().trim());
            BigInteger p = new BigInteger(pField.getText().trim());

            outputArea.setText("");
            String solution = solve(a, b, p);
            outputArea.setText(solution);

        } catch (NumberFormatException ex) {
            outputArea.setText("Ошибка: Пожалуйста, введите корректные целые числа во все поля.");
        } catch (Exception ex) {
            outputArea.setText("Произошла ошибка: " + ex.getMessage());
        }
    }

    private String solve(BigInteger a, BigInteger b, BigInteger p) {
        StringBuilder sb = new StringBuilder();

        BigInteger m = p.sqrt().add(BigInteger.ONE);
        sb.append("Сначала считается m:\n");
        sb.append("m = [\u221A").append(p).append("] + 1 = ").append(m).append("\n\n");

        sb.append("Имеем:\n");
        sb.append("a = ").append(a).append("; b = ").append(b).append("; p = ").append(p).append("\n\n");
        sb.append("Составляем выражение на основе формулы a^(im) \u2261 b * a^j mod p:\n");
        sb.append(a).append("^(").append("i*").append(m).append(") \u2261 ").append(b).append(" * ").append(a).append("^j mod ").append(p).append("\n\n");

        BigInteger am = a.modPow(m, p);
        sb.append("Считаем левую часть: (").append(a).append("^").append(m).append(")^i \u2261 ").append(am).append("^i mod ").append(p).append("\n");

        Map<BigInteger, BigInteger> giantSteps = new HashMap<>();
        StringBuilder iLine = new StringBuilder("i:\t");
        StringBuilder valLine = new StringBuilder(am + "^i:\t");

        for (BigInteger i = BigInteger.ONE; i.compareTo(m) <= 0; i = i.add(BigInteger.ONE)) {
            BigInteger value = am.modPow(i, p);
            giantSteps.put(value, i);
            iLine.append(i).append("\t");
            valLine.append(value).append("\t");
        }

        sb.append("Составляем таблицу для ").append(am).append("^i:\n");
        sb.append(iLine.toString().trim()).append("\n");
        sb.append(valLine.toString().trim()).append("\n\n");

        sb.append("Теперь составляем таблицу для ").append(b).append(" * ").append(a).append("^j и ищем совпадение:\n");
        StringBuilder jLine = new StringBuilder("j:\t");
        StringBuilder bajLine = new StringBuilder(b + "*" + a + "^j:\t");

        BigInteger match_i = null;
        BigInteger match_j = null;

        for (BigInteger j = BigInteger.ONE; j.compareTo(m) <= 0; j = j.add(BigInteger.ONE)) {
            BigInteger aj = a.modPow(j, p);
            BigInteger value = b.multiply(aj).mod(p);
            jLine.append(j).append("\t");
            bajLine.append(value).append("\t");
            if (giantSteps.containsKey(value)) {
                match_i = giantSteps.get(value);
                match_j = j;
                break;
            }
        }
        sb.append(jLine.toString().trim()).append("\n");
        sb.append(bajLine.toString().trim()).append("\n\n");

        if (match_i != null) {
            sb.append("Найдено совпадение при i = ").append(match_i).append(" и j = ").append(match_j).append("\n");
            BigInteger finalValue = b.multiply(a.modPow(match_j, p)).mod(p);
            sb.append(am).append("^").append(match_i).append(" \u2261 ").append(b).append(" * ").append(a).append("^").append(match_j).append(" (оба равны ").append(finalValue).append(")").append("\n\n");

            sb.append("Составляем уравнение x = i*m - j:\n");
            BigInteger x = match_i.multiply(m).subtract(match_j);
            BigInteger pMinusOne = p.subtract(BigInteger.ONE);
            x = x.mod(pMinusOne);
            sb.append("x = ").append(match_i).append(" * ").append(m).append(" - ").append(match_j).append(" = ").append(x).append(" mod ").append(pMinusOne).append("\n\n");

            // ИЗМЕНЕННАЯ СТРОКА: Формат ответа теперь "x mod p"
            sb.append("Ответ: ").append(x).append(" mod ").append(p);
        } else {
            sb.append("Решение не найдено.");
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GelfondShanksGUI gui = new GelfondShanksGUI();
            gui.setVisible(true);
        });
    }
}