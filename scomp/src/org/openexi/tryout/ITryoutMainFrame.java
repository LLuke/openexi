package org.openexi.tryout;

import org.openexi.schema.EXISchema;

interface ITryoutMainFrame extends ITryoutFrame {

  public static final String PAGE_NAME_SCHEMA_QUESTION = "SchemaQuestionPage";
  public static final String PAGE_NAME_SCHEMA_SELECTION = "SchemaSelectionPage";
  public static final String PAGE_NAME_GRAMMAR_QUESTION = "GrammarQuestionPage";
  public static final String PAGE_NAME_SAVE_GRAMMAR_TO_FILE = "SaveGrammarToFilePage";

  void setSaveAsEnabled(boolean enabled);
  
  void setLeftArrowEnabled(boolean enabled);

  void setRightArrowEnabled(boolean enabled);
  
  void moveToNextPage(String name);
  
  /**
   * For use by SavePage.
   */
  EXISchema getEXISchema();
  
  void discardEXISchema();
  
  /**
   * For use by SavePage.
   */
  GrammarQuestionPageDocument.GrammarFormat getGrammarFormat();

  IPage getPage();
  
}
