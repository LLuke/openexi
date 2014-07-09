package org.openexi.tryout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public abstract class EXITryoutPage extends JPanel implements IPage {
  
  protected JLabel m_statusLabel;
  
  private static final long serialVersionUID = 7641548052972965103L;

  public final void initPage() {
    getPageDocument().updatePage();
  }
  
  protected abstract PageDocument getPageDocument();
  
  public abstract ITryoutMainFrame getMainFrame();
  
  public abstract void saveAs();
  
  public abstract void copy();
  public abstract void selectAll();
  
  public final void setStatusLabel(JLabel statusLabel) {
    m_statusLabel = statusLabel;
  }
  
  public abstract JLabel getStatusLabel();

  public final void setStatusText(String text) {
    getStatusLabel().setText(text);
  }

}
