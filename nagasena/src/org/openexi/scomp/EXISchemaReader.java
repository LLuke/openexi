package org.openexi.scomp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.openexi.proc.EXIDecoder;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.EventDescription;
import org.openexi.proc.common.EventType;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.common.XmlUriConst;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.Base64BinaryValueScriber;
import org.openexi.proc.io.DateTimeValueScriber;
import org.openexi.proc.io.DateValueScriber;
import org.openexi.proc.io.DecimalValueScriber;
import org.openexi.proc.io.FloatValueScriber;
import org.openexi.proc.io.GDayValueScriber;
import org.openexi.proc.io.GMonthDayValueScriber;
import org.openexi.proc.io.GMonthValueScriber;
import org.openexi.proc.io.GYearMonthValueScriber;
import org.openexi.proc.io.GYearValueScriber;
import org.openexi.proc.io.HexBinaryValueScriber;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.Scribble;
import org.openexi.proc.io.Scriber;
import org.openexi.proc.io.TimeValueScriber;
import org.openexi.schema.Characters;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EXISchemaLayout;
import org.openexi.schema.GrammarSchema;

/**
 * EXISchemaReader parses EXI-encoded EXI Grammar into an EXISchema. 
 */
public final class EXISchemaReader extends EXISchemaStruct {
  
  private SchemaScanner m_scanner;
  private final Scribble m_scribble;

  private final ArrayList<Integer> m_grammarsInTypes;

  private final ArrayList<Integer> m_typePositions;
  private final ArrayList<Integer> m_gramPositions;
  
  // Production content composite (event type & subsequent grammar) to production address 
  private final HashMap<Long,Integer> m_productionMap;
  
  private static final String ENCODED_FIXTURE_GRAMMARS           = "FixtureGrammars.exi";
  private static final String ENCODED_FIXTURE_TYPES              = "FixtureTypes.exi";
  private static final String ENCODED_FIXTURE_NAMES_NONAMESPACE  = "FixtureNamesNoNamespace.exi";
  private static final String ENCODED_FIXTURE_NAMES_XMLNAMESPACE = "FixtureNamesXmlNamespace.exi";
  private static final String ENCODED_FIXTURE_NAMES_XSINAMESPACE = "FixtureNamesXsiNamespace.exi";
  private static final String ENCODED_FIXTURE_NAMES_XSDNAMESPACE = "FixtureNamesXsdNamespace.exi";
  private static final byte[] m_fixtureGrammarsBytes = new byte[40];
  private static final byte[] m_fixtureTypesBytes = new byte[660];
  private static final byte[] m_fixtureNamesNoNamespace = new byte[4];
  private static final byte[] m_fixtureNamesXmlNamespace = new byte[60];
  private static final byte[] m_fixtureNamesXsiNamespace = new byte[60];
  private static final byte[] m_fixtureNamesXsdNamespace = new byte[470];
  
