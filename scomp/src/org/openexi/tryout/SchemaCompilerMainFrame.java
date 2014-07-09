package org.openexi.tryout;

import java.net.URI;
import java.net.URISyntaxException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Stack;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.GridLayout;

import javax.swing.border.BevelBorder;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.Point;
import java.awt.Rectangle;

import org.xml.sax.Locator;
import org.xml.sax.XMLReader;

import org.openexi.schema.EXISchema;
import org.openexi.util.MessageResolver;
import java.awt.FlowLayout;
import java.awt.Color;

public class SchemaCompilerMainFrame extends JFrame implements ITryoutMainFrame {
  
  private static final long serialVersionUID = 2914986880527444182L;

  private static final MessageResolver m_msgs =
      new MessageResolver(SchemaCompiler.class, System.err);
  
  final XMLReader xmlReader = XMLHelper.createXMLReader();
  final JFileChooser fileChooser = new JFileChooser();

  private JMenuItem validateMenuItem;
  
  private JMenuItem saveAsMenuItem;
  
  private JPanel cardPanel;
  private CardLayout cardLayout;
  
  private JButton leftArrowButton;
  private JButton rightArrowButton;

  private final Stack<String> m_pageLocus = new Stack<String>(); 
  private EXITryoutPage m_currentPage;
  
  /** SystemID (String) -> TextFrame */
  private final HashMap<String,TextFrame> m_textFrames = new HashMap<String,TextFrame>();
  private TextFrame m_lastTextFrame = null;
  
  private final SchemaPageDocument m_schemaPageDocument;
  private final GrammarQuestionPageDocument m_grammarQuestionPageDocument;
  
