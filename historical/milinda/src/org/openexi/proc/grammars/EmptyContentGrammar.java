package org.openexi.proc.grammars;

import java.util.ArrayList;

import org.openexi.proc.common.EventTypeList;
import org.openexi.schema.EXISchema;

final class EmptyContentGrammar extends ContentGrammar {

  private static final int[] INITIALS = new int[] { EXISchema.NIL_NODE };
  
  private final ArrayEventTypeList m_eventTypes;
  private final EventCodeTuple m_eventCodes;

  ///////////////////////////////////////////////////////////////////////////
  /// constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  EmptyContentGrammar(GrammarCache cache) {
    super(EXISchema.NIL_NODE, SCHEMA_GRAMMAR_NIL_CONTENT, cache);

    m_eventTypes = new ArrayEventTypeList();

    final ArrayList<AbstractEventType> eventTypeList = new ArrayList<AbstractEventType>();
    eventTypeList.add(new EventTypeSchemaEndElement(0, this, m_eventTypes));

    final EventCodeTupleSink res = new EventCodeTupleSink();
    createEventCodeTuple(eventTypeList, cache.grammarOptions, res, m_eventTypes);

    m_eventCodes = res.eventCodeTuple;
    m_eventTypes.setItems(res.eventTypes); 
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in CommonState
  ///////////////////////////////////////////////////////////////////////////

  @Override
  EventTypeList getNextEventTypes(GrammarState stateVariables) {
    return m_eventTypes;
  }

  @Override
  public EventCodeTuple getNextEventCodes(GrammarState stateVariables) {
    return m_eventCodes;
  }
  
  @Override
  void element(int i, String uri, String name, GrammarState stateVariables) {
    // this method will never be called.
    throw new UnsupportedOperationException();
  }
  
  @Override
  void schemaAttribute(int eventTypeIndex, String uri, String name, GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  void xsitp(int tp, GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  void nillify(GrammarState stateVariables) {
    throw new UnsupportedOperationException();
  }
  
  @Override
  public void chars(GrammarState stateVariables) {
  }

  @Override
  public void undeclaredChars(GrammarState stateVariables) {
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Implementation of abstract methods declared in ContentState
  ///////////////////////////////////////////////////////////////////////////
  
  @Override
  protected void end(GrammarState stateVariables) {
    finish(stateVariables);
  }

  @Override
  int[] getInitials() {
    return INITIALS;
  }
  
  @Override
  final String getContentRegime() {
    return "empty";
  }
  
}