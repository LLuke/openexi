package org.openexi.proc.grammars;

import org.openexi.proc.common.EventTypeList;
import org.openexi.schema.EXISchema;

abstract class EventTypeSchemaAttributeWildcardAny extends EventTypeSchemaAttributeWildcard {

  private EventTypeSchemaAttributeWildcardAny(int wc, int serial, byte depth, 
    Grammar ownerGrammar, EventTypeList eventTypeList) {
    super(wc, (String)null, serial, depth, ownerGrammar, eventTypeList, ITEM_SCHEMA_AT_WC_ANY);
  }
  
//  @Override
//  public final byte getItemType() {
//    return ITEM_SCHEMA_AT_WC_ANY;
//  }
  
  static final EventTypeSchemaAttributeWildcardAny createLevelOne(int wc, int serial, 
      Grammar ownerGrammar, EventTypeList eventTypeList){
    return new EventTypeSchemaAttributeWildcardAny(wc, serial, 
        EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList) {
      @Override
      final EventTypeSchema duplicate(EventTypeList eventTypeList) {
        return createLevelOne(m_substance, serial, m_ownerGrammar, eventTypeList);
      }
      @Override
      final boolean isAugmented() {
        return false;
      }
    };
  }

  static final EventTypeSchemaAttributeWildcardAny createLevelOne( 
      Grammar ownerGrammar, EventTypeList eventTypeList){
    return new EventTypeSchemaAttributeWildcardAny(EXISchema.NIL_NODE, NIL_INDEX, 
        EVENT_CODE_DEPTH_ONE, ownerGrammar, eventTypeList) {
      @Override
      final EventTypeSchema duplicate(EventTypeList eventTypeList) {
        // will never be called.
        throw new UnsupportedOperationException();
      }
      @Override
      final boolean isAugmented() {
        return false;
      }
    };
  }

  static final EventTypeSchemaAttributeWildcardAny createLevelTwo(
      Grammar ownerGrammar, EventTypeList eventTypeList){
    return new EventTypeSchemaAttributeWildcardAny(EXISchema.NIL_NODE, NIL_INDEX,
        EVENT_CODE_DEPTH_TWO, ownerGrammar, eventTypeList) {
      @Override
      final EventTypeSchema duplicate(EventTypeList eventTypeList) {
        // will never be called because it is an augmentation.
        throw new UnsupportedOperationException();
      }
      @Override
      final boolean isAugmented() {
        return true;
      }
    };
  }

  
}
