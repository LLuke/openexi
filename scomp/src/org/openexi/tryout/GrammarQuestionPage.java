package org.openexi.tryout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JLabel;
import javax.swing.JRadioButton;

class GrammarQuestionPage extends EXITryoutPage implements IGrammarQuestionPage {

  private static final long serialVersionUID = 2816423793701814390L;

  private final SchemaCompilerMainFrame m_mainFrame;
  private GrammarQuestionPageDocument m_document;

  private JRadioButton m_withEXIRadioButton;
  private JRadioButton m_noEXIRadioButton;
  
  public GrammarQuestionPage(SchemaCompilerMainFrame mainFrame) {
    m_mainFrame = mainFrame;
    m_document = new GrammarQuestionPageDocument(this);
    addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {
      }
      public void componentMoved(ComponentEvent e) {
      }
      public void componentResized(ComponentEvent e) {
      }
      public void componentShown(ComponentEvent e) {
        m_mainFrame.setPage(GrammarQuestionPage.this);
        m_document.updateMainFrame();
      }
    });
  }
  
  /////////////////////////////////////////////////////////////////////////
  // Component setters
  /////////////////////////////////////////////////////////////////////////

  public void setWithEXIRadioButton(JRadioButton withCompressionRadioButton) {
    m_withEXIRadioButton = withCompressionRadioButton;
    m_withEXIRadioButton.addActionListener(new WithEXIRadioButton_ActionListener());
  }

  public void setNoEXIRadioButton(JRadioButton noCompressionRadioButton) {
    m_noEXIRadioButton = noCompressionRadioButton;
    m_noEXIRadioButton.addActionListener(new NoEXIRadioButton_ActionListener());
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

  @Override
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
  // ICompressionQuestionPage implementation
  /////////////////////////////////////////////////////////////////////////

  public void setGrammarFormat(GrammarQuestionPageDocument.GrammarFormat grammarFormat) {
    final JRadioButton radioButton = grammarFormat == GrammarQuestionPageDocument.GrammarFormat.exi ? m_withEXIRadioButton : m_noEXIRadioButton;
    radioButton.setSelected(true);
  }

  /////////////////////////////////////////////////////////////////////////
  // UI event adapters
  /////////////////////////////////////////////////////////////////////////
  
  private class WithEXIRadioButton_ActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      m_document.setGrammarFormat(GrammarQuestionPageDocument.GrammarFormat.exi);
    }
  }
  
  private class NoEXIRadioButton_ActionListener implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      m_document.setGrammarFormat(GrammarQuestionPageDocument.GrammarFormat.xml);
    }
  }

}
