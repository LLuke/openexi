package org.openexi.tryout;

class SchemaQuestionPageDocument extends PageDocument {
  
  private final ITryoutMainFrame m_mainFrame;
  private final ISchemaQuestionPage m_schemaQuestionPage;
  
  private boolean m_withSchema;
  
  public SchemaQuestionPageDocument(ISchemaQuestionPage schemaQuestionPage) {
    m_schemaQuestionPage = schemaQuestionPage;
    m_mainFrame = m_schemaQuestionPage.getMainFrame();
    setWithSchema(true);
  }
  
  /////////////////////////////////////////////////////////////////////////
  // PageDocument implementation
  /////////////////////////////////////////////////////////////////////////
  
  @Override
  public void moveToNext() {
    m_mainFrame.discardEXISchema();
    if (m_withSchema) {
      m_mainFrame.moveToNextPage(ITryoutMainFrame.PAGE_NAME_SCHEMA_SELECTION);
    }
    else {
      m_mainFrame.moveToNextPage(ITryoutMainFrame.PAGE_NAME_GRAMMAR_QUESTION);
    }
  }
 
  @Override
  public void updateMainFrame() {
    m_mainFrame.setLeftArrowEnabled(false);
    m_mainFrame.setRightArrowEnabled(true);
    m_mainFrame.setSaveAsEnabled(false);
  }
  
  @Override
  public void updatePage() {
    m_schemaQuestionPage.setWithSchema(m_withSchema);
  }
  
  public void setWithSchema(boolean withSchema) {
    m_withSchema = withSchema;
  }
  
}
