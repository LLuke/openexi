using System;
using System.Diagnostics;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using QName = Nagasena.Proc.Common.QName;
using StringTable = Nagasena.Proc.Common.StringTable;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;

namespace Nagasena.Proc.Grammars {

  /// <exclude/>
  public abstract class Apparatus {

    private const short INIT_GRAMMARS_DEPTH = 32;

    ///////////////////////////////////////////////////////////////////////////
    /// variables
    ///////////////////////////////////////////////////////////////////////////

    public GrammarState currentState;

    protected internal GrammarState[] m_statesStack;
    protected internal int m_n_stackedStates;

    /// <summary>
    /// work space used for duplicating BuiltinElementGrammar and BuiltinFragmentGrammar.
    /// </summary>
    public readonly EventType[] eventTypesWorkSpace;

    public StringTable stringTable;

    private const short CODEC_NOT_A_CODEC = 0; // DO NOT CHANGE!
    public const short CODEC_BASE64BINARY = 1;
    public const short CODEC_HEXBINARY = 2;
    public const short CODEC_BOOLEAN = 3;
    public const short CODEC_DATETIME = 4;
    public const short CODEC_TIME = 5;
    public const short CODEC_DATE = 6;
    public const short CODEC_GYEARMONTH = 7;
    public const short CODEC_GYEAR = 8;
    public const short CODEC_GMONTHDAY = 9;
    public const short CODEC_GDAY = 10;
    public const short CODEC_GMONTH = 11;
    public const short CODEC_DECIMAL = 12;
    public const short CODEC_DOUBLE = 13;
    public const short CODEC_INTEGER = 14;
    public const short CODEC_STRING = 15;
    public const short CODEC_LIST = 16;
    public const short CODEC_ENUMERATION = 17;
    protected internal const short N_CODECS = 18;

    // pseudo codec with codec IDs intentionally *not* smaller than N_CODECS
    public const short CODEC_LEXICAL = 18;

    private static readonly short[] defaultCodecTable;
    static Apparatus() {
      // Implements Table 7-1. Built-in EXI Datatype Representations
      defaultCodecTable = new short[EXISchemaConst.N_PRIMITIVE_TYPES_PLUS_INTEGER];
      defaultCodecTable[EXISchemaConst.BASE64BINARY_TYPE] = CODEC_BASE64BINARY;
      defaultCodecTable[EXISchemaConst.HEXBINARY_TYPE] = CODEC_HEXBINARY;
      defaultCodecTable[EXISchemaConst.BOOLEAN_TYPE] = CODEC_BOOLEAN;
      defaultCodecTable[EXISchemaConst.DATETIME_TYPE] = CODEC_DATETIME;
      defaultCodecTable[EXISchemaConst.TIME_TYPE] = CODEC_TIME;
      defaultCodecTable[EXISchemaConst.DATE_TYPE] = CODEC_DATE;
      defaultCodecTable[EXISchemaConst.G_YEARMONTH_TYPE] = CODEC_GYEARMONTH;
      defaultCodecTable[EXISchemaConst.G_YEAR_TYPE] = CODEC_GYEAR;
      defaultCodecTable[EXISchemaConst.G_MONTHDAY_TYPE] = CODEC_GMONTHDAY;
      defaultCodecTable[EXISchemaConst.G_DAY_TYPE] = CODEC_GDAY;
      defaultCodecTable[EXISchemaConst.G_MONTH_TYPE] = CODEC_GMONTH;
      defaultCodecTable[EXISchemaConst.DECIMAL_TYPE] = CODEC_DECIMAL;
      defaultCodecTable[EXISchemaConst.FLOAT_TYPE] = CODEC_DOUBLE;
      defaultCodecTable[EXISchemaConst.DOUBLE_TYPE] = CODEC_DOUBLE;
      defaultCodecTable[EXISchemaConst.INTEGER_TYPE] = CODEC_INTEGER;
      defaultCodecTable[EXISchemaConst.STRING_TYPE] = CODEC_STRING;
      defaultCodecTable[EXISchemaConst.ANY_SIMPLE_TYPE] = CODEC_STRING;
      /// Lists are take care of in updateCodecTable()
      /// defaultCodecTable[EXISchemaConst.NMTOKENS_TYPE] = CODEC_LIST;
      /// defaultCodecTable[EXISchemaConst.IDREFS_TYPE] = CODEC_LIST;
      /// defaultCodecTable[EXISchemaConst.ENTITIES_TYPE] = CODEC_LIST;
    }

    public EXISchema schema;
    protected internal int[] m_types;
  //  protected byte[] m_ancestryIds;
    protected internal short[] m_codecTable; // simple type serial -> codec id
    protected internal int[] m_restrictedCharacterCountTable; // simple type serial -> # in restricted character set

    protected internal abstract ValueApparatus[] ValueApparatuses { get; }

    protected internal bool m_preserveLexicalValues;

