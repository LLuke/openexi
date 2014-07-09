package org.openexi.tryout;

import java.awt.Color;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;

import org.xml.sax.Locator;

class SchemaPage extends EXITryoutPage implements ISchemaPage, ClipboardOwner {
  
  private static final long serialVersionUID = -5259576576091997605L;
  
  private final SchemaCompilerMainFrame m_mainFrame;
  private final SchemaPageDocument m_document;
  
  private JTextField m_fileNameField;
  private JLabel m_fileTypeLabel;
  private JButton m_selectSchemaButton;
  private JLabel m_xmlSchemaLabel;
  private TryoutMessageList m_schemaMessageList;
  
  SchemaPage(SchemaCompilerMainFrame tryoutMainFrame) {
    m_mainFrame = tryoutMainFrame;
    m_document = new SchemaPageDocument(this, tryoutMainFrame);
    addComponentListener(new ComponentListener() {
      public void componentHidden(ComponentEvent e) {
      }
      public void componentMoved(ComponentEvent e) {
      }
      public void componentResized(ComponentEvent e) {
      }
      public void componentShown(ComponentEvent e) {
        m_mainFrame.setPage(SchemaPage.this);
        m_mainFrame.setLeftArrowEnabled(true);
        m_mainFrame.setRightArrowEnabled(false);
        m_document.updateMainFrame();
      }
    });
  }
  
  public void resetPage() {
    initXmlSchemaLabel();
    ListModel<EXISchemaFactoryThread.AnnotException> listModel;
    if ((listModel = m_schemaMessageList.getModel()) instanceof EXISchemaFactoryErrorListModel) {
      ((EXISchemaFactoryErrorListModel)listModel).clear();
    }
    setStatusText("");
  }

  void initXmlSchemaLabel() {
    m_xmlSchemaLabel.setForeground(Color.red);
    m_xmlSchemaLabel.setText("  Select an XML Schema File     ");
  }

  /////////////////////////////////////////////////////////////////////////
  // Component setters
  /////////////////////////////////////////////////////////////////////////

  public void setSelectSchemaButton(JButton selectSchemaButton) {
    m_selectSchemaButton = selectSchemaButton;
    m_selectSchemaButton.addActionListener(new SelectSchemaButton_actionAdapter(this));
  }
  
  public void setXmlSchemaLabel(JLabel xmlSchemaLabel) {
    m_xmlSchemaLabel = xmlSchemaLabel;
    initXmlSchemaLabel();
  }
  
  public void setFileNameField(JTextField schemaFileNameField) {
    m_fileNameField = schemaFileNameField;
  }
  
  public void setSchemaMessageList(TryoutMessageList schemaMessageList) {
    m_schemaMessageList = schemaMessageList;
    m_schemaMessageList.addMouseListener(new EXITryoutFrame_messageList_mouseAdapter(this));
  }
  
  public void setFileTypeLabel(JLabel fileTypeLabel) {
    m_fileTypeLabel = fileTypeLabel;
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
    StringBuilder buf = new StringBuilder();
    int[] indices;
    if ((indices = m_schemaMessageList.getSelectedIndices()) != null) {
      int i;
      for (i = 0; i < indices.length; i++) {
        EXISchemaFactoryThread.AnnotException annotException = (EXISchemaFactoryThread.AnnotException)m_schemaMessageList.getModel().getElementAt(indices[i]);
        buf.append(annotException.toString());
        buf.append('\n');
      }
    }

    StringSelection contents = new StringSelection(buf.toString());

    Clipboard clipboard = getToolkit().getSystemClipboard();
    clipboard.setContents(contents, this);
  }

  @Override
  public void selectAll() {
    ListModel<EXISchemaFactoryThread.AnnotException> listModel;
    if ((listModel = m_schemaMessageList.getModel()) != null) {
      int size;
      if ((size = listModel.getSize()) > 0) {
        ListSelectionModel selModel = m_schemaMessageList.getSelectionModel();
        selModel.clearSelection();
        selModel.addSelectionInterval(0, size - 1);
      }
    }
  }

  @Override
  public JLabel getStatusLabel() {
    return m_statusLabel;
  }

  /////////////////////////////////////////////////////////////////////////
  // UI event adapters
  /////////////////////////////////////////////////////////////////////////

  private class SelectSchemaButton_actionAdapter implements java.awt.event.ActionListener {
    SchemaPage adaptee;

    SelectSchemaButton_actionAdapter(SchemaPage adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.selectFileButton_actionPerformed(e);
    }
  }

  private class EXITryoutFrame_messageList_mouseAdapter extends java.awt.event.MouseAdapter {
    SchemaPage adaptee;

