package org.openexi.fujitsu.schema;

/**
 */
public final class SimpleTypeValidationInfo {

  private ListTypedValue m_listValue;

  public SimpleTypeValidationInfo() {
    m_listValue = null;
  }

  void setListTypedValue(ListTypedValue listValue) {
    m_listValue = listValue;
  }

  public TypedValue getTypedValue() {
    if (m_listValue != null) {
      if (m_listValue.isList()) {
        return m_listValue;
      }
      else {
        if (m_listValue.getAtomicValueCount() > 0) {
          assert m_listValue.getAtomicValueCount() == 1;
          return m_listValue.getAtomicValue(0);
        }
      }
    }
    return null;
  }

}