    public Apparatus() {
      m_statesStack = new GrammarState[INIT_GRAMMARS_DEPTH];
      currentState = m_statesStack[0] = new GrammarState(this);
      for (int i = 1; i < INIT_GRAMMARS_DEPTH; i++) {
        m_statesStack[i] = new GrammarState(this);
      }
      m_n_stackedStates = 1;
      eventTypesWorkSpace = new EventType[BuiltinGrammar.N_NONSCHEMA_ITEMS];

      schema = null;
      m_types = null;
      m_codecTable = null;
      m_restrictedCharacterCountTable = null;
    }

    public virtual void reset() {
      m_n_stackedStates = 1;
      currentState = m_statesStack[0];
      stringTable.reset();
    }

    public abstract AlignmentType AlignmentType { get; }

    public virtual void setSchema(EXISchema schema, QName[] dtrm, int n_bindings) {
      if ((this.schema = schema) != null) {
        m_types = schema.Types;
        updateCodecTable(dtrm, n_bindings);
        updateSimpleTypeData();
      }
      else {
        m_types = null;
      }
    }

    public virtual StringTable StringTable {
      set {
        this.stringTable = value;
      }
    }

    public virtual bool PreserveLexicalValues {
      set {
        m_preserveLexicalValues = value;
      }
      get {
        return m_preserveLexicalValues;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    /// APIs specific to SchemaVM
    ///////////////////////////////////////////////////////////////////////////

    public void startDocument() {
      currentState.targetGrammar.startDocument(currentState);
    }

    public EventTypeList NextEventTypes {
      get {
        return currentState.targetGrammar.getNextEventTypes(currentState);
      }
    }

    public EventCodeTuple NextEventCodes {
      get {
        return currentState.targetGrammar.getNextEventCodes(currentState);
      }
    }

    public void startElement(EventType eventType) {
      currentState.targetGrammar.element(eventType, currentState);
    }

    public void startWildcardElement(int eventTypeIndex, int uri, int localName) {
      currentState.targetGrammar.wildcardElement(eventTypeIndex, uri, localName, currentState);
    }

    public void xsitp(int tp) {
      currentState.targetGrammar.xsitp(tp, currentState);
    }

    public void nillify(int eventTypeIndex) {
      currentState.targetGrammar.nillify(eventTypeIndex, currentState);
    }

    public void attribute(EventType eventType) {
      currentState.targetGrammar.attribute(eventType, currentState);
    }

    public void wildcardAttribute(int eventTypeIndex, int uri, int localName) {
      currentState.targetGrammar.wildcardAttribute(eventTypeIndex, uri, localName, currentState);
    }

    public void characters(EventType eventType) {
      currentState.targetGrammar.chars(eventType, currentState);
    }

    public void undeclaredCharacters(int eventTypeIndex) {
      currentState.targetGrammar.undeclaredChars(eventTypeIndex, currentState);
    }

    public void miscContent(int eventTypeIndex) {
      currentState.targetGrammar.miscContent(eventTypeIndex, currentState);
    }

    /// <summary>
    /// Signals the end of an element.
    /// </summary>
    public void endElement() {
      currentState.targetGrammar.end(currentState);
      currentState = m_statesStack[--m_n_stackedStates - 1];
    }

    public void endDocument() {
      Debug.Assert(currentState.targetGrammar.grammarType == Grammar.SCHEMA_GRAMMAR_DOCUMENT || currentState.targetGrammar.grammarType == Grammar.SCHEMA_GRAMMAR_FRAGMENT || currentState.targetGrammar.grammarType == Grammar.BUILTIN_GRAMMAR_FRAGMENT);
      currentState.targetGrammar.endDocument(currentState);
    }

    ///////////////////////////////////////////////////////////////////////////
    /// Utilities
    ///////////////////////////////////////////////////////////////////////////

    internal GrammarState pushState() {
      int stackLength;
      if ((stackLength = m_statesStack.Length) == m_n_stackedStates) {
        GrammarState[] _statesStack;
        int _stackLength = 2 * stackLength;
        _statesStack = new GrammarState[_stackLength];
        Array.Copy(m_statesStack, 0, _statesStack, 0, stackLength);
        for (int i = stackLength; i < _stackLength; i++) {
          _statesStack[i] = new GrammarState(this);
        }
        m_statesStack = _statesStack;
      }
      return currentState = m_statesStack[m_n_stackedStates++];
    }

    private void updateCodecTable(QName[] dtrm, int n_bindings) {
      Debug.Assert(schema != null);
      int n_stypes = schema.TotalSimpleTypeCount;
      if (m_codecTable == null || m_codecTable.Length < n_stypes + 1) {
        m_codecTable = new short[n_stypes + 1];
      }
      else {
        int len = m_codecTable.Length;
        for (int i = 0; i < len; i++) {
          m_codecTable[i] = CODEC_NOT_A_CODEC;
        }
      }
      // Intrinsic DTRM (i.e. Table 7-1)
      Array.Copy(defaultCodecTable, 0, m_codecTable, 0, defaultCodecTable.Length);
      int anySimpleType = schema.getBuiltinTypeOfSchema(EXISchemaConst.ANY_SIMPLE_TYPE);
      int stype = anySimpleType;
      for (int ind = 1; stype != EXISchema.NIL_NODE; stype = schema.getNextSimpleType(stype), ++ind) {
        if (m_codecTable[ind] == CODEC_NOT_A_CODEC) {
          if (schema.getBaseTypeOfSimpleType(stype) == anySimpleType) {
            switch (schema.getVarietyOfSimpleType(stype)) {
            case EXISchema.LIST_SIMPLE_TYPE:
              m_codecTable[ind] = CODEC_LIST;
              break;
            case EXISchema.UNION_SIMPLE_TYPE:
              m_codecTable[ind] = CODEC_STRING;
              break;
            }
          }
        }
      }
      // Go through user's DTRM
      int n_qnames;
      if ((n_qnames = 2 * n_bindings) != 0) {
        ValueApparatus[] valueApparatuses = ValueApparatuses;
        for (int i = 0; i < n_qnames; i += 2) {
          QName typeQName = dtrm[i];
          string typeUri = typeQName.namespaceName;
          string typeName = typeQName.localName;
          int tp;
          if ((tp = schema.getTypeOfSchema(typeUri, typeName)) != EXISchema.NIL_NODE) {
            if (schema.isSimpleType(tp)) {
              QName codecQName = dtrm[i + 1];
              int n_valueScribers = valueApparatuses.Length;
              for (int j = 0; j < n_valueScribers; j++) {
                ValueApparatus valueScriber = valueApparatuses[j];
                if (codecQName.Equals(valueScriber.Name)) {
                  short codecID = valueScriber.CodecID;
                  int typeSerial = schema.getSerialOfType(tp);
                  m_codecTable[typeSerial] = codecID;
                  break;
                }
              }
            }
          }
        }
      }
      stype = anySimpleType;
      for (int ind = 1; stype != EXISchema.NIL_NODE; stype = schema.getNextSimpleType(stype), ++ind) {
        if (m_codecTable[ind] == CODEC_NOT_A_CODEC) {
          short codecID;
          short _codecID = CODEC_NOT_A_CODEC;
          sbyte variety;
          switch (variety = schema.getVarietyOfSimpleType(stype)) {
            case EXISchema.UNION_SIMPLE_TYPE:
            case EXISchema.LIST_SIMPLE_TYPE:
            case EXISchema.ATOMIC_SIMPLE_TYPE:
              int tp = stype;
              if (_codecID == CODEC_NOT_A_CODEC) {
                do {
                  tp = schema.getBaseTypeOfSimpleType(tp);
                  if ((_codecID = m_codecTable[schema.getSerialOfType(tp)]) != CODEC_NOT_A_CODEC) {
                    break;
                  }
                }
                while (true);
              }
              if (variety == EXISchema.ATOMIC_SIMPLE_TYPE) {
                int ancestryId = schema.ancestryIds[schema.getSerialOfType(stype)];
                if (ancestryId != EXISchemaConst.QNAME_TYPE && ancestryId != EXISchemaConst.NOTATION_TYPE) {
                  if (schema.getEnumerationFacetCountOfAtomicSimpleType(stype) != 0) {
                    if (stype == tp || schema.getEnumerationFacetCountOfAtomicSimpleType(tp) == 0) {
                      _codecID = CODEC_ENUMERATION;
                    }
                  }
                }
              }
              else {
                Debug.Assert(_codecID != CODEC_NOT_A_CODEC);
              }
              codecID = _codecID;
              break;
            case EXISchema.UR_SIMPLE_TYPE:
              Debug.Assert(false);
              continue;
            default:
              Debug.Assert(false);
              codecID = CODEC_NOT_A_CODEC;
              break;
          }
          m_codecTable[ind] = codecID;
        }
      }
    }

    private void updateSimpleTypeData() {
      int n_stypes = schema.TotalSimpleTypeCount;
      if (m_restrictedCharacterCountTable == null || m_restrictedCharacterCountTable.Length < n_stypes + 1) {
        m_restrictedCharacterCountTable = new int[n_stypes + 1];
      }
      int stype = schema.getBuiltinTypeOfSchema(EXISchemaConst.ANY_SIMPLE_TYPE);
      for (int ind = 1; stype != EXISchema.NIL_NODE; stype = schema.getNextSimpleType(stype), ++ind) {
        int serial = schema.getSerialOfType(stype);
        Debug.Assert(serial == ind);
        m_restrictedCharacterCountTable[ind] = schema.ancestryIds[serial] == EXISchemaConst.STRING_TYPE ? schema.getRestrictedCharacterCountOfStringSimpleType(stype) : 0;
      }
    }

  }

}