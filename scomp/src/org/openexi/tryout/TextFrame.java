package org.openexi.tryout;

import java.io.IOException;
import java.net.MalformedURLException;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;

import org.xml.sax.Locator;

import org.openexi.util.MessageResolver;

class TextFrame extends JFrame implements ITextFrame, ClipboardOwner {

  private static final long serialVersionUID = 1271607993786080033L;

  private static final MessageResolver m_msgs =
      new MessageResolver(TextFrame.class);

  private final TextFrameDocument m_document;

  JPanel contentPane;
  BorderLayout borderLayout1 = new BorderLayout();
  JMenuBar jMenuBar1 = new JMenuBar();
  JMenu fileMenu = new JMenu();
  JMenu editMenu = new JMenu();
  JMenuItem selectAllMenuItem = new JMenuItem();
  JMenuItem copyMenuItem = new JMenuItem();
  JMenuItem closeMenuItem = new JMenuItem();
  JPanel mainPane = new JPanel();
  JScrollPane textScrollPane = new JScrollPane();
  JTextArea textArea = new JTextArea();
  BorderLayout borderLayout3 = new BorderLayout();
  JPanel statusPanel = new JPanel();
  JLabel statusLabel = new JLabel();
  BorderLayout borderLayout2 = new BorderLayout();
  JLabel caretPosLabel = new CaretListenerLabel(textArea);
  JToolBar TextToolBar = new JToolBar();
  JButton gotoButton = new JButton();
  JMenuItem gotoMenuItem = new JMenuItem();
  AbstractButton[] gotoBtns = new AbstractButton[] { gotoButton, gotoMenuItem };

  public TextFrame() {
    enableEvents(AWTEvent.WINDOW_EVENT_MASK);
    jbInit();
    m_document = new TextFrameDocument(this);
  }

  void setLocator(Locator locator)
      throws MalformedURLException, IOException {
    String systemId = locator.getSystemId();
    String shortName = systemId.substring(systemId.lastIndexOf('/') + 1,
                                          systemId.length());
    statusLabel.setText(m_msgs.getMessage(
                          TextFrameXMsg.TF_STATUS_OPENING_A_FILE,
                          new String[] { shortName }));
    boolean failed = false;
    try {
      m_document.setLocator(locator);
    }
    catch (MalformedURLException mue) {
      failed = true;
      throw mue;
    }
    catch (IOException ioe) {
      failed = true;
      throw ioe;
    }
    finally {
      enableGotos(!failed && locator.getLineNumber() > 0);
      if (failed) {
        textArea.setText("");
        statusLabel.setText(m_msgs.getMessage(
            TextFrameXMsg.TF_STATUS_FILE_OPEN_FAILED,
            new String[] {shortName}));
      }
      else {
        statusLabel.setText(m_msgs.getMessage(
            TextFrameXMsg.TF_STATUS_OPENED_A_FILE,
            new String[] {shortName}));
      }
    }
  }

  //Component initialization
  private void jbInit() {
    contentPane = (JPanel)this.getContentPane();
    contentPane.setLayout(borderLayout1);
    this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    this.setSize(new Dimension(500, 400));
    this.setTitle("Text Display Window");

    fileMenu.setText("File");
    editMenu.setText("Edit");
    selectAllMenuItem.setText("Select All");
    selectAllMenuItem.addActionListener(new TextFrame_selectAllMenuItem_actionAdapter(this));
    copyMenuItem.setText("Copy");
    copyMenuItem.addActionListener(new TextFrame_copyMenuItem_actionAdapter(this));
    closeMenuItem.setText("Close");
    closeMenuItem.addActionListener(new TextFrame_closeMenuItem_actionAdapter(this));
    statusPanel.setLayout(borderLayout2);
    caretPosLabel.setText(" ");
    statusLabel.setText("");
    statusPanel.setBorder(BorderFactory.createLoweredBevelBorder());
    gotoButton.setIcon(new ImageIcon(TextFrame.class.getResource("goto.png")));
    gotoButton.addActionListener(new TextFrame_gotoButton_actionAdapter(this));
    gotoButton.setToolTipText("Jump to error position");
    gotoMenuItem.setText("Show Error Position");
    gotoMenuItem.addActionListener(new TextFrame_gotoMenuItem_actionAdapter(this));
    statusPanel.add(caretPosLabel,  BorderLayout.EAST);
    statusPanel.add(statusLabel, BorderLayout.CENTER);
    mainPane.setLayout(borderLayout3);
    textArea.setEditable(false);
    textArea.getCaret().setVisible(true);
    jMenuBar1.add(fileMenu);
    jMenuBar1.add(editMenu);
    editMenu.add(copyMenuItem);
    editMenu.addSeparator();
    editMenu.add(gotoMenuItem);
    editMenu.addSeparator();
    editMenu.add(selectAllMenuItem);
    fileMenu.add(closeMenuItem);
    contentPane.add(statusPanel,  BorderLayout.SOUTH);
    contentPane.add(mainPane,  BorderLayout.CENTER);
    mainPane.add(textScrollPane,  BorderLayout.CENTER);
    contentPane.add(TextToolBar, BorderLayout.NORTH);
    TextToolBar.add(gotoButton, null);
    textScrollPane.getViewport().add(textArea, null);
    this.setJMenuBar(jMenuBar1);
  }