  private static void loadBytes(String fileName, byte[] bts) {
    URL fixtureGrammarsURI = EXISchemaReader.class.getResource(fileName);
    try {
      InputStream inputStream = fixtureGrammarsURI.openConnection().getInputStream();
      int pos = 0;
      do {
        final int n_bytes;
        if ((n_bytes = inputStream.read(bts, pos, bts.length - pos)) == -1)
          break;
        else
          if (pos == bts.length) {
            throw new RuntimeException();
          }
          if ((pos += n_bytes) == bts.length)
            break;
      }
      while (true);
    }
    catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
  
  static {
    loadBytes(ENCODED_FIXTURE_GRAMMARS, m_fixtureGrammarsBytes);
    loadBytes(ENCODED_FIXTURE_TYPES, m_fixtureTypesBytes);
    loadBytes(ENCODED_FIXTURE_NAMES_NONAMESPACE, m_fixtureNamesNoNamespace);
    loadBytes(ENCODED_FIXTURE_NAMES_XMLNAMESPACE, m_fixtureNamesXmlNamespace);
    loadBytes(ENCODED_FIXTURE_NAMES_XSINAMESPACE, m_fixtureNamesXsiNamespace);
    loadBytes(ENCODED_FIXTURE_NAMES_XSDNAMESPACE, m_fixtureNamesXsdNamespace);
  }
  
  public EXISchemaReader() {
    m_scanner = null;
    m_scribble = new Scribble();
    m_grammarsInTypes = new ArrayList<Integer>();
    m_typePositions = new ArrayList<Integer>();
    m_gramPositions = new ArrayList<Integer>();
    m_productionMap = new HashMap<Long,Integer>();
  }
  
  @Override
  protected void reset() {
    // initialize the arrays
    super.reset();
    m_scanner = null;
  }
  
  @Override
  protected void clear() {
    super.clear();
    m_grammarsInTypes.clear();
    m_typePositions.clear();
    m_gramPositions.clear();
    m_productionMap.clear();
  }

  /**
   * Parses EXI-encoded EXI Grammar into an EXISchema.
   * @param inputStream EXI-encoded EXI Grammar
   * @return EXISchema
   * @throws IOException
   * @throws EXIOptionsException
   */
  public EXISchema parse(InputStream inputStream) throws IOException, EXIOptionsException {
    try {
      reset();
      EXIDecoder decoder = new EXIDecoder();
      decoder.setInputStream(inputStream);
      decoder.setGrammarCache(GrammarCache4Grammar.getGrammarCache());
      final Scanner scanner = decoder.processHeader();
      final EXIOptions exiOptions = scanner.getHeaderOptions();
      if (exiOptions == null) {
        throw new RuntimeException();
      }
      final SchemaId schemaId = exiOptions.getSchemaId();
      if (schemaId == null) {
        throw new RuntimeException();
      }
      final String schemaName = schemaId.getValue();
      if (schemaName != null) {
        if (!"nagasena:grammar".equals(schemaName))
          throw new RuntimeException();
        if (!exiOptions.isStrict())
          throw new RuntimeException();
      }
      m_scanner = SchemaScanner.newScanner(scanner);
      final EventType eventType = expectStartElement("EXIGrammar");
      if (schemaName != null && eventType.itemType != EventType.ITEM_SE)
        throw new RuntimeException();
      return processEXIGrammar();
    }
    finally {
      inputStream.close();
    }
  }
  
  private EXISchema processEXIGrammar() throws IOException {
    expectStartElement("StringTable");
    processStringTable();
    expectStartElement("Types");
    processTypes();
    expectStartElement("Elements");
    processElements();
    expectStartElement("Attributes");
    processAttributes();
    expectStartElement("Grammars");
    processGrammars();
    expectEndElement();
    
    for (int i = 0; i < m_grammarsInTypes.size(); i++) {
      final int pos = m_grammarsInTypes.get(i);
      m_types[pos] = m_gramPositions.get(m_types[pos]);
    }
    
    return new EXISchema(m_elems, m_n_elems,
        m_attrs, m_n_attrs, m_types, m_n_types,
        m_uris, m_n_uris,
        m_names, m_n_names, m_localNames,
        m_strings, m_n_strings,
        m_ints, m_n_ints,
        m_mantissas, m_exponents, m_n_floats,
        m_signs, m_integralDigits, m_reverseFractionalDigits, m_n_decimals,
        m_integers, m_n_integers,
        m_longs, m_n_longs,
        m_datetimes, m_n_datetimes,
        m_durations, m_n_durations,
        m_binaries, m_n_binaries,
        m_variantTypes, m_variants, m_n_variants,
        m_grammars, m_n_grammars, m_grammarCount,
        m_productions, m_n_productions,
        m_eventTypes, m_eventData, m_n_events,
        m_n_stypes);
  }
  
  private void processStringTable() throws IOException {
    ArrayList<int[]> allLocalNames = new ArrayList<int[]>();
    EventDescription event;
    int next = 0;
    event = nextElement();
    do {
      final String uri;
      if (event != null && "NoNamespace".equals(event.getName())) {
        if (next != 0)
          throw new RuntimeException();
        uri = "";
      }
      else if (event != null && "XmlNamespace".equals(event.getName())) {
        if (next == 0) {
          m_scanner.putBack(event);
          // load NoNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesNoNamespace);
          continue;
        }
        if (next != 1)
          throw new RuntimeException();
        uri = XmlUriConst.W3C_XML_1998_URI;
      }
      else if (event != null && "XsiNamespace".equals(event.getName())) {
        if (next == 0) {
          m_scanner.putBack(event);
          // load NoNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesNoNamespace);
          continue;
        }
        if (next == 1) {
          m_scanner.putBack(event);
          // load XmlNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesXmlNamespace);
          continue;
        }
        if (next != 2)
          throw new RuntimeException();
        uri = XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI;
      }      
      else if (event != null && "XsdNamespace".equals(event.getName())) {
        if (next == 0) {
          m_scanner.putBack(event);
          // load NoNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesNoNamespace);
          continue;
        }
        if (next == 1) {
          m_scanner.putBack(event);
          // load XmlNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesXmlNamespace);
          continue;
        }
        if (next == 2) {
          m_scanner.putBack(event);
          // load XsiNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesXsiNamespace);
          continue;
        }
        if (next != 3)
          throw new RuntimeException();
        uri = XmlUriConst.W3C_2001_XMLSCHEMA_URI;
      }
      else if (event == null || "Namespace".equals(event.getName())) {
        if (next == 0) {
          if (event != null)
            m_scanner.putBack(event);
          // load NoNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesNoNamespace);
          continue;
        }
        if (next == 1) {
          if (event != null)
            m_scanner.putBack(event);
          // load XmlNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesXmlNamespace);
          continue;
        }
        if (next == 2) {
          if (event != null)
            m_scanner.putBack(event);
          // load XsiNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesXsiNamespace);
          continue;
        }
        if (next == 3) {
          if (event != null)
            m_scanner.putBack(event);
          // load XsdNamespace
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureNamesXsdNamespace);
          continue;
        }
        assert next >= 4;
        uri = null;
      }
      else {
        throw new RuntimeException();
      }
      doNamespace(uri, allLocalNames);
      ++next;
    }
    while ((event = nextElement()) != null || next < 4);
    expectEndElement();
    final int n_uris = allLocalNames.size();
    m_localNames = new int[n_uris][];
    for (int i = 0; i < n_uris; i++) {
      m_localNames[i] = allLocalNames.get(i);
    }
  }
  
  private void doNamespace(String uri, ArrayList<int[]> allLocalNames) throws IOException {
    if (uri == null) {
      expectStartElement("Uri");
      uri = readStringContent();
    }
    int uriId = internUri(uri);
    EventDescription event;
    ArrayList<String> localNamesList = new ArrayList<String>(); 
    while ((event = nextElement()) != null) {
      if (!"Name".equals(event.getName())) {
        throw new RuntimeException();
      }
      localNamesList.add(readStringContent());
    }
    expectEndElement();
    final int n_localNames = localNamesList.size();
    final int[] localNames = new int[n_localNames];
    for (int i = 0; i < n_localNames; i++) {
      final String localName = localNamesList.get(i);
      localNames[i] = internName(localName);
    }
    allLocalNames.add(uriId, localNames);
  }
  