  public SchemaCompilerMainFrame() {
    setSize(new Dimension(600, 480));
    getContentPane().setLayout(new BorderLayout(0, 0));
    this.setTitle("Nagasena");
    
    JPanel controlPanel = new JPanel();
    getContentPane().add(controlPanel, BorderLayout.NORTH);
    controlPanel.setLayout(new BorderLayout(0, 0));
    
//    validateButton = new JButton("");
//    validateButton.setEnabled(false);
//    validateButton.setIcon(new ImageIcon(EXITryoutMainFrame.class.getResource("validate.png")));
//    validateButton.setToolTipText("Process File");
//    validateButton.addActionListener(new EXITryoutMainFrame_validateButton_actionAdapter(this));
//    toolBar.add(validateButton);
    
    JPanel arrowPanel = new JPanel();
    controlPanel.add(arrowPanel, BorderLayout.EAST);
    
    leftArrowButton = new JButton();
    leftArrowButton.setIcon(new ImageIcon(SchemaCompilerMainFrame.class.getResource("LeftArrow_s.png")));
    leftArrowButton.setVisible(true);
    arrowPanel.add(leftArrowButton);
    leftArrowButton.addActionListener(new EXITryoutMainFrame_leftArrowButton_actionAdapter(this));
    
    rightArrowButton = new JButton();
    rightArrowButton.setIcon(new ImageIcon(SchemaCompilerMainFrame.class.getResource("RightArrow_s.png")));
    rightArrowButton.setVisible(true);
    arrowPanel.add(rightArrowButton);
    rightArrowButton.addActionListener(new EXITryoutMainFrame_rightArrowButton_actionAdapter(this));
    
    cardPanel = new JPanel();
    getContentPane().add(cardPanel, BorderLayout.CENTER);
    cardLayout = new CardLayout(0, 0); 
    cardPanel.setLayout(cardLayout);
    
    SchemaQuestionPage schemaQuestionPage = new SchemaQuestionPage(this);
    schemaQuestionPage.setBackground(Color.LIGHT_GRAY);
    cardPanel.add(schemaQuestionPage, PAGE_NAME_SCHEMA_QUESTION);
    schemaQuestionPage.setVisible(false);
    schemaQuestionPage.setLayout(new BorderLayout(0, 0));

    m_pageLocus.push(PAGE_NAME_SCHEMA_QUESTION);
    
    JPanel schemaQuestionPanel_1 = new JPanel();
    schemaQuestionPage.add(schemaQuestionPanel_1, BorderLayout.CENTER);
    schemaQuestionPanel_1.setLayout(new BorderLayout(0, 0));
    
    JSeparator separator_2 = new JSeparator();
    schemaQuestionPanel_1.add(separator_2, BorderLayout.NORTH);
    
    JPanel schemaQuestionPanel_2 = new JPanel();
    schemaQuestionPanel_1.add(schemaQuestionPanel_2);
    schemaQuestionPanel_2.setLayout(new BoxLayout(schemaQuestionPanel_2, BoxLayout.X_AXIS));
    
    JPanel panel_6 = new JPanel();
    schemaQuestionPanel_2.add(panel_6);
    panel_6.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
    
    JPanel schemaQuestionPanel = new JPanel();
    schemaQuestionPanel_2.add(schemaQuestionPanel);
    schemaQuestionPanel.setLayout(new BoxLayout(schemaQuestionPanel, BoxLayout.Y_AXIS));
    
    JLabel haveSchemaQuestionLabel = new JLabel("Do you have an XML Schema for use with EXI?");
    schemaQuestionPanel.add(haveSchemaQuestionLabel);
    
    JLabel label = new JLabel(" ");
    schemaQuestionPanel.add(label);
    
    JRadioButton withSchemaRadioButton = new JRadioButton("Yes. I have an XML Schema.");
    schemaQuestionPanel.add(withSchemaRadioButton);
    schemaQuestionPage.setWithSchemaRadioButton(withSchemaRadioButton);
    
    JRadioButton noSchemaRadioButton = new JRadioButton("No. I do not have an XML Schema.");
    schemaQuestionPanel.add(noSchemaRadioButton);
    schemaQuestionPage.setNoSchemaRadioButton(noSchemaRadioButton);
    
    ButtonGroup haveSchemaQuestionButtonGroup = new ButtonGroup();
    haveSchemaQuestionButtonGroup.add(withSchemaRadioButton);
    haveSchemaQuestionButtonGroup.add(noSchemaRadioButton);
    
    JLabel label_1 = new JLabel("     \n     \n     \n");
    label_1.setMaximumSize(new Dimension(45, 100));
    label_1.setMinimumSize(new Dimension(45, 100));
    schemaQuestionPanel.add(label_1);
    
    JPanel panel_9 = new JPanel();
    schemaQuestionPanel_2.add(panel_9);
    
    JSeparator separator_1 = new JSeparator();
    schemaQuestionPanel_1.add(separator_1, BorderLayout.SOUTH);
    
    JPanel statusPanel_1 = new JPanel();
    schemaQuestionPage.add(statusPanel_1, BorderLayout.SOUTH);
    statusPanel_1.setLayout(new BorderLayout(0, 0));
    
    JLabel statusLabel_1 = new JLabel("");
    statusPanel_1.add(statusLabel_1);
    schemaQuestionPage.setStatusLabel(statusLabel_1);

    JLabel statusPlaceHolder_1 = new JLabel(" ");
    statusPanel_1.add(statusPlaceHolder_1, BorderLayout.EAST);
    
    schemaQuestionPage.initPage();

    // Schema Page
    SchemaPage schemaPage = new SchemaPage(this);
    schemaPage.setBackground(Color.LIGHT_GRAY);
    schemaPage.setVisible(false);
    
    m_schemaPageDocument = (SchemaPageDocument)schemaPage.getPageDocument();

    cardPanel.add(schemaPage, PAGE_NAME_SCHEMA_SELECTION);
    schemaPage.setLayout(new BorderLayout(0, 0));
    
    JPanel schemaFileSpecPane = new JPanel();
    schemaPage.add(schemaFileSpecPane, BorderLayout.NORTH);
    schemaFileSpecPane.setLayout(new BorderLayout(0, 0));
    
    JPanel schemaFileSelectionPane = new JPanel();
    schemaFileSpecPane.add(schemaFileSelectionPane, BorderLayout.NORTH);
    schemaFileSelectionPane.setLayout(new BorderLayout(0, 0));
    
    JTextField schemaFileNameField = new JTextField();
    schemaFileNameField.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
    schemaFileNameField.setEditable(false);
    schemaFileSelectionPane.add(schemaFileNameField);
    schemaPage.setFileNameField(schemaFileNameField);
    
    JButton selectSchemaButton = new JButton();
    schemaFileSelectionPane.add(selectSchemaButton, BorderLayout.EAST);
    selectSchemaButton.setBorder(BorderFactory.createRaisedBevelBorder());
    selectSchemaButton.setBorderPainted(true);
    selectSchemaButton.setIcon(null);
    selectSchemaButton.setMnemonic('0');
    selectSchemaButton.setText(" ... ");
    schemaPage.setSelectSchemaButton(selectSchemaButton);
    
    JPanel panel_4 = new JPanel();
    schemaFileSpecPane.add(panel_4, BorderLayout.SOUTH);
    panel_4.setLayout(new BorderLayout(0, 0));
    
    JLabel fileTypeLabel = new JLabel("File Type:");
    fileTypeLabel.setEnabled(true);
    fileTypeLabel.setVisible(false);
    fileTypeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    panel_4.add(fileTypeLabel, BorderLayout.CENTER);
    schemaPage.setFileTypeLabel(fileTypeLabel);
    
    JLabel xmlSchemaLabel = new JLabel();
    xmlSchemaLabel.setEnabled(true);
    panel_4.add(xmlSchemaLabel, BorderLayout.EAST);
    schemaPage.setXmlSchemaLabel(xmlSchemaLabel);
    
    JScrollPane scrollPane = new JScrollPane();
    scrollPane.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
    schemaPage.add(scrollPane, BorderLayout.CENTER);
    
    TryoutMessageList schemaMessageList = new TryoutMessageList();
    schemaMessageList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    schemaMessageList.setBorder(BorderFactory.createEtchedBorder());
    scrollPane.setViewportView(schemaMessageList);
    schemaPage.setSchemaMessageList(schemaMessageList);
    
    JPanel statusPanel = new JPanel();
    schemaPage.add(statusPanel, BorderLayout.SOUTH);
    statusPanel.setLayout(new BorderLayout(0, 0));
    
    JLabel statusLabel = new JLabel("");
    statusPanel.add(statusLabel, BorderLayout.CENTER);
    schemaPage.setStatusLabel(statusLabel);
    
    JLabel statusPlaceHolder = new JLabel(" ");
    statusPanel.add(statusPlaceHolder, BorderLayout.EAST);

    schemaPage.initPage();

    // Grammar Question Page

    GrammarQuestionPage compressionQuestionPage = new GrammarQuestionPage(this);
    compressionQuestionPage.setBackground(Color.LIGHT_GRAY);
    compressionQuestionPage.setVisible(false);
    cardPanel.add(compressionQuestionPage, PAGE_NAME_GRAMMAR_QUESTION);
    compressionQuestionPage.setLayout(new BorderLayout(0, 0));

    m_grammarQuestionPageDocument = (GrammarQuestionPageDocument)compressionQuestionPage.getPageDocument();
    
    JPanel compressionQuestionPanel_1 = new JPanel();
    compressionQuestionPage.add(compressionQuestionPanel_1, BorderLayout.CENTER);
    compressionQuestionPanel_1.setLayout(new BorderLayout(0, 0));
    
    JSeparator separator_3 = new JSeparator();
    compressionQuestionPanel_1.add(separator_3, BorderLayout.NORTH);
    
    JPanel compressionQuestionPanel_2 = new JPanel();
    compressionQuestionPanel_1.add(compressionQuestionPanel_2, BorderLayout.CENTER);
    compressionQuestionPanel_2.setLayout(new BoxLayout(compressionQuestionPanel_2, BoxLayout.X_AXIS));
    
    JPanel panel = new JPanel();
    compressionQuestionPanel_2.add(panel);
    
    JPanel exiQuestionPanel = new JPanel();
    compressionQuestionPanel_2.add(exiQuestionPanel);
    exiQuestionPanel.setLayout(new BoxLayout(exiQuestionPanel, BoxLayout.Y_AXIS));

    JLabel needCompressionQuestionLabel = new JLabel("Which format do you want to use for saving the EXI grammar?");
    needCompressionQuestionLabel.setAlignmentX(0.4f);
    exiQuestionPanel.add(needCompressionQuestionLabel);
    
    JLabel label_2 = new JLabel(" ");
    exiQuestionPanel.add(label_2);
    
    JRadioButton withEXIRadioButton = new JRadioButton("EXI");
    exiQuestionPanel.add(withEXIRadioButton);
    compressionQuestionPage.setWithEXIRadioButton(withEXIRadioButton);
    
    JRadioButton noEXIRadioButton = new JRadioButton("XML");
    exiQuestionPanel.add(noEXIRadioButton);
    compressionQuestionPage.setNoEXIRadioButton(noEXIRadioButton);
    
    ButtonGroup compressionQuestionButtonGroup = new ButtonGroup();
    compressionQuestionButtonGroup.add(withEXIRadioButton);
    compressionQuestionButtonGroup.add(noEXIRadioButton);

    JLabel label_3 = new JLabel("");
    label_3.setMinimumSize(new Dimension(45, 100));
    label_3.setMaximumSize(new Dimension(45, 100));
    exiQuestionPanel.add(label_3);

    JPanel panel_2 = new JPanel();
    compressionQuestionPanel_2.add(panel_2);
    
    JSeparator separator_4 = new JSeparator();
    compressionQuestionPanel_1.add(separator_4, BorderLayout.SOUTH);

    JPanel statusPanel_3 = new JPanel();
    compressionQuestionPage.add(statusPanel_3, BorderLayout.SOUTH);
    statusPanel_3.setLayout(new BorderLayout(0, 0));

    JLabel statusLabel_3 = new JLabel("");
    statusPanel_3.add(statusLabel_3);
    compressionQuestionPage.setStatusLabel(statusLabel_3);

    JLabel statusPlaceHolder_3 = new JLabel(" ");
    statusPanel_3.add(statusPlaceHolder_3, BorderLayout.EAST);
    
    compressionQuestionPage.initPage();

    // Grammar Save Page
    XmlPage xmlSelectionPage = new XmlPage(this);
    xmlSelectionPage.setVisible(false);
    
    cardPanel.add(xmlSelectionPage, PAGE_NAME_SAVE_GRAMMAR_TO_FILE);
    xmlSelectionPage.setLayout(new BorderLayout(0, 0));
    
    JPanel savePanel_1 = new JPanel();
    xmlSelectionPage.add(savePanel_1, BorderLayout.CENTER);
    savePanel_1.setLayout(new BorderLayout(0, 0));
    
    JSeparator separator_5 = new JSeparator();
    savePanel_1.add(separator_5, BorderLayout.NORTH);
    
    JPanel savePane_2 = new JPanel();
    savePanel_1.add(savePane_2, BorderLayout.CENTER);
    savePane_2.setLayout(new GridLayout(3, 3));
    
    savePane_2.add(new JLabel(" "));
    savePane_2.add(new JLabel(" "));
    savePane_2.add(new JLabel(" "));

    savePane_2.add(new JLabel(" "));
    JTextArea saveLabel = new JTextArea();
    saveLabel.setLineWrap(true);
    saveLabel.setWrapStyleWord(true);
    saveLabel.setEditable(false);
    saveLabel.setCursor(null);
    saveLabel.setOpaque(false);  
    saveLabel.setFocusable(false);  
    saveLabel.setFont(UIManager.getFont("Label.font"));      
    saveLabel.setText("Please save the generated EXI grammar from File menu.");
    savePane_2.add(saveLabel);
    savePane_2.add(new JLabel(" "));
    
    savePane_2.add(new JLabel(" "));
    savePane_2.add(new JLabel(" "));
    savePane_2.add(new JLabel(" "));
    
    JSeparator separator_6 = new JSeparator();
    savePanel_1.add(separator_6, BorderLayout.SOUTH);
    
    JPanel statusPanel_2 = new JPanel();
    xmlSelectionPage.add(statusPanel_2, BorderLayout.SOUTH);
    statusPanel_2.setLayout(new BorderLayout(0, 0));
    
    JLabel statusLabel_2 = new JLabel("");
    statusPanel_2.add(statusLabel_2, BorderLayout.CENTER);
    xmlSelectionPage.setStatusLabel(statusLabel_2);
    
    JLabel statusPlaceHolder_2 = new JLabel(" ");
    statusPanel_2.add(statusPlaceHolder_2, BorderLayout.EAST);
    
    xmlSelectionPage.initPage();
    
    JMenuBar menuBar = new JMenuBar();
    setJMenuBar(menuBar);
    
    JMenu mnNewMenu = new JMenu("File");
    menuBar.add(mnNewMenu);
    
    validateMenuItem = new JMenuItem("Process");
    validateMenuItem.setEnabled(false);
    mnNewMenu.add(validateMenuItem);
    
    saveAsMenuItem = new JMenuItem("Save as...");
    saveAsMenuItem.addActionListener(new EXITryoutMainFrame_saveAsMenuItem_ActionAdapter(this));
    mnNewMenu.add(saveAsMenuItem);

    JMenuItem jMenuFileExit = new JMenuItem("Exit");
    jMenuFileExit.addActionListener(new EXITryoutMainFrame_jMenuFileExit_ActionAdapter(this));
    mnNewMenu.add(jMenuFileExit);
    
    JMenu mnEdit = new JMenu("Edit");
    menuBar.add(mnEdit);
    
    JMenuItem jMenuEditCopy = new JMenuItem("Copy");
    jMenuEditCopy.addActionListener(new EXITryoutMainFrame_jMenuEditCopy_actionAdapter(this));
    mnEdit.add(jMenuEditCopy);
    
    JSeparator separator = new JSeparator();
    mnEdit.add(separator);
    
    JMenuItem jMenuSelectAll = new JMenuItem("Select All");
    jMenuSelectAll.addActionListener(new EXITryoutMainFrame_jMenuSelectAll_actionAdapter(this));
    mnEdit.add(jMenuSelectAll);
    
    JMenu mnHelp = new JMenu("Help");
    menuBar.add(mnHelp);
    
    JMenuItem jMenuHelpAbout = new JMenuItem("About");
    jMenuHelpAbout.addActionListener(new EXITryoutMainFrame_jMenuHelpAbout_ActionAdapter(this));
    mnHelp.add(jMenuHelpAbout);
    
    switchPage(PAGE_NAME_SCHEMA_QUESTION);
//    switchPage(PAGE_NAME_XML_SELECTION);
    
  }

