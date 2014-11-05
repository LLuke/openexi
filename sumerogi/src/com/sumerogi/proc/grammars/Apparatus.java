package com.sumerogi.proc.grammars;

import com.sumerogi.proc.common.AlignmentType;
import com.sumerogi.proc.common.EventType;
import com.sumerogi.proc.common.EventTypeList;
//import com.sumerogi.proc.common.QName;
import com.sumerogi.proc.common.StringTable;
//import com.sumerogi.schema.EXISchema;
//import com.sumerogi.schema.EXISchemaConst;

public abstract class Apparatus {

  private static final short INIT_GRAMMARS_DEPTH = 32;
  
  ///////////////////////////////////////////////////////////////////////////
  /// variables
  ///////////////////////////////////////////////////////////////////////////

  public GrammarState currentState;

  protected GrammarState[] m_statesStack;
  protected int m_n_stackedStates;
  
  /**
   * work space used for duplicating BuiltinElementGrammar and BuiltinFragmentGrammar.
   */
//  public final EventType[] eventTypesWorkSpace;  
  
  public StringTable stringTable;
  
//  private static final short CODEC_NOT_A_CODEC = 0; // DO NOT CHANGE!
  public static final short CODEC_STRING = 1;
//  public static final short CODEC_NUMBER = 2;
  public static final short CODEC_BOOLEAN = 3;

//  public static final short CODEC_BASE64BINARY = 1;
//  public static final short CODEC_HEXBINARY = 2;
//  public static final short CODEC_BOOLEAN = 3;
//  public static final short CODEC_DATETIME = 4;
//  public static final short CODEC_TIME = 5;
//  public static final short CODEC_DATE = 6;
//  public static final short CODEC_GYEARMONTH = 7;
//  public static final short CODEC_GYEAR = 8;
//  public static final short CODEC_GMONTHDAY = 9;
//  public static final short CODEC_GDAY = 10;
//  public static final short CODEC_GMONTH = 11;
//  public static final short CODEC_DECIMAL = 12;
//  public static final short CODEC_DOUBLE = 13;
//  public static final short CODEC_INTEGER = 14;
//  public static final short CODEC_STRING = 15;
//  public static final short CODEC_LIST = 16;
//  public static final short CODEC_ENUMERATION = 17;
//  protected static final short N_CODECS = 18;
  
//  // pseudo codec with codec IDs intentionally *not* smaller than N_CODECS
//  public static final short CODEC_LEXICAL = 18;  

//  private static final short[] defaultCodecTable; 
//  static {
//    // Implements Table 7-1. Built-in EXI Datatype Representations
//    defaultCodecTable = new short[EXISchemaConst.N_PRIMITIVE_TYPES_PLUS_INTEGER];
//    defaultCodecTable[EXISchemaConst.BASE64BINARY_TYPE] = CODEC_BASE64BINARY;
//    defaultCodecTable[EXISchemaConst.HEXBINARY_TYPE] = CODEC_HEXBINARY;
//    defaultCodecTable[EXISchemaConst.BOOLEAN_TYPE] = CODEC_BOOLEAN;
//    defaultCodecTable[EXISchemaConst.DATETIME_TYPE] = CODEC_DATETIME;
//    defaultCodecTable[EXISchemaConst.TIME_TYPE] = CODEC_TIME;
//    defaultCodecTable[EXISchemaConst.DATE_TYPE] = CODEC_DATE;
//    defaultCodecTable[EXISchemaConst.G_YEARMONTH_TYPE] = CODEC_GYEARMONTH;
//    defaultCodecTable[EXISchemaConst.G_YEAR_TYPE] = CODEC_GYEAR;
//    defaultCodecTable[EXISchemaConst.G_MONTHDAY_TYPE] = CODEC_GMONTHDAY;
//    defaultCodecTable[EXISchemaConst.G_DAY_TYPE] = CODEC_GDAY;
//    defaultCodecTable[EXISchemaConst.G_MONTH_TYPE] = CODEC_GMONTH;
//    defaultCodecTable[EXISchemaConst.DECIMAL_TYPE] = CODEC_DECIMAL;
//    defaultCodecTable[EXISchemaConst.FLOAT_TYPE] = CODEC_DOUBLE;
//    defaultCodecTable[EXISchemaConst.DOUBLE_TYPE] = CODEC_DOUBLE;
//    defaultCodecTable[EXISchemaConst.INTEGER_TYPE] = CODEC_INTEGER;
//    defaultCodecTable[EXISchemaConst.STRING_TYPE] = CODEC_STRING;
//    defaultCodecTable[EXISchemaConst.ANY_SIMPLE_TYPE] = CODEC_STRING;
//    /**
//     * Lists are take care of in updateCodecTable()
//     * defaultCodecTable[EXISchemaConst.NMTOKENS_TYPE] = CODEC_LIST;
//     * defaultCodecTable[EXISchemaConst.IDREFS_TYPE] = CODEC_LIST;
//     * defaultCodecTable[EXISchemaConst.ENTITIES_TYPE] = CODEC_LIST;
//     */
//  }
  
//  public EXISchema schema;
//  protected int[] m_types;
//  protected short[] m_codecTable; // simple type serial -> codec id
//  protected int[] m_restrictedCharacterCountTable; // simple type serial -> # in restricted character set

//  protected abstract ValueApparatus[] getValueApparatuses();
  
//  protected boolean m_preserveLexicalValues;
  