  private void processTypes() throws IOException {
    final Set<Integer> typables = new HashSet<Integer>();
    int serial;
    EventDescription event;
    if ((event = nextElement()) != null) {
      final String typeTagName = event.getName();
      if ("MakeTypable".equals(typeTagName)) {
        do {
          serial = readIntContent();
          typables.add(serial);
          if ((event = nextElement()) != null) {
            if (!"MakeTypable".equals(event.getName()))
              m_scanner.putBack(event);
            else
              continue; // another <MakeTypable>
          }
          break;
        }
        while (true);
        m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureTypesBytes);
      }
      else {
        // "AnyType" or one of ArcheTypes
        m_scanner.putBack(event);
        if (!"AnyType".equals(typeTagName))
          m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureTypesBytes);
      }
    }
    else {
      m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureTypesBytes);
    }
    
    final ArrayList<Integer> typesInTypes = new ArrayList<Integer>(); 
    serial = 0;
    event = nextElement();
    if ("AnyType".equals(event.getName())) {
      int tp = doComplexType(XmlUriConst.W3C_2001_XMLSCHEMA_URI, "anyType");
      assert tp == 0;
      m_types[tp + EXISchemaLayout.TYPE_NUMBER] = serial++;
      m_grammarsInTypes.add(tp + EXISchemaLayout.TYPE_GRAMMAR);
      m_typePositions.add(tp);
      if ((event = nextElement()) == null || !"AnySimpleType".equals(event.getName()))
        throw new RuntimeException();
      tp = doSimpleType("AnySimpleType", typesInTypes, false);
      m_types[tp + EXISchemaLayout.TYPE_NUMBER] = serial++;
      m_grammarsInTypes.add(tp + EXISchemaLayout.TYPE_GRAMMAR);
      m_typePositions.add(tp);
      for (; (event = nextElement()) != null; serial++) {
        final String typeTagName = event.getName(); 
        if (serial == 0 || "ComplexType".equals(typeTagName)) {
          tp = doComplexType();
        }
        else {
          tp = doSimpleType(typeTagName, typesInTypes, typables.contains(serial));
        }
        m_types[tp + EXISchemaLayout.TYPE_NUMBER] = serial;
        m_grammarsInTypes.add(tp + EXISchemaLayout.TYPE_GRAMMAR);
        m_typePositions.add(tp);
      }
    }
    else {
      // REVISIT: Load built-in types implicitly. 
    }
    expectEndElement();
    final int len = typesInTypes.size();
    for (int i = 0; i < len; i++) {
      final int pos = typesInTypes.get(i);
      m_types[pos] = m_typePositions.get(m_types[pos]);
    }
    typesInTypes.clear();
  }
  
  private void doTypeCommon(int tp, boolean forcedTypable) throws IOException {
    doTypeCommon(tp, (String)null, (String)null, forcedTypable);
  }
  
  private void doTypeCommon(int tp, String uri, String localName, boolean forcedTypable) throws IOException {
    EventDescription event;
    if ((event = nextElement()) == null)
      throw new RuntimeException();

    if ("Uri".equals(event.getName())) {
      uri = readStringContent();
      expectStartElement("Name");
      localName = readStringContent();
      if ((event = nextElement()) == null)
        throw new RuntimeException();
    }
    int uriId, localNameId;
    uriId = localNameId = -1;
    if (localName != null) {
      assert uri != null;
      if ((uriId = indexOfUri(uri)) == -1)
        throw new RuntimeException();
      if ((localNameId = indexOfLocalName(localName, uriId)) == -1)
        throw new RuntimeException();
    }
    m_types[tp + EXISchemaLayout.TYPE_NAME] = localNameId;
    m_types[tp + EXISchemaLayout.TYPE_URI]  = uriId;
    
    boolean typable = forcedTypable;
    if ("Typable".equals(event.getName())) {
      typable = true;
      expectEndElement();
      if ((event = nextElement()) == null)
        throw new RuntimeException();
    }
    m_types[tp + EXISchemaLayout.TYPE_TYPABLE] = typable ? 1 : 0;
    
    if (!"Grammar".equals(event.getName()))
      throw new RuntimeException();
    final int gram = readIntContent();
    m_types[tp + EXISchemaLayout.TYPE_GRAMMAR] = gram;
  }

  private int doComplexType() throws IOException {
    return doComplexType((String)null, (String)null);
  }

  private int doComplexType(String uri, String localName) throws IOException {
    assert localName == null && uri == null || "anyType".equals(localName) && XmlUriConst.W3C_2001_XMLSCHEMA_URI.equals(uri);
    final int tp = m_n_types;
    ensureTypes(EXISchemaLayout.SZ_COMPLEX_TYPE);
    m_n_types += EXISchemaLayout.SZ_COMPLEX_TYPE;
    
    doTypeCommon(tp, uri, localName, false);
    int contentDatatype = 0;
    EventDescription event;
    if ((event = nextElement()) != null) {
      if (!"ContentDatatype".equals(event.getName()))
        throw new RuntimeException();
      contentDatatype = readIntContent();
      contentDatatype = m_typePositions.get(contentDatatype);
    }
    m_types[tp + EXISchemaLayout.TYPE_AUX] = contentDatatype;
    expectEndElement(); 
    return tp;
  }
  
  private int doSimpleType(String typeTagName, ArrayList<Integer> typesInTypes, boolean forcedTypable) throws IOException {
    final int tp = m_n_types;
    ensureTypes(EXISchemaLayout.SZ_SIMPLE_TYPE);
    m_n_types += EXISchemaLayout.SZ_SIMPLE_TYPE;

    ++m_n_stypes;
    final int baseType;
    final int pos = tp + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_BASE_TYPE); 
    if ("AnySimpleType".equals(typeTagName)) {
      doTypeCommon(tp, XmlUriConst.W3C_2001_XMLSCHEMA_URI, "anySimpleType", forcedTypable);
      baseType = -1;
    }
    else {
      doTypeCommon(tp, forcedTypable);
      expectStartElement("BaseType");
      baseType = readIntContent();
      assert baseType > 0;
      typesInTypes.add(pos);
    }
    m_types[pos] = baseType;
    
    int auxValue = EXISchemaLayout.TYPE_TYPE_OFFSET_MASK;
    
    byte variety = EXISchema.ATOMIC_SIMPLE_TYPE;
    if ("ListType".equals(typeTagName)) {
      variety = EXISchema.LIST_SIMPLE_TYPE;
      doListType(tp, typesInTypes);
      expectEndElement();
    }
    else if ("UnionType".equals(typeTagName)) {
      variety = EXISchema.UNION_SIMPLE_TYPE;
      expectEndElement();
    }
    else if ("AnySimpleType".equals(typeTagName)) {
      variety = EXISchema.UR_SIMPLE_TYPE;
      expectEndElement();
    }
    else {
      do {
        EventDescription event;
        if ("StringType".equals(typeTagName)) {
          auxValue = doStringType(tp, auxValue);
          if ((event = nextElement()) != null) {
            if ("RestrictedCharset".equals(event.getName()))
              auxValue = doRestrictedCharset(auxValue);
            else
              m_scanner.putBack(event);
          }
        }
        else if ("BooleanType".equals(typeTagName)) {
          auxValue = doBooleanType(tp, auxValue);
          expectEndElement();
          break;
        }
        else if ("DecimalType".equals(typeTagName)) {
        }
        else if ("IntegerType".equals(typeTagName)) {
          auxValue = doIntegerType(tp, auxValue);
        }
        else if ("FloatType".equals(typeTagName)) {
        }
        else if ("DurationType".equals(typeTagName)) {
        }
        else if ("DateTimeType".equals(typeTagName)) {
        }
        else if ("TimeType".equals(typeTagName)) {
        }
        else if ("DateType".equals(typeTagName)) {
        }
        else if ("GYearMonthType".equals(typeTagName)) {
        }
        else if ("GYearType".equals(typeTagName)) {
        }
        else if ("GMonthDayType".equals(typeTagName)) {
        }
        else if ("GDayType".equals(typeTagName)) {
        }
        else if ("GMonthType".equals(typeTagName)) {
        }
        else if ("HexBinaryType".equals(typeTagName)) {
        }
        else if ("Base64BinaryType".equals(typeTagName)) {
        }
        else if ("AnyURIType".equals(typeTagName)) {
        }
        else if ("QNameType".equals(typeTagName)) {
          expectEndElement();
          break;
        }
        else
          assert false;
        if ((event = nextElement()) != null) {
          if ("Enumeration".equals(event.getName())) {
            doEnumerations();
            auxValue |= EXISchemaLayout.SIMPLE_TYPE_HAS_ENUMERATED_VALUES_MASK;
          }
          else
            throw new RuntimeException();
        }
        expectEndElement();
      }
      while (false);
    }
    auxValue |= variety;
    m_types[tp + EXISchemaLayout.TYPE_AUX] = auxValue;
    return tp;
  }
  
  private int doRestrictedCharset(int auxValue) throws IOException {
    int rcsCount = 0;    
    EventDescription event;
    while ((event = nextElement()) != null) {
      final String typeTagName = event.getName();
      if ("StartChar".equals(typeTagName)) {
        final int startChar = readIntContent();
        if ((event = nextElement()) == null || !"EndChar".equals(event.getName()))
          throw new RuntimeException();
        final int endChar = readIntContent();
        if (endChar < startChar)
          throw new RuntimeException();
        for (int ch = startChar; ch <= endChar; ch++) {
          ++rcsCount;
          ensureTypes(1);
          m_types[m_n_types++] = ch;
        }
      }
      else if ("Char".equals(typeTagName)) {
        final int ch = readIntContent();
        ++rcsCount;
        ensureTypes(1);
        m_types[m_n_types++] = ch;
      }
      else
        throw new RuntimeException();
    }
    if (rcsCount == 0 || 255 < rcsCount)
      throw new RuntimeException();
    expectEndElement();
    return auxValue | (rcsCount << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET);
  }
  
  private void doEnumerations() throws IOException {
    ensureTypes(1);
    final int countPosition = m_n_types++;
    int count = 0;
    EventDescription event;
    for (; (event = nextElement()) != null; ++count) {
      ensureTypes(1);
      final int valuePosition = m_n_types++;
      String stringValue;
      final int variant;
      
      final String typeTagName = event.getName();
      if ("String".equals(typeTagName)) {
        stringValue = readStringContent();
        variant = addVariantStringValue(stringValue);
      }
      else if ("Integer".equals(typeTagName)) {
        BigInteger bigInteger = readBigIntegerContent();
        variant = doIntegralVariantValue(bigInteger, 0); 
      }
      else if ("Decimal".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!DecimalValueScriber.instance.doProcess(stringValue, m_scribble, new StringBuilder(), new StringBuilder())) {
          throw new RuntimeException();
        }
        DecimalValueScriber.canonicalizeValue(m_scribble);
        variant = addVariantDecimalValue(DecimalValueScriber.getSign(m_scribble), 
            DecimalValueScriber.getIntegralDigits(m_scribble), 
            DecimalValueScriber.getReverseFractionalDigits(m_scribble));
      }
      else if ("Float".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!FloatValueScriber.instance.doProcess(stringValue, m_scribble, new StringBuilder())) {
          throw new RuntimeException();
        }
        FloatValueScriber.canonicalizeValue(m_scribble);
        variant = addVariantFloatValue(FloatValueScriber.getMantissa(m_scribble), FloatValueScriber.getExponent(m_scribble));
      }
      else if ("Duration".equals(typeTagName)) {
        stringValue = readStringContent();
        variant = addVariantDurationValue(EXISchema.datatypeFactory.newDuration(stringValue));
      }
      else if ("DateTime".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!DateTimeValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        variant = addVariantDateTimeValue(m_scribble.dateTime);
      }
      else if ("Time".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!TimeValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        variant = addVariantDateTimeValue(m_scribble.dateTime);
      }
      else if ("Date".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!DateValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        variant = addVariantDateTimeValue(m_scribble.dateTime);
      }
      else if ("GYearMonth".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!GYearMonthValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        variant = addVariantDateTimeValue(m_scribble.dateTime);
      }
      else if ("GYear".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!GYearValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        variant = addVariantDateTimeValue(m_scribble.dateTime);
      }
      else if ("GMonthDay".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!GMonthDayValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        variant = addVariantDateTimeValue(m_scribble.dateTime);
      }
      else if ("GDay".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!GDayValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        variant = addVariantDateTimeValue(m_scribble.dateTime);
      }
      else if ("GMonth".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!GMonthValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        variant = addVariantDateTimeValue(m_scribble.dateTime);
      }
      else if ("HexBinary".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!HexBinaryValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        final int byteCount = m_scribble.intValue1;
        final byte[] binaryValue = new byte[byteCount];
        System.arraycopy(m_scribble.binaryValue, 0, binaryValue, 0, byteCount);
        variant = addVariantBinaryValue(binaryValue, EXISchema.VARIANT_HEXBIN);
      }
      else if ("Base64Binary".equals(typeTagName)) {
        stringValue = readStringContent();
        if (!Base64BinaryValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
          throw new RuntimeException();
        }
        final int byteCount = m_scribble.intValue1;
        final byte[] binaryValue = new byte[byteCount];
        System.arraycopy(m_scribble.binaryValue, 0, binaryValue, 0, byteCount);
        variant = addVariantBinaryValue(binaryValue, EXISchema.VARIANT_BASE64);
      }
      else
        throw new RuntimeException();
      m_types[valuePosition] = variant;
    }
    if (count == 0)
      throw new RuntimeException();
    m_types[countPosition] = count;
    expectEndElement(); // </Enumeration>
  }
  
  private int doStringType(int tp, int auxValue) throws IOException {
    final EventDescription event;
    int whiteSpace = EXISchema.WHITESPACE_PRESERVE;
    if ((event = nextElement()) != null) {
      if ("Replace".equals(event.getName())) {
        whiteSpace = EXISchema.WHITESPACE_REPLACE;
        expectEndElement();
      }
      else if ("Collapse".equals(event.getName())) {
        whiteSpace = EXISchema.WHITESPACE_COLLAPSE;
        expectEndElement();
      }
      else {
        m_scanner.putBack(event);
      }
    }
    auxValue |= whiteSpace << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET;
    return auxValue;
  }
  
  private void doListType(int tp, ArrayList<Integer> typesInTypes)  throws IOException {
    expectStartElement("ItemType");
    final int itemType = readIntContent();
    final int pos = tp + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_ITEM_TYPE); 
    m_types[pos] = itemType;
    typesInTypes.add(pos);
  }

  private int doBooleanType(int tp, int auxValue) throws IOException {
    final EventDescription event;
    boolean patterned = false;
    if ((event = nextElement()) != null) {
      if ("Patterned".equals(event.getName())) {
        patterned = true;
        expectEndElement();
      }
      else {
        throw new RuntimeException();
      }
    }
    if (patterned)
      auxValue |= EXISchemaLayout.SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_MASK;
    return auxValue;
  }
  
  private int doIntegerType(int tp, int auxValue) throws IOException {
    EventDescription event;
    int integralWidth = EXISchema.INTEGER_CODEC_DEFAULT;
    int minInclusiveFacet = EXISchema.NIL_VALUE;
    if ((event = nextElement()) != null) {
      if ("NonNegative".equals(event.getName())) {
        integralWidth = EXISchema.INTEGER_CODEC_NONNEGATIVE;
        expectEndElement(); // </NonNegative>
      }
      else if ("NBit".equals(event.getName())) {
        integralWidth = readIntContent();
        if (integralWidth < 0 || 12 < integralWidth)
          throw new RuntimeException();
        if ((event = nextElement()) != null) {
          if ("MinInteger".equals(event.getName())) {
            final BigInteger minValue = readBigIntegerContent();
            minInclusiveFacet = doIntegralVariantValue(minValue, integralWidth);
          }
          else
            throw new RuntimeException();
        }
        else
          throw new RuntimeException();
      }
      else
        m_scanner.putBack(event);
    }
    m_types[tp + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE)] = minInclusiveFacet;
    auxValue |= integralWidth << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET;
    return auxValue;
  }
  
  private void processElements() throws IOException {
    EventDescription event;
    int uriId, localNameId;
    for (uriId = localNameId = -1; (event = nextElement()) != null; ) {
      final String elementTagName = event.getName();
      if (uriId == -1 && !"Uri".equals(elementTagName)) {
        throw new RuntimeException();
      }
      if ("Uri".equals(elementTagName)) {
        final String uri = readStringContent();
        if ((uriId = indexOfUri(uri)) == -1)
          throw new RuntimeException();
        expectStartElement("Name");
        final String localName = readStringContent();
        if ((localNameId = indexOfLocalName(localName, uriId)) == -1)
          throw new RuntimeException();
      }
      else {
        if ("GlobalElement".equals(elementTagName))
          doElement(uriId, localNameId, true);
        else if ("LocalElement".equals(elementTagName))
          doElement(uriId, localNameId, false);
        else
          throw new RuntimeException();
      }
    }
    expectEndElement();
  }
  
  private void doElement(int uri, int name, boolean isGlobal) throws IOException {
    final int elem = m_n_elems;
    ensureElems(EXISchemaLayout.SZ_ELEM);
    m_n_elems += EXISchemaLayout.SZ_ELEM;

    m_elems[elem + EXISchemaLayout.INODE_NAME] = name;
    m_elems[elem + EXISchemaLayout.INODE_URI]  = uri;

    expectStartElement("Type");
    int serial = readIntContent();
    int tp = m_typePositions.get(serial);
    if (isGlobal)
      tp = (0 - tp) - 1; // minus one in case tp was 0
    m_elems[elem + EXISchemaLayout.INODE_TYPE] = tp;
    final boolean nillable;
    EventDescription event;
    if ((event = nextElement()) != null) {
      if (!"Nillable".equals(event.getName()))
        throw new RuntimeException();
      nillable = true;
      expectEndElement();
    }
    else {
      nillable = false;
    }
    m_elems[elem + EXISchemaLayout.ELEM_NILLABLE] = nillable ? 1 : 0;
    expectEndElement();
  }

  private void processAttributes() throws IOException {
    EventDescription event;
    int uriId, localNameId;
    for (uriId = localNameId = -1; (event = nextElement()) != null; ) {
      final String attributeTagName = event.getName();
      if (uriId == -1 && !"Uri".equals(attributeTagName)) {
        throw new RuntimeException();
      }
      if ("Uri".equals(attributeTagName)) {
        final String uri = readStringContent();
        if ((uriId = indexOfUri(uri)) == -1)
          throw new RuntimeException();
        expectStartElement("Name");
        final String localName = readStringContent();
        if ((localNameId = indexOfLocalName(localName, uriId)) == -1)
          throw new RuntimeException();
      }
      else {
        if ("GlobalAttribute".equals(attributeTagName))
          doAttribute(uriId, localNameId, true);
        else if ("LocalAttribute".equals(attributeTagName))
          doAttribute(uriId, localNameId, false);
        else
          throw new RuntimeException();
      }
    }
    expectEndElement();
  }
  
  private void doAttribute(int uri, int name, boolean isGlobal) throws IOException {
    final int attr = m_n_attrs;
    ensureAttrs(EXISchemaLayout.SZ_ATTR);
    m_n_attrs += EXISchemaLayout.SZ_ATTR;

    m_attrs[attr + EXISchemaLayout.INODE_NAME] = name;
    m_attrs[attr + EXISchemaLayout.INODE_URI]  = uri;
    
    expectStartElement("Type");
    final int serial = readIntContent();
    int tp = m_typePositions.get(serial);
    if (isGlobal)
      tp = (0 - tp) - 1; // minus one in case tp was 0
    m_attrs[attr + EXISchemaLayout.INODE_TYPE] = tp;
    expectEndElement();
  }

  private void processGrammars() throws IOException {
    final ArrayList<Integer> grammarsInGrammars = new ArrayList<Integer>();
    final ArrayList<Integer> grammarsInProductions = new ArrayList<Integer>();
    int serial = 0;
    EventDescription event;
    
    if ((event = nextElement()) != null) {
      final String grammarTagName = event.getName();
      m_scanner.putBack(event);
      if (!"Fixture".equals(grammarTagName)) {
        m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureGrammarsBytes);
      }
    }
    else {
      m_scanner = new SchemaScannerFacade(m_scanner, m_fixtureGrammarsBytes);
    }
    
    for (; (event = nextElement()) != null; serial++) {
      if (!"Grammar".equals(event.getName()) && !"Fixture".equals(event.getName()))
        throw new RuntimeException();
      doGrammar(serial, grammarsInGrammars, grammarsInProductions);
    }
    expectEndElement(); // </Grammars>
    for (int i = 0; i < grammarsInGrammars.size(); i++) {
      final int pos = grammarsInGrammars.get(i);
      m_grammars[pos] = m_gramPositions.get(m_grammars[pos]);
    }
    for (int i = 0; i < grammarsInProductions.size(); i++) {
      final int pos = grammarsInProductions.get(i);
      m_productions[pos] = m_gramPositions.get(m_productions[pos]);
    }
  }
  
  private void doGrammar(int serial, ArrayList<Integer> grammarsInGrammars, 
      ArrayList<Integer> grammarsInProductions) 
    throws IOException {
    ++m_grammarCount;
    boolean hasEndElement = false;
    expectStartElement("Productions");
    final ArrayList<Integer> productionsInGrammar = new ArrayList<Integer>();
    EventDescription event;
    while ((event = nextElement()) != null) {
      final String productionTagName = event.getName();
      if ("EndElement".equals(productionTagName)) {
        hasEndElement = true;
        expectEndElement();
        continue;
      }
      else {
        final int eventType;
        if ("AttributeWildcard".equals(productionTagName)) {
          eventType = EXISchema.EVENT_AT_WILDCARD;
          expectEndElement();
        }
        else if ("ElementWildcard".equals(productionTagName)) {
          eventType = EXISchema.EVENT_SE_WILDCARD;
          expectEndElement();
        }
        else if ("CharactersMixed".equals(productionTagName)) {
          eventType = EXISchema.EVENT_CH_UNTYPED;
          expectEndElement();
        }
        else if ("CharactersTyped".equals(productionTagName)) {
          eventType = EXISchema.EVENT_CH_TYPED;
          expectEndElement();
        }
        else if ("Attribute".equals(productionTagName)){
          final int val = readIntContent(); // attribute serial
          final int attr = EXISchemaLayout.SZ_ATTR * val;
          if (attr < 0 || m_n_attrs <= attr)
            throw new RuntimeException();
          eventType = addEvent(EXISchema.EVENT_TYPE_AT, attr);
        }
        else if ("Element".equals(productionTagName)){
          final int val = readIntContent(); // element serial
          final int elem = EXISchemaLayout.SZ_ELEM * val;
          if (elem < 0 || m_n_elems <= elem)
            throw new RuntimeException();
          eventType = addEvent(EXISchema.EVENT_TYPE_SE, elem);
        }
        else if ("AttributeWildcardNS".equals(productionTagName)){
          final String val = readStringContent();
          eventType = addEvent(EXISchema.EVENT_TYPE_AT_WILDCARD_NS, val);
        }
        else if ("ElementWildcardNS".equals(productionTagName)){
          final String val = readStringContent();
          eventType = addEvent(EXISchema.EVENT_TYPE_SE_WILDCARD_NS, val);
        }
        else
          throw new RuntimeException();

        if ((event = nextElement()) == null || !"Grammar".equals(event.getName()))
          throw new RuntimeException();
        final int subsequentGram = readIntContent();
        if (subsequentGram < 0)
          throw new RuntimeException();
        
        final long productionComposite = ((long)(eventType + (0 - EXISchema.MIN_EVENT_ID)) << 32) | subsequentGram;
        final int prod;
        final Integer _prod;
        if ((_prod = m_productionMap.get(productionComposite)) != null) {
          prod = _prod.intValue();
        }
        else {
          prod = m_n_productions;
          ensureProduction();
          m_n_productions += EXISchemaLayout.SZ_PRODUCTION;
          m_productionMap.put(productionComposite, prod);
          
          m_productions[prod + EXISchemaLayout.PRODUCTION_EVENT] = eventType;
          m_productions[prod + EXISchemaLayout.PRODUCTION_GRAMMAR] = subsequentGram;
          grammarsInProductions.add(prod + EXISchemaLayout.PRODUCTION_GRAMMAR);
        }
        productionsInGrammar.add(prod);
      }
    }
    expectEndElement(); // </Productions>
    boolean hasContentGrammar = false;
    boolean hasEmptyGrammar = false;
    int contentGrammar, emptyGrammar;
    contentGrammar = emptyGrammar = -1;
    if ((event = nextElement()) != null) {
      if (!"ContentGrammar".equals(event.getName()))
        throw new RuntimeException();
      hasContentGrammar = true;
      contentGrammar = readIntContent();
      if ((event = nextElement()) != null) {
        if (!"EmptyGrammar".equals(event.getName()))
          throw new RuntimeException();
        hasEmptyGrammar = true;
        emptyGrammar = readIntContent();
      }
    }
    assert !hasEmptyGrammar || hasContentGrammar;
    
    final int gram = m_n_grammars;
    int sz = EXISchemaLayout.SZ_GRAMMAR;
    if (hasContentGrammar) {
      ++sz;
      if (hasEmptyGrammar)
        ++sz;
    }
    int pos = gram + sz;
    final int n_prods = productionsInGrammar.size();
    sz += n_prods;
    ensureGrammar(sz);
    m_n_grammars += sz;
    
    m_grammars[gram + EXISchemaLayout.GRAMMAR_NUMBER] = serial;
    int fieldInt = n_prods;
    if (hasEndElement)
      fieldInt |= EXISchemaLayout.GRAMMAR_HAS_END_ELEMENT_MASK;
    if (hasContentGrammar) {
      fieldInt |= EXISchemaLayout.GRAMMAR_HAS_CONTENT_GRAMMAR_MASK;
      final int posContentGrammar = gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_CONTENT_GRAMMAR); 
      m_grammars[posContentGrammar] = contentGrammar;
      grammarsInGrammars.add(posContentGrammar);
      if (hasEmptyGrammar) {
        fieldInt |= EXISchemaLayout.GRAMMAR_HAS_EMPTY_GRAMMAR_MASK;
        final int posEmptyGrammar = gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_EMPTY_GRAMMAR); 
        m_grammars[posEmptyGrammar] = emptyGrammar;
        grammarsInGrammars.add(posEmptyGrammar);
      }
    }
    m_grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] = fieldInt;
    
    // Populate productions
    for (int i = 0; i < n_prods; i++, pos++)
      m_grammars[pos] =  productionsInGrammar.get(i);
    
    m_gramPositions.add(gram);
    expectEndElement(); // </Grammar>
  }

  private EventType expectStartElement(String name) throws IOException {
    final EventDescription event = nextElement();
    if (event == null || !name.equals(event.getName()))
      throw new RuntimeException();
    return event.getEventType();
  }

  private void expectEndElement() throws IOException {
    EventDescription event = nextElement();
    if (event != null)
      throw new RuntimeException();
    else {
      event = m_scanner.nextEvent();
      if (event.getEventKind() != EventDescription.EVENT_EE)
        throw new RuntimeException();
    }
  }

  private EventDescription nextElement() throws IOException {
    EventDescription event;
    while ((event = m_scanner.nextEvent()) != null) {
      switch (event.getEventKind()) {
        case EventDescription.EVENT_CH:
          final Characters characters = event.getCharacters();
          if (characters.length != 0) {
            final int limit = characters.startIndex + characters.length;
            for (int i = characters.startIndex; i < limit; i++) {
              final char c = characters.characters[i];
              if (c != ' ' && c != '\t' && c != '\n' && c != '\r')
                throw new RuntimeException();
            }
          }
        case EventDescription.EVENT_SD:
        case EventDescription.EVENT_NS:
        case EventDescription.EVENT_PI:
        case EventDescription.EVENT_CM:
        case EventDescription.EVENT_ER:
        case EventDescription.EVENT_DTD:
          // The above events are simply ignored.
          continue;
        case EventDescription.EVENT_SE:
          if (!"urn:publicid:nagasena".equals(event.getURI()))
            throw new RuntimeException();
          return event;
        default:
          m_scanner.putBack(event);
          return null;
      }
    }
    throw new RuntimeException();
  }

  /**
   * Read an int value that terminates with EE.
   */
  private int readIntContent() throws IOException {
    return Integer.parseInt(readStringContent());
  }
  
  /**
   * Read a BigInteger value that terminates with EE.
   */
  private BigInteger readBigIntegerContent() throws IOException {
    return new BigInteger(readStringContent());
  }
  
  /**
   * Read a string value that terminates with EE.
   */
  private String readStringContent() throws IOException {
    String value = "";
    EventDescription event;
    while ((event = m_scanner.nextEvent()) != null) {
      switch (event.getEventKind()) {
        case EventDescription.EVENT_CH:
          value = value + event.getCharacters().makeString();
          continue;
        case EventDescription.EVENT_EE:
          return value;
        case EventDescription.EVENT_NS:
        case EventDescription.EVENT_PI:
        case EventDescription.EVENT_CM:
        case EventDescription.EVENT_ER:
        case EventDescription.EVENT_DTD:
          continue;
        // Unexpected events. 
        case EventDescription.EVENT_SD:
        case EventDescription.EVENT_ED:
        case EventDescription.EVENT_SE:
        case EventDescription.EVENT_AT:
        case EventDescription.EVENT_TP:
        case EventDescription.EVENT_NL:
        default:
          throw new RuntimeException();
      }
    }
    throw new RuntimeException();
  }
  
  private static abstract class SchemaScanner {
    public EventDescription event;
    
    SchemaScanner() {
      event = null;
    }
    
    public abstract EventDescription nextEvent() throws IOException;
    
    public final void putBack(EventDescription event) {
      assert this.event == null;
      this.event = event;
    }
    
    public static SchemaScanner newScanner(final Scanner scanner) {
      return new SchemaScanner() {
        private final Scanner m_scanner = scanner;
        public EventDescription nextEvent() throws IOException {
          if (event != null) {
            EventDescription _nextEvent = event;
            event = null;
            return _nextEvent;
          }
          return m_scanner.nextEvent();
        }
      };
    }
  }
  
  private final class SchemaScannerFacade extends SchemaScanner {
    private final Scanner m_fragmentScanner;
    private final SchemaScanner m_schemaScanner;

    SchemaScannerFacade(SchemaScanner schemaScanner, byte[] bts)  {
      m_schemaScanner = schemaScanner;
      
      EXIDecoder decoder = new EXIDecoder();
      decoder.setInputStream(new ByteArrayInputStream(bts));
      Scanner scanner = null;
      try {
        decoder.setGrammarCache(new GrammarCache(GrammarSchema.getEXISchema(), GrammarOptions.STRICT_OPTIONS));
        decoder.setFragment(true);
        scanner = decoder.processHeader();
      }
      catch (Exception e) {
      }
      
      m_fragmentScanner = scanner;
      try {
        EventDescription event;
        event = m_fragmentScanner.nextEvent();
        assert event.getEventKind() == EventDescription.EVENT_SD;
      }
      catch (IOException ioe) {
        
      }
    }
    
    public EventDescription nextEvent() throws IOException {
      if (event != null) {
        EventDescription _nextEvent = event;
        event = null;
        return _nextEvent;
      }
      EventDescription nextEvent;
      if ((nextEvent = m_fragmentScanner.nextEvent()) != null && nextEvent.getEventKind() != EventDescription.EVENT_ED) {
        return nextEvent;
      }
      else
        return m_schemaScanner.nextEvent();
    }
  }
  
}