  public void setPage(EXITryoutPage page) {
    m_currentPage = page;
  }

  public IPage getPage() {
    return m_currentPage;
  }
  
  /////////////////////////////////////////////////////////////////////////
  // ITryoutFrame method implementation
  /////////////////////////////////////////////////////////////////////////

  public MessageResolver getMessageResolver() {
    return m_msgs;
  }

  /////////////////////////////////////////////////////////////////////////
  // ITryoutMainFrame implementation
  /////////////////////////////////////////////////////////////////////////

//  public void setValidationEnabled(boolean enabled) {
//    validateButton.setEnabled(enabled);
//    validateMenuItem.setEnabled(enabled);
//  }
  
  public void setSaveAsEnabled(boolean enabled) {
    if (saveAsMenuItem != null)
      saveAsMenuItem.setEnabled(enabled);
  }

  public void setLeftArrowEnabled(boolean enabled) {
    leftArrowButton.setEnabled(enabled);
  }

  public void setRightArrowEnabled(boolean enabled) {
    rightArrowButton.setEnabled(enabled);
  }
  
  public void moveToNextPage(String name) {
    m_pageLocus.push(name);
    switchPage(name);
  }

  public EXISchema getEXISchema() {
    return m_schemaPageDocument.getSchema();
  }
  
