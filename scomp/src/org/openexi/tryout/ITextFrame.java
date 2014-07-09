package org.openexi.tryout;

interface ITextFrame extends ITryoutFrame {

  void setTextAreaText(String text);
  
  void setSelectLine(int line);

  void setStatusText(String msg);

}
