package org.openexi.tryout;

import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;

public class CaretListenerLabel extends JLabel implements CaretListener {
  
  private static final long serialVersionUID = 1624417145962945135L;
  
  private final JTextArea m_textArea;

  public CaretListenerLabel(JTextArea textArea) {
    super();
    m_textArea = textArea;
    m_textArea.addCaretListener(this);
  }

  public void caretUpdate(CaretEvent e) {
    int line = 1, column = 1;
    try {
      line = m_textArea.getLineOfOffset(e.getDot());
      column = e.getDot() - m_textArea.getLineStartOffset(line);
      setText("line:" + ++line + " col:" + ++column);
    }
    catch (BadLocationException ble) {
      ble.printStackTrace();
    }
  }
}
