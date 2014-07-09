package org.openexi.tryout;

import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

class XmlPage extends EXITryoutPage implements IPage {

  private static final long serialVersionUID = -4150235341886294373L;
  
  private final SchemaCompilerMainFrame m_mainFrame;
  private final XmlPageDocument m_document;
  
  XmlPage(SchemaCompilerMainFrame tryoutMainFrame) {
    m_mainFrame = tryoutMainFrame;
    m_document = new XmlPageDocument(this, tryoutMainFrame);
    addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {
        m_mainFrame.setSaveAsEnabled(false);        
      }
      public void componentMoved(ComponentEvent e) {
      }
      public void componentResized(ComponentEvent e) {
      }
      public void componentShown(ComponentEvent e) {
        m_statusLabel.setText("");
        m_mainFrame.setPage(XmlPage.this);
        m_mainFrame.setSaveAsEnabled(true);        
        m_mainFrame.setLeftArrowEnabled(true);
        m_mainFrame.setRightArrowEnabled(false);
        m_document.updateValidatorMainFrame();
      }
    });
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
    final JFileChooser fileChooser = m_mainFrame.fileChooser;
    fileChooser.setSelectedFile(new File(""));
    if (JFileChooser.APPROVE_OPTION == fileChooser.showSaveDialog(this)) {
      File file = fileChooser.getSelectedFile();
      if (!file.exists() || isOverwriteConfirmed(file)) {
        final byte[] exiStream;
        try {
          exiStream = m_document.getBytes();
        }
        catch (Exception e) {
          m_statusLabel.setText(e.getMessage());
          return;
        }
        assert exiStream != null;
        try {
          final OutputStream fos = new FileOutputStream(file);
          fos.write(exiStream);
          fos.close();
        } catch (IOException ioe) {
          m_statusLabel.setText(ioe.getMessage());
          return;
        }
        m_document.setEXIStream((byte[])null);
        setStatusText(m_mainFrame.getMessageResolver().getMessage(
            SchemaCompilerXMsg.MF_STATUS_IO_SUCCESS_FILE_SAVED, 
            new String[] { file.getAbsolutePath() }));
      }
    }
  }

  private boolean isOverwriteConfirmed(File file){
    final String message = m_mainFrame.getMessageResolver().getMessage(
        SchemaCompilerXMsg.MF_STATUS_CONFIRMATION_FILE_OVERWRITE,
        new String[] { file.getAbsolutePath() });

    final int confirm = JOptionPane.showConfirmDialog(this, message, 
        "Confirmation required", JOptionPane.WARNING_MESSAGE, JOptionPane.OK_CANCEL_OPTION );

    return confirm == JOptionPane.OK_OPTION; 
  }
  
  @Override
  public void copy() {
    assert false;
  }

  @Override
  public void selectAll() {
    assert false;
  }

  @Override
  public JLabel getStatusLabel() {
    return m_statusLabel;
  }

  /////////////////////////////////////////////////////////////////////////
  // IPage method implementation
  /////////////////////////////////////////////////////////////////////////
  
  public ITryoutMainFrame getMainFrame() {
    return m_mainFrame;
  }
  
  /////////////////////////////////////////////////////////////////////////
  // IXmlPage method implementation
  /////////////////////////////////////////////////////////////////////////

}