  public Apparatus() {
    m_statesStack = new GrammarState[INIT_GRAMMARS_DEPTH];
    currentState = m_statesStack[0] = new GrammarState(this);
    for (int i = 1; i < INIT_GRAMMARS_DEPTH; i++) {
      m_statesStack[i] = new GrammarState(this);
    }
    m_n_stackedStates = 1;
//    eventTypesWorkSpace = new EventType[BuiltinGrammar.N_NONSCHEMA_ITEMS];
    
//    schema = null;
//    m_types = null;
//    m_codecTable = null;
//    m_restrictedCharacterCountTable = null;
  }

  public void reset() {
    m_n_stackedStates = 1;
    currentState = m_statesStack[0];
    stringTable.reset();
  }
  
  public abstract AlignmentType getAlignmentType();

//  public void setSchema(EXISchema schema, QName[] dtrm, int n_bindings) {
//    if ((this.schema = schema) != null) {
//      m_types = schema.getTypes();
//      updateCodecTable(dtrm, n_bindings);
//      updateSimpleTypeData();
//    }
//    else {
//      m_types = null;
//    }
//  }
  
  public void setStringTable(StringTable stringTable) {
    this.stringTable = stringTable;
  }
  
//  public void setPreserveLexicalValues(boolean preserveLexicalValues) {
//    m_preserveLexicalValues = preserveLexicalValues;
//  }
//
//  /**
//   * Not for public use.
//   * @y.exclude
//   */  
//  public final boolean getPreserveLexicalValues() {
//    return m_preserveLexicalValues;
//  }

  ///////////////////////////////////////////////////////////////////////////
  /// APIs specific to SchemaVM
  ///////////////////////////////////////////////////////////////////////////

  public final void startDocument() {
    currentState.targetGrammar.startDocument(currentState);
  }
  
  public final EventTypeList getNextEventTypes() {
    return currentState.targetGrammar.getNextEventTypes(currentState);
  }
  
  public final EventCodeTuple getNextEventCodes() {
    return currentState.targetGrammar.getNextEventCodes(currentState);
  }
  
  public final void startObjectNamed(EventType eventType) {
    currentState.targetGrammar.startObjectNamed(eventType, currentState);
  }
  
  public final void startObjectAnonymous(EventType eventType) {
    currentState.targetGrammar.startObjectAnonymous(eventType, currentState);
  }
  
  public final void startObjectWildcard(int name) {
    currentState.targetGrammar.startObjectWildcard(name, currentState);
  }

  public final void startArrayNamed(EventType eventType) {
    currentState.targetGrammar.startArrayNamed(eventType, currentState);
  }
  
  public final void startArrayAnonymous() {
    currentState.targetGrammar.startArrayAnonymous(currentState);
  }

  public final void startArrayWildcard(int name) {
    currentState.targetGrammar.startArrayWildcard(name, currentState);
  }

//  public final void startWildcardElement(int eventTypeIndex, int uri, int localName) {
//    currentState.targetGrammar.wildcardElement(eventTypeIndex, uri, localName, currentState);
//  }
//  
//  public final void attribute(EventType eventType) {
//    currentState.targetGrammar.attribute(eventType, currentState);
//  }
//  
  
