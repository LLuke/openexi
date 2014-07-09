package org.openexi.tryout;

import javax.swing.ListModel;

interface IMessageList {
  
  void setModel(ListModel<EXISchemaFactoryThread.AnnotException> listModel);
  
}
