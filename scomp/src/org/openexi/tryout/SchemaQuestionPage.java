package org.openexi.tryout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JLabel;
import javax.swing.JRadioButton;

class SchemaQuestionPage extends EXITryoutPage implements ISchemaQuestionPage {
  
  private static final long serialVersionUID = -8474771701632638462L;
  
  private final SchemaCompilerMainFrame m_mainFrame;
  private SchemaQuestionPageDocument m_document;
  
  private JRadioButton m_withSchemaRadioButton;
  private JRadioButton m_noSchemaRadioButton;
  
  public SchemaQuestionPage(SchemaCompilerMainFrame mainFrame) {
    m_mainFrame = mainFrame;
    m_document = new SchemaQuestionPageDocument(this);
    addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {
      }
      public void componentMoved(ComponentEvent e) {
      }
      public void componentResized(ComponentEvent e) {
      }
      public void componentShown(ComponentEvent e) {
        m_mainFrame.setPage(SchemaQuestionPage.this);
        m_document.updateMainFrame();
      }
    });
  }
  
  /////////////////////////////////////////////////////////////////////////
  // Component setters
  /////////////////////////////////////////////////////////////////////////

  public void setWithSchemaRadioButton(JRadioButton withSchemaRadioButton) {
    m_withSchemaRadioButton = withSchemaRadioButton;
    m_withSchemaRadioButton.addActionListener(new WithSchemaRadioButton_ActionListener());
  }

  public void setNoSchemaRadioButton(JRadioButton noSchemaRadioButton) {
    m_noSchemaRadioButton = noSchemaRadioButton;
    m_noSchemaRadioButton.addActionListener(new NoSchemaRadioButton_ActionListener());
  }
  
  /////////////////////////////////////////////////////////////////////////
  // EXITryoutPage implementation
  /////////////////////////////////////////////////////////////////////////

  @Override
  protected PageDocument getPageDocument() {
    return m_document;
  }

  @Override
  public void saveAs() {
  }

  @Override
  public void copy() {
  }
  
  @Override
  public void selectAll() {
  }
  
  public JLabel getStatusLabel() {
    return m_statusLabel;
  }

  /////////////////////////////////////////////////////////////////////////
  // IPage implementation
  /////////////////////////////////////////////////////////////////////////

  public ITryoutMainFrame getMainFrame() {
    return m_mainFrame;
  }

  /////////////////////////////////////////////////////////////////////////
  // ISchemaQuestionPage implementation
  /////////////////////////////////////////////////////////////////////////

  public void setWithSchema(boolean withSchema) {
    final JRadioButton radioButton = withSchema ? m_withSchemaRadioButton : m_noSchemaRadioButton;
    radioButton.setSelected(true);
  }

  /////////////////////////////////////////////////////////////////////////
  // UI event adapters
  /////////////////////////////////////////////////////////////////////////
  
  private class WithSchemaRadioButton_ActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      m_document.setWithSchema(true);
    }
  }
  
  private class NoSchemaRadioButton_ActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      m_document.setWithSchema(false);
    }
  }
  
}
