package org.openexi.proc.grammars;

import org.openexi.proc.common.EventCode;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.EventTypeList;
import org.openexi.proc.common.EXIEvent;

abstract class AbstractEventType extends EventType {

  protected final byte m_depth;
  private final EventCode[] m_path;
  protected final String m_uri;
  protected final String m_name;
  protected final Grammar m_ownerGrammar;

  /**
   * The index of this event type in the EventTypeList to which it belongs.
   */
  private int m_index; 

  private final EventTypeList m_ownerList;

  protected AbstractEventType(String uri, String name, byte depth, 
    Grammar ownerGrammar, EventTypeList eventTypeList, byte itemType) {
    super(itemType);
    m_depth = depth;
    m_path = new EventCode[depth];
    m_uri = uri != null ? uri : "";
    m_name = name;
    m_ownerGrammar = ownerGrammar;
    m_index = NIL_INDEX;
    m_ownerList = eventTypeList;
  }

  final Grammar getGrammar() {
    return m_ownerGrammar;
  }

  ///////////////////////////////////////////////////////////////////////////
  // EventType API implementation
  ///////////////////////////////////////////////////////////////////////////

  public final int getDepth() {
    return m_depth;
  }
  
  public final EventCode[] getItemPath() {
    return m_path;
  }
  
  public final String getURI() {
    return m_uri;
  }
  
  public final String getName() {
    return m_name;
  }
  
  public abstract boolean isSchemaInformed();
  
  public final int getIndex() {
    assert m_index != NIL_INDEX;
    return m_ownerList.isMutable() ? m_ownerList.getLength() - (m_index + 1) : m_index;
  }

  public final EventTypeList getEventTypeList() {
    return m_ownerList;
  }
  
  public EXIEvent asEXIEvent() {
    return null;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Own API
  ///////////////////////////////////////////////////////////////////////////
  
  abstract boolean isContent();

  ///////////////////////////////////////////////////////////////////////////
  // Other APIs
  ///////////////////////////////////////////////////////////////////////////
  
  final void computeItemPath() {
    int depth = getDepth();
    int i;
    EventCode item;
    for (i = 0, item = this; i < depth; i++) {
      m_path[(depth - 1) - i] = item;
      item = item.parent;
    }
  }
  
  void setIndex(int index) {
    m_index = index;
  }

}