  public void discardEXISchema() {
    m_schemaPageDocument.discardEXISchema();
  }
  
  public GrammarQuestionPageDocument.GrammarFormat getGrammarFormat() {
    return m_grammarQuestionPageDocument.getGrammarFormat();
  }

  /////////////////////////////////////////////////////////////////////////
  // Semantic conveniences
  /////////////////////////////////////////////////////////////////////////

  /**
   * Intern a TextFrame by its systemID
   */
  private TextFrame internTextFrame(String systemId)
      throws URISyntaxException {
    TextFrame textFrame = null;
    if (systemId != null && systemId.length() != 0) {
      systemId = new URI(systemId).toString(); // canonicalize URI
      synchronized (m_textFrames) {
        if ( (textFrame = (TextFrame) m_textFrames.get(systemId)) == null) {
          textFrame = new TextFrame();
          m_textFrames.put(systemId, textFrame);
          textFrame.validate();
        }
      }
    }
    return textFrame;
  }

  /**
   * Open a TextFrame given a Locator.
   */
  void openTextFrame(Locator locator) {
    try {
      TextFrame textFrame;
      if ((textFrame = internTextFrame(locator.getSystemId())) != null) {
        Rectangle rec1, rec2;
        Point loc;
        if (m_lastTextFrame != null) {
          rec1 = m_lastTextFrame.getBounds();
          rec2 = m_lastTextFrame.getContentPane().getBounds();
          loc = m_lastTextFrame.getLocation();
        }
        else {
          rec1 = getBounds();
          rec2 = getContentPane().getBounds();
          loc = getLocation();
        }
        if (textFrame != m_lastTextFrame)
          textFrame.setLocation(
              (int)(loc.getX() + (rec1.getHeight() - rec2.getHeight()) / 2),
              (int)(loc.getY() + (rec1.getHeight() - rec2.getHeight()) / 2));
        textFrame.setVisible(true);
        textFrame.setLocator(locator);
        m_lastTextFrame = textFrame;
      }
    }
    catch (URISyntaxException mue) {
      m_currentPage.getStatusLabel().setText(m_msgs.getMessage(SchemaCompilerXMsg.MF_STATUS_NOT_A_VALID_URI,
                                            new String[] { locator.getSystemId() }));
    }
    catch (IOException ioe) {
      m_currentPage.getStatusLabel().setText(m_msgs.getMessage(SchemaCompilerXMsg.MF_STATUS_IO_ERROR_FILE_READ,
                                            new String[] { locator.getSystemId() }));
    }
  }

