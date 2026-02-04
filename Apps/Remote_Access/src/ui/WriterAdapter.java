package ui;

import javax.swing.*;
import java.io.Writer;

public class WriterAdapter extends Writer {

    private final JTextArea area;

    public WriterAdapter(JTextArea area) {
        this.area = area;
    }

    @Override
    public void write(char[] cbuf, int off, int len) {
        SwingUtilities.invokeLater(() ->
                area.append(new String(cbuf, off, len))
        );
    }

    @Override public void flush() {}
    @Override public void close() {}
}