  public void anonymousStringValue(EventType eventType) {
    currentState.targetGrammar.anonymousStringValue(eventType, currentState);
  }
  
  public void wildcardStringValue(int eventTypeIndex, int nameId) {
    currentState.targetGrammar.wildcardStringValue(eventTypeIndex, nameId);
  }

  public void anonymousNumberValue(EventType eventType) {
    currentState.targetGrammar.anonymousNumberValue(eventType, currentState);
  }
  
  public void wildcardNumberValue(int eventTypeIndex, int nameId) {
    currentState.targetGrammar.wildcardNumberValue(eventTypeIndex, nameId);
  }

  public void anonymousNullValue(EventType eventType) {
    currentState.targetGrammar.anonymousNullValue(eventType, currentState);
  }

  public void wildcardNullValue(int eventTypeIndex, int nameId, GrammarState stateVariables) {
    currentState.targetGrammar.wildcardNullValue(eventTypeIndex, nameId);
  }

  public void anonymousBooleanValue(EventType eventType) {
    currentState.targetGrammar.anonymousBooleanValue(eventType, currentState);
  }

  public void wildcardBooleanValue(int eventTypeIndex, int nameId) {
    currentState.targetGrammar.wildcardBooleanValue(eventTypeIndex, nameId);
  }
  
//  public final void characters(EventType eventType) {
//    currentState.targetGrammar.chars(eventType, currentState);
//  }

//  public final void undeclaredCharacters(int eventTypeIndex) {
//    currentState.targetGrammar.undeclaredChars(eventTypeIndex, currentState);
//  }

//  public final void miscContent(int eventTypeIndex) {
//    currentState.targetGrammar.miscContent(eventTypeIndex, currentState);
//  }

  /**
   * Signals the end of an object.
   */
  public final void endObject() {
    currentState.targetGrammar.endObject(currentState);
    currentState = m_statesStack[--m_n_stackedStates - 1];
  }

  /**
   * Signals the end of an array.
   */
  public final void endArray() {
    currentState.targetGrammar.endArray(currentState);
    currentState = m_statesStack[--m_n_stackedStates - 1];
  }

  public final void endDocument() {
    assert currentState.targetGrammar.grammarType == Grammar.SCHEMA_GRAMMAR_DOCUMENT; 
    currentState.targetGrammar.endDocument(currentState);
  }

  ///////////////////////////////////////////////////////////////////////////
  /// Utilities
  ///////////////////////////////////////////////////////////////////////////