  public void switchPage(String pageName) {
    cardLayout.show(cardPanel, pageName);
  }
  
  /////////////////////////////////////////////////////////////////////////
  // Adapters
  /////////////////////////////////////////////////////////////////////////

  class EXITryoutMainFrame_saveAsMenuItem_ActionAdapter implements ActionListener {
    SchemaCompilerMainFrame adaptee;

    EXITryoutMainFrame_saveAsMenuItem_ActionAdapter(SchemaCompilerMainFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.saveAsMenuItem_actionPerformed(e);
    }
  }
  
  class EXITryoutMainFrame_jMenuFileExit_ActionAdapter implements ActionListener {
    SchemaCompilerMainFrame adaptee;

    EXITryoutMainFrame_jMenuFileExit_ActionAdapter(SchemaCompilerMainFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.jMenuFileExit_actionPerformed(e);
    }
  }

//  class EXITryoutMainFrame_validateButton_actionAdapter implements java.awt.event.ActionListener {
//    EXITryoutMainFrame adaptee;
//
//    EXITryoutMainFrame_validateButton_actionAdapter(EXITryoutMainFrame adaptee) {
//      this.adaptee = adaptee;
//    }
//    public void actionPerformed(ActionEvent e) {
//      adaptee.validate_actionPerformed(e);
//    }
//  }
  
