package org.openexi.schema;

import java.util.ArrayList;

/**
 * This class represents a typed list data value that results from
 * simple-type schema validation.
 */
public final class ListTypedValue extends TypedValue {

  private ArrayList<AtomicTypedValue> m_atomicList;

  ListTypedValue(EXISchema corpus) {
    super(corpus);
    m_atomicList = new ArrayList<AtomicTypedValue>();
  }

  @Override
  public boolean isList() {
    return m_type != EXISchema.NIL_NODE;
  }

  public int getAtomicValueCount() {
    return m_atomicList.size();
  }

  void appendAtomicValue(AtomicTypedValue atomicValue) {
    m_atomicList.add(atomicValue);
  }

  public AtomicTypedValue getAtomicValue(int i) {
    return (AtomicTypedValue)m_atomicList.get(i);
  }

  public AtomicTypedValue getLastAtomicValue() {
    final int count = m_atomicList.size();
    return count > 0 ? (AtomicTypedValue)m_atomicList.get(count - 1) :
                       (AtomicTypedValue)null;
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof ListTypedValue) {
      ListTypedValue that = (ListTypedValue)obj;
      if (m_atomicList.size() == that.m_atomicList.size()) {
        for (int i = 0; i < m_atomicList.size(); i++) {
          if (!m_atomicList.get(i).equals(that.m_atomicList.get(i))) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }
  
}