    EXITryoutFrame_messageList_mouseAdapter(SchemaPage adaptee) {
      this.adaptee = adaptee;
    }
    public void mouseClicked(MouseEvent e) {
      if (e.getClickCount() != 2)
        return;
      adaptee.messageListDoubleClicked(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // UI event semantics
  /////////////////////////////////////////////////////////////////////////

  private void selectFileButton_actionPerformed(ActionEvent e) {
    final JFileChooser fileChooser = m_mainFrame.fileChooser;
    if (JFileChooser.APPROVE_OPTION == m_mainFrame.fileChooser.showOpenDialog(this)) {
      setStatusText("");
      m_document.discardEXISchema();      
      final URI uri = fileChooser.getSelectedFile().toURI();
      if (m_schemaMessageList.getModel() != null &&
          m_schemaMessageList.getModel() instanceof EXISchemaFactoryErrorListModel) {
        EXISchemaFactoryErrorListModel messageListModel =
            (EXISchemaFactoryErrorListModel)m_schemaMessageList.getModel();
        if (!uri.equals(messageListModel.getSystemId())) {
          messageListModel.clear();
          messageListModel.setSystemId(uri.toString());
        }
      }
      m_document.probeFile(uri, m_mainFrame.xmlReader);
    }
  }
  
  public void messageListDoubleClicked(MouseEvent e) {
    
    int index = m_schemaMessageList.locationToIndex(e.getPoint());
    final EXISchemaFactoryThread.AnnotException exception;
    exception = (EXISchemaFactoryThread.AnnotException)m_schemaMessageList.getModel().getElementAt(index);
    Locator locator = exception.getLocator();

    int excType = exception.getSeverity();
    if (locator != null && locator.getSystemId() != null && locator.getSystemId().length() != 0) {
      String systemId = locator.getSystemId();
      String name = systemId.substring(systemId.lastIndexOf('/') + 1,
                                       systemId.length());
      int mcode;
      switch (excType) {
        case EXISchemaFactoryThread.AnnotException.EXC_CLASS_WARNING:
          mcode = SchemaCompilerXMsg.MF_STATUS_WARNING_POSITION;
          break;
        case EXISchemaFactoryThread.AnnotException.EXC_CLASS_ERROR:
          mcode = SchemaCompilerXMsg.MF_STATUS_ERROR_POSITION;
          break;
        case EXISchemaFactoryThread.AnnotException.EXC_CLASS_FATAL_ERROR:
          mcode = SchemaCompilerXMsg.MF_STATUS_FATAL_ERROR_POSITION;
          break;
        default:
          mcode = -1;
          break;
      }
      setStatusText(m_mainFrame.getMessageResolver().getMessage(mcode, new String[] {
                            name, String.valueOf(locator.getLineNumber()) }));
      m_mainFrame.openTextFrame(locator);
    }
    else
      setStatusText(m_mainFrame.getMessageResolver().getMessage(SchemaCompilerXMsg.
                            MF_STATUS_ERROR_POSITION_NOT_AVAIL));
  }

  /////////////////////////////////////////////////////////////////////////
  // IPage method implementation
  /////////////////////////////////////////////////////////////////////////
  
  public ITryoutMainFrame getMainFrame() {
    return m_mainFrame;
  }
  
  /////////////////////////////////////////////////////////////////////////
  // ISchemaPage method implementation
  /////////////////////////////////////////////////////////////////////////
  
  public void updateXMLFileTypeLabel(XMLFileType fileType) {
    m_xmlSchemaLabel.setEnabled(true);
    switch (fileType) {
      case schema:
        m_fileTypeLabel.setVisible(true);
        m_xmlSchemaLabel.setForeground(Color.black);
        m_xmlSchemaLabel.setText("  XML Schema     ");
        break;
      case xml:
      case other:
      case none:
        m_fileTypeLabel.setVisible(false);
        initXmlSchemaLabel();        
        break;
    }
  }
  
  public void showMessage(String message) {
    JOptionPane.showMessageDialog(this, message);
  }
  
  public void setFileName(String fileName) {
    m_fileNameField.setText(fileName);
  }

  public IMessageList getMessageList() {
    return m_schemaMessageList;
  }
   
  /////////////////////////////////////////////////////////////////////////
  // 
  /////////////////////////////////////////////////////////////////////////

  /**
   * Implementing java.awt.datatransfer.ClipboardOwner interface.
   */
  public void lostOwnership(Clipboard clipboard,
                            Transferable contents)  {
    // For now, there is nothing to do.
  }

}
