package org.openexi.fujitsu.proc.grammars;

import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.openexi.fujitsu.proc.common.EventTypeList;
import org.openexi.fujitsu.schema.EXISchema;

/**
 * CommonState is common base of those classes that represent
 * grammars such as documents, elements, groups and so on.
 */
public abstract class Grammar {

  /** -1 is reserved state. Do not use -1 as a state value in subclasses. */
  protected static final int COMMON_STATE_ABSENT = -1;

  public    static final byte BUILTIN_GRAMMAR_ELEMENT                = 0;
  public    static final byte BUILTIN_GRAMMAR_FRAGMENT               = 1;

  private   static final int  SCHEMA_GRAMMAR_MASK                    = 0x40;
  protected static final byte SCHEMA_GRAMMAR_DOCUMENT                = SCHEMA_GRAMMAR_MASK + 0;
  public    static final byte SCHEMA_GRAMMAR_FRAGMENT                = SCHEMA_GRAMMAR_MASK + 1;
  public    static final byte SCHEMA_GRAMMAR_ELEMENT_FRAGMENT        = SCHEMA_GRAMMAR_MASK + 2;
  public    static final byte SCHEMA_GRAMMAR_ELEMENT                 = SCHEMA_GRAMMAR_MASK + 3;
  public    static final byte SCHEMA_GRAMMAR_ELEMENT_TAG             = SCHEMA_GRAMMAR_MASK + 4;
  public    static final byte SCHEMA_GRAMMAR_ELEMENT_CONTENT         = SCHEMA_GRAMMAR_MASK + 5;
  public    static final byte SCHEMA_GRAMMAR_NIL_CONTENT             = SCHEMA_GRAMMAR_MASK + 6;
  protected static final byte SCHEMA_GRAMMAR_GROUP                   = SCHEMA_GRAMMAR_MASK + 7;

  private static final byte DOCUMENT_STATE_BASE = 0;
  protected static final byte DOCUMENT_STATE_CREATED     = DOCUMENT_STATE_BASE;
  protected static final byte DOCUMENT_STATE_DEPLETE     = DOCUMENT_STATE_CREATED + 1;
  public static final byte DOCUMENT_STATE_COMPLETED      = DOCUMENT_STATE_DEPLETE + 1;
  public static final byte DOCUMENT_STATE_END            = DOCUMENT_STATE_COMPLETED + 1;

  ///////////////////////////////////////////////////////////////////////////
  /// immutables (Do not reset immutables!)
  ///////////////////////////////////////////////////////////////////////////

  protected final byte m_grammarType;

  protected final GrammarCache m_grammarCache;
  protected final EXISchema m_schema;

  ///////////////////////////////////////////////////////////////////////////
  /// Constructors, initializers
  ///////////////////////////////////////////////////////////////////////////

  /**
   */
  protected Grammar(byte grammarType, GrammarCache grammarCache) {
    // immutables
    m_grammarType = grammarType;
    m_grammarCache = grammarCache;
    m_schema = m_grammarCache.getEXISchema();
  }

  /**
   */
  public void init(GrammarState stateVariables) {
    stateVariables.targetGrammar = this;
    stateVariables.phase = COMMON_STATE_ABSENT;
  }

  public abstract boolean isSchemaInformed();
  
  ///////////////////////////////////////////////////////////////////////////
  /// Accessors
  ///////////////////////////////////////////////////////////////////////////
  
  EXISchema getEXISchema() {
    return m_schema;
  }
  
  public final byte getGrammarType() {
    return m_grammarType;
  }
  
