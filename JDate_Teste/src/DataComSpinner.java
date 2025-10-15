import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataComSpinner {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Selecionar Data");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 150);
        frame.setLayout(new FlowLayout());

        JLabel label = new JLabel("Data:");
        SpinnerDateModel model = new SpinnerDateModel();
        JSpinner spinner = new JSpinner(model);
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        spinner.setEditor(editor);

        JButton btn = new JButton("OK");
        btn.addActionListener(e -> {
            Date data = (Date) spinner.getValue();
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            JOptionPane.showMessageDialog(frame, "Selecionou: " + sdf.format(data));
        });

        frame.add(label);
        frame.add(spinner);
        frame.add(btn);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }
}
