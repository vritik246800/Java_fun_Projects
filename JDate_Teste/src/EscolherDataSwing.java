import com.toedter.calendar.JDateChooser;
import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;

public class EscolherDataSwing {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Selecionar Data");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new FlowLayout());

        JLabel label = new JLabel("Escolha a data:");
        JDateChooser dateChooser = new JDateChooser();
        JButton btn = new JButton("Mostrar Data");

        btn.addActionListener(e -> {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String data = sdf.format(dateChooser.getDate());
            JOptionPane.showMessageDialog(frame, "Data selecionada: " + data);
        });

        frame.add(label);
        frame.add(dateChooser);
        frame.add(btn);

        frame.setVisible(true);
    }
}