  ///////////////////////////////////////////////////////////////////////////
  /// Finish
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Remove itself from the stack then notify the parent.
   */
  final void finish(GrammarState stateVariables) {
    assert stateVariables == stateVariables.documentGrammarState.currentState;
    final GrammarState parent = stateVariables.parent;
    stateVariables.documentGrammarState.popState();
    if (parent != null)
      parent.targetGrammar.done(stateVariables, parent);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Method declarations for event processing
  ///////////////////////////////////////////////////////////////////////////

  abstract EventTypeList getNextEventTypes(GrammarState stateVariables);
  
  abstract EventCodeTuple getNextEventCodes(GrammarState stateVariables);

  abstract void element(int eventTypeIndex, String uri, String name, GrammarState stateVariables);

  /**
   * Signals xsi:type event.
   */
  abstract void xsitp(int tp, GrammarState stateVariables);

  /**
   * Signals xsi:nil event.
   */
  abstract void nillify(GrammarState stateVariables);
  
  public abstract void chars(GrammarState stateVariables);

  public abstract void undeclaredChars(GrammarState stateVariables);

  /**
   * Signals CM, PI or ER event. 
   */
  public abstract void miscContent(GrammarState stateVariables);

  abstract void done(GrammarState kid, GrammarState stateVariables);

  /**
   * Signals the end of an element.
   * @param uri uri of the element (interned, nullable)
   * @param name name of the element (interned)
   */
  abstract void end(String uri, String name, GrammarState stateVariables);

  Grammar undeclaredElement(final String uri, final String name, final GrammarState stateVariables) {
    final GrammarState kid = stateVariables.documentGrammarState.pushState();
    final int substance = getElementBroadly(uri, name);
    if (substance != EXISchema.NIL_NODE) {
      final Grammar elementGrammar;
      elementGrammar = m_grammarCache.retrieveElementGrammar(substance);
      elementGrammar.init(kid);
      return elementGrammar;
    }
    else {
      final DocumentGrammarState documentGrammarState = stateVariables.documentGrammarState;
      final Grammar builtinElementGrammar;
      builtinElementGrammar = documentGrammarState.builtinGrammarCache.retrieveElementGrammar(
          uri, name, m_grammarCache, documentGrammarState.eventTypesWorkSpace);
      builtinElementGrammar.init(kid);
      return builtinElementGrammar;
    }
  }

  void undeclaredAttribute(String uri, String name, GrammarState stateVariables) {
  }

  void startDocument(GrammarState stateVariables) {
  }

  void endDocument(GrammarState stateVariables) {
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Utility implementations
  ///////////////////////////////////////////////////////////////////////////

  /**
   * Returns an element that has the specified name and namespace name.
   * @param uri namespace name (nillable)
   * @param name element name
   * @return element if available otherwise NIL_NODE
   */
  protected final int getElementBroadly(String uri, String name) {
    return m_schema != null ? m_schema.getElemOfSchema(uri, name) : EXISchema.NIL_NODE; 
  }
  
  /**
   * Create EventTypeSchema(s) from a node in EXISchema.
   */
  protected final EventTypeSchema[] createEventType(final int nd, final int index, int serial, 
    final Grammar ownerGrammar, EventTypeList eventTypeList) {
    EventTypeSchema eventType;
    String uri, name;
    if (nd != EXISchema.NIL_NODE) {
      final int[] nodes = m_schema.getNodes();
      switch (nodes[nd]) {
        case EXISchema.ATTRIBUTE_NODE:
          assert ownerGrammar.getGrammarType() == Grammar.SCHEMA_GRAMMAR_ELEMENT_FRAGMENT;
          uri = m_schema.getTargetNamespaceNameOfAttr(nd);
          name = m_schema.getNameOfAttr(nd);
          eventType = new EventTypeSchemaAttribute(nd, m_schema.isSpecificINodeInFragment(nd),
              uri, name, index, serial, ownerGrammar, eventTypeList);
          break;
        case EXISchema.ATTRIBUTE_USE_NODE:
          uri = m_schema.getTargetNamespaceNameOfAttrUse(nd);
          name = m_schema.getNameOfAttrUse(nd);
          eventType = new EventTypeSchemaAttribute(m_schema.getAttrOfAttrUse(nd), 
              uri, name, index, serial, ownerGrammar, eventTypeList);
          break;
        case EXISchema.PARTICLE_NODE:
          final int term = m_schema.getTermOfParticle(nd);
          EventTypeSchema[] eventTypes;
          switch (nodes[term]) {
            case EXISchema.ELEMENT_NODE:
              final int n_substitutables = m_schema.getSubstitutableCountOfElem(term);
              eventTypes = new EventTypeSchema[n_substitutables + 1];
              if (n_substitutables == 0) {
                uri = m_schema.getTargetNamespaceNameOfElem(term);
                name = m_schema.getNameOfElem(term);
                eventTypes[0] = new EventTypeSchemaElement(nd, term, uri, name, index, 
                    serial++, ownerGrammar, eventTypeList);
              }
              else {
                // REVISIT: find a way to sort while processing schemas
                SortedSet<ComparableSchemaElement> sortedElements;
                sortedElements = new TreeSet<ComparableSchemaElement>();
                uri = m_schema.getTargetNamespaceNameOfElem(term);
                name = m_schema.getNameOfElem(term);
                sortedElements.add(new ComparableSchemaElement(uri, name, term));
                int i;
                for (i = 0; i < n_substitutables; i++) {
                  final int elem = m_schema.getSubstitutableOfElem(term, i);
                  uri = m_schema.getTargetNamespaceNameOfElem(elem);
                  name = m_schema.getNameOfElem(elem);
                  sortedElements.add(new ComparableSchemaElement(uri, name, elem));
                }
                final Iterator<ComparableSchemaElement> iter = sortedElements.iterator();
                for (i = 0; i < n_substitutables + 1; i++) {
                  assert iter.hasNext();
                  ComparableSchemaElement nextElement = iter.next();
                  eventTypes[i] = new EventTypeSchemaElement(nd, nextElement.elem,
                      nextElement.uri, nextElement.name, index, serial++, ownerGrammar, eventTypeList);
                }
                assert !iter.hasNext();
              }
              return eventTypes;
            default:
              assert nodes[term] == EXISchema.WILDCARD_NODE;
              int constraintType = m_schema.getConstraintTypeOfWildcard(term);
              if (constraintType == EXISchema.WC_TYPE_NAMESPACES) {
                int n_uris = m_schema.getNamespaceCountOfWildcard(term);
                eventTypes = new EventTypeSchema[n_uris];
                for (int i = 0; i < n_uris; i++) {
                  uri = m_schema.getNamespaceNameOfWildcard(term, i);
                  eventTypes[i] = new EventTypeSchemaElementWildcardNS(nd, term, uri, index, 
                      serial++, ownerGrammar, eventTypeList);
                }
                return eventTypes;
              }
              eventType = new EventTypeSchemaElementWildcardAny(nd, term, index, 
                  serial, ownerGrammar, eventTypeList);
          }
          break;
        case EXISchema.SIMPLE_TYPE_NODE:
          uri = m_schema.getTargetNamespaceNameOfType(nd);
          name = m_schema.getNameOfType(nd);
          eventType = new EventTypeSchemaCharacters(nd, uri, name, index, serial, ownerGrammar, eventTypeList);
          break;
        case EXISchema.ELEMENT_NODE:
          uri = m_schema.getTargetNamespaceNameOfElem(nd);
          name = m_schema.getNameOfElem(nd);
          eventType = new EventTypeSchemaElement(EXISchema.NIL_NODE, nd, uri, name, index, serial, ownerGrammar, eventTypeList);
          break;
        case EXISchema.WILDCARD_NODE:
          assert ownerGrammar instanceof ElementTagGrammar;
          int constraintType = m_schema.getConstraintTypeOfWildcard(nd);
          if (constraintType == EXISchema.WC_TYPE_NAMESPACES) {
            int n_uris = m_schema.getNamespaceCountOfWildcard(nd);
            eventTypes = new EventTypeSchema[n_uris];
            for (int i = 0; i < n_uris; i++) {
              uri = m_schema.getNamespaceNameOfWildcard(nd, i);
              eventTypes[i] = new EventTypeSchemaAttributeWildcardNS(nd, uri, serial++, ownerGrammar, eventTypeList);
            }
            return eventTypes;
          }
          eventType = EventTypeSchemaAttributeWildcardAny.createLevelOne(nd, serial, ownerGrammar, eventTypeList); 
          break;
        default:
          assert false;
          eventType = null;
          break;
      }
    }
    else {
      eventType = new EventTypeSchemaEndElement(index, serial, ownerGrammar, eventTypeList);
    }
    return new EventTypeSchema[] { eventType };
  }
  
  private static final class ComparableSchemaElement implements Comparable<ComparableSchemaElement> {
    final String uri;
    final String name;
    final int elem;
    
    ComparableSchemaElement(String uri, String name, int elem) {
      this.uri = uri != null ? uri : "";
      this.name = name;
      this.elem = elem;
    }
    
    public int compareTo(ComparableSchemaElement other) {
      int res;
      if ((res = name.compareTo(other.name)) != 0)
        return res;
      else {
        return uri.compareTo(other.uri);
      }
    }
  }
  
}
