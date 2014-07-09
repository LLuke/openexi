package org.openexi.tryout;

class GrammarQuestionPageDocument extends PageDocument {

  private final ITryoutMainFrame m_mainFrame;
  private final IGrammarQuestionPage m_grammarQuestionPage;
  
  private GrammarFormat m_grammarFormat;

  public GrammarQuestionPageDocument(IGrammarQuestionPage grammarQuestionPage) {
    m_grammarQuestionPage = grammarQuestionPage;
    m_mainFrame = m_grammarQuestionPage.getMainFrame();
    setGrammarFormat(GrammarFormat.exi);
  }
  
  public static enum GrammarFormat {
    exi,
    xml
  }
  
  public GrammarFormat getGrammarFormat() {
    return m_grammarFormat;
  }
  
  /////////////////////////////////////////////////////////////////////////
  // PageDocument implementation
  /////////////////////////////////////////////////////////////////////////
  
  @Override
  public void moveToNext() {
    m_mainFrame.moveToNextPage(ITryoutMainFrame.PAGE_NAME_SAVE_GRAMMAR_TO_FILE);
  }

  @Override
  public void updateMainFrame() {
    m_mainFrame.setLeftArrowEnabled(true);
    m_mainFrame.setRightArrowEnabled(true);
    m_mainFrame.setSaveAsEnabled(false);
  }

  @Override
  public void updatePage() {
    m_grammarQuestionPage.setGrammarFormat(m_grammarFormat);
  }
  
  public void setGrammarFormat(GrammarFormat grammarFormat) {
    m_grammarFormat = grammarFormat;
  }

}
