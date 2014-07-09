package org.openexi.tryout;

interface ISchemaPage extends IPage {
  
  void updateXMLFileTypeLabel(XMLFileType fileType);

  IMessageList getMessageList();
  
  void resetPage();
  
  void showMessage(String message);
  
  void setFileName(String fileName);
  
}
