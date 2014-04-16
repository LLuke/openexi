package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;
import org.openexi.schema.EXISchema;

public abstract class EventTypeSchema extends AbstractEventType {
  
  final int index;
  public final int serial;
  
  protected final EXISchema m_schema;
  protected final int m_substance;
  
  protected EventTypeSchema(int nd, String uri, String name, int index, int serial, byte depth, 
    Grammar ownerGrammar, EventTypeList eventTypeList, byte itemType) {
    super(uri, name, depth, ownerGrammar, eventTypeList, itemType);
    this.index = index;
    this.serial = serial;
    m_schema = ownerGrammar.getEXISchema();
    int substance;
    if (nd != EXISchema.NIL_NODE && m_schema.getNodes()[nd] == EXISchema.PARTICLE_NODE)
      substance = m_schema.getTermOfParticle(nd);
    else
      substance = nd;
    m_substance = substance;
  }
  
  abstract EventTypeSchema duplicate(EventTypeList eventTypeList);
  
  /**
   * Returns true if the event type was created for augmentation. 
   */
  abstract boolean isAugmented();

  ///////////////////////////////////////////////////////////////////////////
  // Accessors
  ///////////////////////////////////////////////////////////////////////////

  public final int getSchemaSubstance() {
    return m_substance;
  }

  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventCodeItem interface
  ///////////////////////////////////////////////////////////////////////////

//  /**
//   * Returns the position of this item among its siblings of the parent tuple.
//   */
//  @Override
//  public final int getPosition() {
//    return m_position;
//  }
  
  ///////////////////////////////////////////////////////////////////////////
  // Implementation of EventType interface
  ///////////////////////////////////////////////////////////////////////////

  @Override
  public final boolean isSchemaInformed() {
    return true;
  }

}