  final GrammarState pushState() {
    final int stackLength; 
    if ((stackLength = m_statesStack.length) == m_n_stackedStates) {
      final GrammarState[] _statesStack;
      final int _stackLength = 2 * stackLength;
      _statesStack = new GrammarState[_stackLength];
      System.arraycopy(m_statesStack, 0, _statesStack, 0, stackLength);
      for (int i = stackLength; i < _stackLength; i++) {
        _statesStack[i] = new GrammarState(this);
      }
      m_statesStack = _statesStack;
    }
    return currentState = m_statesStack[m_n_stackedStates++];
  }
  
//  private void updateCodecTable(QName[] dtrm, int n_bindings) {
//    assert schema != null;
//    final int n_stypes = schema.getTotalSimpleTypeCount();
//    if (m_codecTable == null || m_codecTable.length < n_stypes + 1)
//      m_codecTable = new short[n_stypes + 1];
//    else {
//      final int len = m_codecTable.length;
//      for (int i = 0; i < len; i++)
//        m_codecTable[i] = CODEC_NOT_A_CODEC;
//    }
//    // Intrinsic DTRM (i.e. Table 7-1)
//    System.arraycopy(defaultCodecTable, 0, m_codecTable, 0, defaultCodecTable.length);
//    final int anySimpleType = schema.getBuiltinTypeOfSchema(EXISchemaConst.ANY_SIMPLE_TYPE); 
//    int stype = anySimpleType;
//    for (int ind = 1; stype != EXISchema.NIL_NODE; stype = schema.getNextSimpleType(stype), ++ind) {
//      if (m_codecTable[ind] == CODEC_NOT_A_CODEC) {
//        if (schema.getBaseTypeOfSimpleType(stype) == anySimpleType) {
//          switch (schema.getVarietyOfSimpleType(stype)) {
//          case EXISchema.LIST_SIMPLE_TYPE:
//            m_codecTable[ind] = CODEC_LIST;
//            break;
//          case EXISchema.UNION_SIMPLE_TYPE:
//            m_codecTable[ind] = CODEC_STRING;
//            break;
//          }
//        }
//      }
//    }
//    // Go through user's DTRM
//    final int n_qnames;
//    if ((n_qnames = 2 * n_bindings) != 0) {
//      final ValueApparatus[] valueApparatuses = getValueApparatuses();
//      for (int i = 0; i < n_qnames; i += 2) {
//        final QName typeQName = dtrm[i];
//        final String typeUri = typeQName.namespaceName; 
//        final String typeName = typeQName.localName;
//        final int tp;
//        if ((tp = schema.getTypeOfSchema(typeUri, typeName)) != EXISchema.NIL_NODE) {
//          if (schema.isSimpleType(tp)) {
//            final QName codecQName = dtrm[i + 1];
//            final int n_valueScribers = valueApparatuses.length;
//            for (int j = 0; j < n_valueScribers; j++) {
//              final ValueApparatus valueScriber = valueApparatuses[j];
//              if (codecQName.equals(valueScriber.getName())) {
//                final short codecID = valueScriber.getCodecID();
//                final int typeSerial = schema.getSerialOfType(tp);
//                m_codecTable[typeSerial] = codecID;
//                break;
//              }
//            }
//          }
//        }
//      }
//    }
//    stype = anySimpleType;
//    for (int ind = 1; stype != EXISchema.NIL_NODE; stype = schema.getNextSimpleType(stype), ++ind) {
//      if (m_codecTable[ind] == CODEC_NOT_A_CODEC) {
//        final short codecID;
//        short _codecID = CODEC_NOT_A_CODEC;
//        final byte variety;
//        switch (variety = schema.getVarietyOfSimpleType(stype)) {
//          case EXISchema.UNION_SIMPLE_TYPE:
//          case EXISchema.LIST_SIMPLE_TYPE:
//          case EXISchema.ATOMIC_SIMPLE_TYPE:
//            int tp = stype;
//            if (_codecID == CODEC_NOT_A_CODEC) {
//              do {
//                tp = schema.getBaseTypeOfSimpleType(tp);
//                if ((_codecID = m_codecTable[schema.getSerialOfType(tp)]) != CODEC_NOT_A_CODEC)
//                  break;
//              }
//              while (true);
//            }
//            if (variety == EXISchema.ATOMIC_SIMPLE_TYPE) {
//              final int ancestryId = schema.ancestryIds[schema.getSerialOfType(stype)];
//              if (ancestryId != EXISchemaConst.QNAME_TYPE && ancestryId != EXISchemaConst.NOTATION_TYPE) {
//                if (schema.getEnumerationFacetCountOfAtomicSimpleType(stype) != 0) {
//                  if (stype == tp || schema.getEnumerationFacetCountOfAtomicSimpleType(tp) == 0)
//                    _codecID = CODEC_ENUMERATION;
//                }
//              }
//            }
//            else {
//              assert _codecID != CODEC_NOT_A_CODEC;
//            }
//            codecID = _codecID;
//            break;
//          case EXISchema.UR_SIMPLE_TYPE:
//            assert false;
//            continue;
//          default:
//            assert false;
//            codecID = CODEC_NOT_A_CODEC;
//            break;
//        }
//        m_codecTable[ind] = codecID;
//      }
//    }
//  }

//  private void updateSimpleTypeData() {
//    final int n_stypes = schema.getTotalSimpleTypeCount();
//    if (m_restrictedCharacterCountTable == null || m_restrictedCharacterCountTable.length < n_stypes + 1) {
//      m_restrictedCharacterCountTable = new int[n_stypes + 1];
//    }
//    int stype = schema.getBuiltinTypeOfSchema(EXISchemaConst.ANY_SIMPLE_TYPE);
//    for (int ind = 1; stype != EXISchema.NIL_NODE; stype = schema.getNextSimpleType(stype), ++ind) {
//      final int serial = schema.getSerialOfType(stype);
//      assert serial == ind;
//      m_restrictedCharacterCountTable[ind] = schema.ancestryIds[serial] == EXISchemaConst.STRING_TYPE ?
//          schema.getRestrictedCharacterCountOfStringSimpleType(stype) : 0;
//    }
//  }

}