  class EXITryoutMainFrame_leftArrowButton_actionAdapter implements java.awt.event.ActionListener {
    SchemaCompilerMainFrame adaptee;

    EXITryoutMainFrame_leftArrowButton_actionAdapter(SchemaCompilerMainFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      leftArrow_actionPerformed(e);      
    }
  }
  
  class EXITryoutMainFrame_rightArrowButton_actionAdapter implements java.awt.event.ActionListener {
    SchemaCompilerMainFrame adaptee;

    EXITryoutMainFrame_rightArrowButton_actionAdapter(SchemaCompilerMainFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      rightArrow_actionPerformed(e);      
    }
  }
  
  class  EXITryoutMainFrame_jMenuEditCopy_actionAdapter implements java.awt.event.ActionListener {
    SchemaCompilerMainFrame adaptee;

    EXITryoutMainFrame_jMenuEditCopy_actionAdapter(SchemaCompilerMainFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.jMenuEditCopy_actionPerformed(e);
    }
  }

  class EXITryoutMainFrame_jMenuSelectAll_actionAdapter implements java.awt.event.ActionListener {
    SchemaCompilerMainFrame adaptee;

    EXITryoutMainFrame_jMenuSelectAll_actionAdapter(SchemaCompilerMainFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.jMenuSelectAll_actionPerformed(e);
    }
  }

