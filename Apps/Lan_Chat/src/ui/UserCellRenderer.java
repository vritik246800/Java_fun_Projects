package ui;

import javax.swing.*;
import java.awt.*;

public class UserCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        JLabel label = (JLabel) super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        String item = value.toString(); // user@ip
        String[] parts = item.split("\\|", 2);

        label.setText(parts[0].trim());
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        return label;
    }
}