  /**
   * Implementing java.awt.datatransfer.ClipboardOwner interface.
   */
  public void lostOwnership(Clipboard clipboard,
                            Transferable contents)  {
    // For now, there is nothing to do.
  }

  // Overridden so we can gracefully close the window.
  protected void processWindowEvent(WindowEvent e) {
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      closeMenuItem_actionPerformed(null);
    }
    super.processWindowEvent(e);
  }

  void closeMenuItem_actionPerformed(ActionEvent e) {
    setVisible(false);
    dispose();
  }

  void gotoButton_actionPerformed(ActionEvent e) {
    Locator locator = m_document.getLocator();
    int line = locator.getLineNumber();
    if (line > 0)
      setSelectLine(line);
  }

  void copyMenuItem_actionPerformed(ActionEvent e) {
    String selectedText = textArea.getSelectedText();

    StringSelection contents = new StringSelection(selectedText);

    Clipboard clipboard = getToolkit().getSystemClipboard();
    clipboard.setContents(contents, this);
  }

  void selectAllMenuItem_actionPerformed(ActionEvent e) {
    textArea.selectAll();
  }

  /////////////////////////////////////////////////////////////////////////
  // ITextFrame implementation
  /////////////////////////////////////////////////////////////////////////

  public MessageResolver getMessageResolver() {
    return m_msgs;
  }

  public void setStatusText(String msg) {
    statusLabel.setText(msg);
  }

  public void setTextAreaText(String text) {
    textArea.setText(text);
  }

  /**
   * Highlight a line in TextArea.
   * @param Line number (one-based)
   */
  public void setSelectLine(int line) {
    int zeroLine = line - 1;
    Caret caret = textArea.getCaret();
    try {
      int startOffset = textArea.getLineStartOffset(zeroLine);
      int endOffset   = textArea.getLineEndOffset(zeroLine);
      caret.setDot(endOffset);
      caret.moveDot(startOffset);
      caret.setSelectionVisible(true);
      caret.setVisible(true);
    }
    catch (BadLocationException ble) {
      ble.printStackTrace();
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // Semantic conveniences
  /////////////////////////////////////////////////////////////////////////

  /**
   * Enables/disables Goto buttons.
   */
  private void enableGotos(boolean enable) {
    for (int i = 0; i < gotoBtns.length; i++)
      gotoBtns[i].setEnabled(enable);
  }

  class TextFrame_closeMenuItem_actionAdapter implements java.awt.event.ActionListener {
    TextFrame adaptee;
  
    TextFrame_closeMenuItem_actionAdapter(TextFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.closeMenuItem_actionPerformed(e);
    }
  }
  
  class TextFrame_gotoButton_actionAdapter implements java.awt.event.ActionListener {
    TextFrame adaptee;
  
    TextFrame_gotoButton_actionAdapter(TextFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.gotoButton_actionPerformed(e);
    }
  }
  
  class TextFrame_gotoMenuItem_actionAdapter implements java.awt.event.ActionListener {
    TextFrame adaptee;
  
    TextFrame_gotoMenuItem_actionAdapter(TextFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.gotoButton_actionPerformed(e);
    }
  }
  
  class TextFrame_copyMenuItem_actionAdapter implements java.awt.event.ActionListener {
    TextFrame adaptee;
  
    TextFrame_copyMenuItem_actionAdapter(TextFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.copyMenuItem_actionPerformed(e);
    }
  }
  
  class TextFrame_selectAllMenuItem_actionAdapter implements java.awt.event.ActionListener {
    TextFrame adaptee;
  
    TextFrame_selectAllMenuItem_actionAdapter(TextFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.selectAllMenuItem_actionPerformed(e);
    }
    
  }
}