  class EXITryoutMainFrame_jMenuHelpAbout_ActionAdapter implements ActionListener {
    SchemaCompilerMainFrame adaptee;

    EXITryoutMainFrame_jMenuHelpAbout_ActionAdapter(SchemaCompilerMainFrame adaptee) {
      this.adaptee = adaptee;
    }
    public void actionPerformed(ActionEvent e) {
      adaptee.jMenuHelpAbout_actionPerformed(e);
    }
  }

  /////////////////////////////////////////////////////////////////////////
  // Event semantics
  /////////////////////////////////////////////////////////////////////////

  private void jMenuEditCopy_actionPerformed(ActionEvent e) {
    m_currentPage.copy();
  }

  private void jMenuSelectAll_actionPerformed(ActionEvent e) {
    m_currentPage.selectAll();
  }

//  private void validate_actionPerformed(ActionEvent e) {
//    setValidationEnabled(false);
//    m_currentPage.validateFile();
//  }

  private void leftArrow_actionPerformed(ActionEvent e) {
    final String currentPageName = (String)m_pageLocus.pop();
    String prevPageName = (String)m_pageLocus.peek();
    if (PAGE_NAME_SAVE_GRAMMAR_TO_FILE.equals(currentPageName)) {
//      assert m_schema != null;
    }
    else if (PAGE_NAME_GRAMMAR_QUESTION.equals(currentPageName)) {
//      assert m_schema != null;
//      m_schema = null;
    }
    else {
//      assert m_schema == null;
    }
    switchPage(prevPageName);
  }

  private void rightArrow_actionPerformed(ActionEvent e) {
    m_currentPage.getPageDocument().moveToNext();
  }

  private void saveAsMenuItem_actionPerformed(ActionEvent e) {
    m_currentPage.saveAs();
  }
  
  // File | Exit action performed
  private void jMenuFileExit_actionPerformed(ActionEvent e) {
    System.exit(0);
  }

  // Help | About action performed
  private void jMenuHelpAbout_actionPerformed(ActionEvent e) {
    EXITryoutMainFrame_AboutBox dlg = new EXITryoutMainFrame_AboutBox(this);
    Dimension dlgSize = dlg.getPreferredSize();
    Dimension frmSize = getSize();
    Point loc = getLocation();
    dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
    dlg.setModal(true);
    dlg.pack();
    dlg.setVisible(true);
  }

  @Override
  protected void processWindowEvent(WindowEvent e) {
    super.processWindowEvent(e);
    if (e.getID() == WindowEvent.WINDOW_CLOSING) {
      jMenuFileExit_actionPerformed(null);
    }
  }


}
