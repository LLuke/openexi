package org.openexi.fujitsu.proc.io;

import org.openexi.fujitsu.proc.common.QName;
import org.openexi.fujitsu.schema.EXISchema;
import org.openexi.fujitsu.schema.EXISchemaConst;

public abstract class Apparatus {

  private static final short CODEC_NOT_A_CODEC = 0; // DO NOT CHANGE!
  public static final short CODEC_BASE64BINARY = 1;
  public static final short CODEC_HEXBINARY = 2;
  public static final short CODEC_BOOLEAN = 3;
  public static final short CODEC_DATETIME = 4;
  public static final short CODEC_TIME = 5;
  public static final short CODEC_DATE = 6;
  public static final short CODEC_GYEARMONTH = 7;
  public static final short CODEC_GYEAR = 8;
  public static final short CODEC_GMONTHDAY = 9;
  public static final short CODEC_GDAY = 10;
  public static final short CODEC_GMONTH = 11;
  public static final short CODEC_DECIMAL = 12;
  public static final short CODEC_DOUBLE = 13;
  public static final short CODEC_INTEGER = 14;
  public static final short CODEC_STRING = 15;
  public static final short CODEC_LIST = 16;
  public static final short CODEC_ENUMERATION = 17;
  protected static final short N_CODECS = 18;

  private static final short[] defaultCodecTable; // Implements Table 7-1. Built-in EXI Datatype Representations
  static {
    defaultCodecTable = new short[EXISchemaConst.N_BUILTIN_TYPES];
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
    defaultCodecTable[EXISchemaConst.NMTOKENS_TYPE] = CODEC_LIST;
    defaultCodecTable[EXISchemaConst.IDREFS_TYPE] = CODEC_LIST;
    defaultCodecTable[EXISchemaConst.ENTITIES_TYPE] = CODEC_LIST;
  }

  protected EXISchema m_schema;
  protected short[] m_codecTable; // simple type serial -> codec id

  private CharacterBuffer m_characterBuffer;

  Apparatus() {
    m_schema = null;
    m_codecTable = null;
    m_characterBuffer = new CharacterBuffer();
  }

  public void reset() {
    m_characterBuffer = new CharacterBuffer();
  }

  abstract ValueApparatus[] getValueApparatuses();

  protected final CharacterBuffer ensureCharacters(final int length) {
    CharacterBuffer characterBuffer = m_characterBuffer;
    final int availability;
    if ((availability = m_characterBuffer.availability()) < length) {
      final int bufSize = length > CharacterBuffer.BUFSIZE_DEFAULT ? length : CharacterBuffer.BUFSIZE_DEFAULT;
      characterBuffer = new CharacterBuffer(bufSize);
    }
    if (characterBuffer != m_characterBuffer) {
      final int _availability = characterBuffer.availability();
      if (_availability != 0 && availability < _availability) {
        m_characterBuffer = characterBuffer;
      }
    }
    return characterBuffer;
  }

  public void setSchema(EXISchema schema, QName[] dtrm, int n_bindings) {
    if ((m_schema = schema) != null) {
      updateCodecTable(dtrm, n_bindings);
    }
  }

