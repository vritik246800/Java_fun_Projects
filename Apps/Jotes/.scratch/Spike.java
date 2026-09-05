import javax.swing.*;
import java.awt.*;

/** Spike: confirma que contentsChanged obriga o BasicListUI a recalcular alturas. */
public class Spike {
    static class M extends DefaultListModel<String> {
        void touch(int a, int b) { fireContentsChanged(this, a, b); }
    }

    static float frac = 1f;

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame f = new JFrame();
            f.setUndecorated(true);
            f.setLocation(-10000, -10000);
            M m = new M();
            for (int i = 0; i < 5; i++) m.addElement("row " + i);
            JList<String> list = new JList<>(m);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override
                public Dimension getPreferredSize() {
                    Dimension d = super.getPreferredSize();
                    return new Dimension(d.width, Math.round(d.height * frac));
                }
            });
            f.add(new JScrollPane(list));
            f.pack();

            int full = list.getCellBounds(2, 2).height;
            System.out.println("full=" + full);

            frac = 0.5f;
            m.touch(0, 4);
            int half = list.getCellBounds(2, 2).height;
            System.out.println("halfAfterTouch=" + half);

            frac = 0.25f;
            int quarterNoTouch = list.getCellBounds(2, 2).height;
            System.out.println("quarterNoTouch=" + quarterNoTouch + " (cache staleness check)");

            System.out.println((half < full && half > 0) ? "SPIKE OK" : "SPIKE FAIL");
            f.dispose();
        });
        System.exit(0);
    }
}