  private void updateCodecTable(QName[] dtrm, int n_bindings) {
    assert m_schema != null;
    final int n_stypes = m_schema.getTotalSimpleTypeCount();
    if (m_codecTable == null || m_codecTable.length < n_stypes + 1)
      m_codecTable = new short[n_stypes + 1];
    else {
      final int len = m_codecTable.length;
      for (int i = 0; i < len; i++)
        m_codecTable[i] = CODEC_NOT_A_CODEC;
    }
    System.arraycopy(defaultCodecTable, 0, m_codecTable, 0, defaultCodecTable.length);
    final int n_qnames;
    if ((n_qnames = 2 * n_bindings) != 0) {
      final ValueApparatus[] valueApparatuses = getValueApparatuses();
      for (int i = 0; i < n_qnames; i += 2) {
        final QName typeQName = dtrm[i];
        final String typeUri = typeQName.namespaceName; 
        final String typeName = typeQName.localName;
        final int ns, tp;
        if ((ns = m_schema.getNamespaceOfSchema(typeUri.length() != 0 ? typeUri : null)) != EXISchema.NIL_NODE) {
          if ((tp = m_schema.getTypeOfNamespace(ns, typeName)) == EXISchema.NIL_NODE)
            continue;
          else if (m_schema.getNodeType(tp) != EXISchema.SIMPLE_TYPE_NODE)
            continue;
        }
        else
          continue;
        final QName codecQName = dtrm[i + 1];
        final int n_valueScribers = valueApparatuses.length;
        for (int j = 0; j < n_valueScribers; j++) {
          final ValueApparatus valueScriber = valueApparatuses[j];
          if (codecQName.equals(valueScriber.getName())) {
            final short codecID = valueScriber.getCodecID();
            final int typeSerial = m_schema.getSerialOfType(tp);
            m_codecTable[typeSerial] = codecID;
            break;
          }
        }
      }
    }
    int stype = m_schema.getBuiltinTypeOfSchema(EXISchemaConst.ANY_SIMPLE_TYPE);
    for (int ind = 1; stype != EXISchema.NIL_NODE; stype = m_schema.getNextSimpleType(stype), ++ind) {
      if (m_codecTable[ind] == CODEC_NOT_A_CODEC) {
        final short codecID;
        switch (m_schema.getVarietyOfSimpleType(stype)) {
          case EXISchema.UNION_SIMPLE_TYPE:
            codecID = CODEC_STRING;
            break;
          case EXISchema.LIST_SIMPLE_TYPE:
            codecID = CODEC_LIST;
            break;
          case EXISchema.UR_SIMPLE_TYPE:
          case EXISchema.ATOMIC_SIMPLE_TYPE:
            short _codecID = CODEC_NOT_A_CODEC;
            /*
             * if (m_schema.getEnumerationFacetCountOfSimpleType(stype) != 0) {
             *   final int primType = m_schema.getPrimitiveTypeOfAtomicSimpleType(stype);
             *   final int primTypeId = m_schema.getSerialOfType(primType);
             *   if (primTypeId != EXISchemaConst.QNAME_TYPE && primTypeId != EXISchemaConst.NOTATION_TYPE)
             *     _codecID = CODEC_ENUMERATION;
             * }
             * if (_codecID == CODEC_NOT_A_CODEC) {
             *   int tp = stype;
             *   do {
             *     tp = m_schema.getBaseTypeOfType(tp);
             *     if ((_codecID = m_codecTable[m_schema.getSerialOfType(tp)]) != CODEC_NOT_A_CODEC) {
             *       break;
             *     }
             *   }
             *   while (true);
             * }
             * codecID = _codecID;
             */
            int tp = stype;
            do {
              tp = m_schema.getBaseTypeOfType(tp);
              if ((_codecID = m_codecTable[m_schema.getSerialOfType(tp)]) != CODEC_NOT_A_CODEC)
                break;
            }
            while (true);
            
            if (m_schema.getEnumerationFacetCountOfSimpleType(stype) != 0) {
              final int primType = m_schema.getPrimitiveTypeOfAtomicSimpleType(stype);
              final int primTypeId = m_schema.getSerialOfType(primType);
              if (primTypeId != EXISchemaConst.QNAME_TYPE && primTypeId != EXISchemaConst.NOTATION_TYPE &&
                  m_schema.getEnumerationFacetCountOfSimpleType(tp) == 0) {
                _codecID = CODEC_ENUMERATION;
              }
            }
            codecID = _codecID;
            break;
          default:
            codecID = CODEC_NOT_A_CODEC;
            assert false;
        }
        m_codecTable[ind] = codecID;
      }
    }
  }

}
