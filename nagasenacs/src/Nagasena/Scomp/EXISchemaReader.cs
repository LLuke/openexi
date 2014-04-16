using System;
using System.IO;
using System.Diagnostics;
using System.Collections.Generic;
using System.Numerics;
using System.Reflection;
using System.Text;
using System.Xml;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EXIOptionsException = Nagasena.Proc.Common.EXIOptionsException;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using SchemaId = Nagasena.Proc.Common.SchemaId;
using XmlUriConst = Nagasena.Proc.Common.XmlUriConst;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Base64BinaryValueScriber = Nagasena.Proc.IO.Base64BinaryValueScriber;
using DateTimeValueScriber = Nagasena.Proc.IO.DateTimeValueScriber;
using DateValueScriber = Nagasena.Proc.IO.DateValueScriber;
using DecimalValueScriber = Nagasena.Proc.IO.DecimalValueScriber;
using FloatValueScriber = Nagasena.Proc.IO.FloatValueScriber;
using GDayValueScriber = Nagasena.Proc.IO.GDayValueScriber;
using GMonthDayValueScriber = Nagasena.Proc.IO.GMonthDayValueScriber;
using GMonthValueScriber = Nagasena.Proc.IO.GMonthValueScriber;
using GYearMonthValueScriber = Nagasena.Proc.IO.GYearMonthValueScriber;
using GYearValueScriber = Nagasena.Proc.IO.GYearValueScriber;
using HexBinaryValueScriber = Nagasena.Proc.IO.HexBinaryValueScriber;
using Scanner = Nagasena.Proc.IO.Scanner;
using Scribble = Nagasena.Proc.IO.Scribble;
using Scriber = Nagasena.Proc.IO.Scriber;
using TimeValueScriber = Nagasena.Proc.IO.TimeValueScriber;
using Characters = Nagasena.Schema.Characters;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaLayout = Nagasena.Schema.EXISchemaLayout;
using GrammarSchema = Nagasena.Schema.GrammarSchema;

namespace Nagasena.Scomp {

  /// <summary>
  /// EXISchemaReader parses EXI-encoded EXI Grammar into an EXISchema. 
  /// </summary>
  public sealed class EXISchemaReader : EXISchemaStruct {

    private SchemaScanner m_scanner;
    private readonly Scribble m_scribble;

    private readonly List<int> m_grammarsInTypes;

    private readonly List<int> m_typePositions;
    private readonly List<int> m_gramPositions;

    // Production content composite (event type & subsequent grammar) to production address 
    private readonly Dictionary<long, int> m_productionMap;

    private const string ENCODED_FIXTURE_GRAMMARS = "FixtureGrammars.exi";
    private const string ENCODED_FIXTURE_TYPES = "FixtureTypes.exi";
    private const string ENCODED_FIXTURE_NAMES_NONAMESPACE = "FixtureNamesNoNamespace.exi";
    private const string ENCODED_FIXTURE_NAMES_XMLNAMESPACE = "FixtureNamesXmlNamespace.exi";
    private const string ENCODED_FIXTURE_NAMES_XSINAMESPACE = "FixtureNamesXsiNamespace.exi";
    private const string ENCODED_FIXTURE_NAMES_XSDNAMESPACE = "FixtureNamesXsdNamespace.exi";
    private static readonly byte[] m_fixtureGrammarsBytes = new byte[40];
    private static readonly byte[] m_fixtureTypesBytes = new byte[660];
    private static readonly byte[] m_fixtureNamesNoNamespace = new byte[4];
    private static readonly byte[] m_fixtureNamesXmlNamespace = new byte[60];
    private static readonly byte[] m_fixtureNamesXsiNamespace = new byte[60];
    private static readonly byte[] m_fixtureNamesXsdNamespace = new byte[470];

    private static void loadBytes(string fileName, byte[] bts) {
      String[] resourceNames = Assembly.GetExecutingAssembly().GetManifestResourceNames();
      int i;
      for (i = 0; i < resourceNames.Length; i++) {
        if (resourceNames[i].EndsWith(fileName))
          break;
      }
      if (i < resourceNames.Length) {
        Stream inputStream = Assembly.GetExecutingAssembly().GetManifestResourceStream(resourceNames[i]);
        int pos = 0;
        do {
          int n_bytes;
          if ((n_bytes = inputStream.Read(bts, pos, bts.Length - pos)) == 0) {
            break;
          }
          else {
            if (pos == bts.Length) {
              throw new Exception();
            }
          }
          if ((pos += n_bytes) == bts.Length) {
            break;
          }
        }
        while (true);
      }
    }

    static EXISchemaReader() {
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
      m_grammarsInTypes = new List<int>();
      m_typePositions = new List<int>();
      m_gramPositions = new List<int>();
      m_productionMap = new Dictionary<long, int>();
    }

    protected internal override void reset() {
      // initialize the arrays
      base.reset();
      m_scanner = null;
    }

    protected internal override void clear() {
      base.clear();
      m_grammarsInTypes.Clear();
      m_typePositions.Clear();
      m_gramPositions.Clear();
      m_productionMap.Clear();
    }

    /// <summary>
    /// Parses EXI-encoded EXI Grammar into an EXISchema. </summary>
    /// <param name="inputStream"> EXI-encoded EXI Grammar </param>
    /// <returns> EXISchema </returns>
    /// <exception cref="IOException"> </exception>
    /// <exception cref="EXIOptionsException"> </exception>
    public EXISchema parse(Stream inputStream) {
      try {
        reset();
        EXIDecoder decoder = new EXIDecoder();
        decoder.InputStream = inputStream;
        decoder.GrammarCache = GrammarCache4Grammar.GrammarCache;
        Scanner scanner = decoder.processHeader();
        EXIOptions exiOptions = scanner.HeaderOptions;
        if (exiOptions == null) {
          throw new Exception();
        }
        SchemaId schemaId = exiOptions.SchemaId;
        if (schemaId == null) {
          throw new Exception();
        }
        string schemaName = schemaId.Value;
        if (schemaName != null) {
          if (!"nagasena:grammar".Equals(schemaName)) {
            throw new Exception();
          }
          if (!exiOptions.Strict) {
            throw new Exception();
          }
        }
        m_scanner = SchemaScanner.newScanner(scanner);
        EventType eventType = expectStartElement("EXIGrammar");
        if (schemaName != null && eventType.itemType != EventType.ITEM_SE) {
          throw new Exception();
        }
        return processEXIGrammar();
      }
      finally {
        inputStream.Close();
      }
    }

    private EXISchema processEXIGrammar() {
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

      for (int i = 0; i < m_grammarsInTypes.Count; i++) {
        int pos = m_grammarsInTypes[i];
        m_types[pos] = m_gramPositions[m_types[pos]];
      }

      return new EXISchema(m_elems, m_n_elems, m_attrs, m_n_attrs, m_types, m_n_types, 
        m_uris, m_n_uris, m_names, m_n_names, m_localNames, m_strings, m_n_strings, 
        m_ints, m_n_ints, m_mantissas, m_exponents, m_n_floats, 
        m_signs, m_integralDigits, m_reverseFractionalDigits, m_n_decimals, 
        m_integers, m_n_integers, m_longs, m_n_longs, m_datetimes, m_n_datetimes, 
        m_durations, m_n_durations, m_binaries, m_n_binaries, 
        m_variantTypes, m_variants, m_n_variants, m_grammars, m_n_grammars, m_grammarCount, 
        m_productions, m_n_productions, m_eventTypes, m_eventData, m_n_events, m_n_stypes);
    }

    private void processStringTable() {
      List<int[]> allLocalNames = new List<int[]>();
      EventDescription @event;
      int next = 0;
      @event = nextElement();
      do {
        string uri;
        if (@event != null && "NoNamespace".Equals(@event.Name)) {
          if (next != 0) {
            throw new Exception();
          }
          uri = "";
        }
        else if (@event != null && "XmlNamespace".Equals(@event.Name)) {
          if (next == 0) {
            m_scanner.putBack(@event);
            // load NoNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesNoNamespace);
            continue;
          }
          if (next != 1) {
            throw new Exception();
          }
          uri = XmlUriConst.W3C_XML_1998_URI;
        }
        else if (@event != null && "XsiNamespace".Equals(@event.Name)) {
          if (next == 0) {
            m_scanner.putBack(@event);
            // load NoNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesNoNamespace);
            continue;
          }
          if (next == 1) {
            m_scanner.putBack(@event);
            // load XmlNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesXmlNamespace);
            continue;
          }
          if (next != 2) {
            throw new Exception();
          }
          uri = XmlUriConst.W3C_2001_XMLSCHEMA_INSTANCE_URI;
        }
        else if (@event != null && "XsdNamespace".Equals(@event.Name)) {
          if (next == 0) {
            m_scanner.putBack(@event);
            // load NoNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesNoNamespace);
            continue;
          }
          if (next == 1) {
            m_scanner.putBack(@event);
            // load XmlNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesXmlNamespace);
            continue;
          }
          if (next == 2) {
            m_scanner.putBack(@event);
            // load XsiNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesXsiNamespace);
            continue;
          }
          if (next != 3) {
            throw new Exception();
          }
          uri = XmlUriConst.W3C_2001_XMLSCHEMA_URI;
        }
        else if (@event == null || "Namespace".Equals(@event.Name)) {
          if (next == 0) {
            if (@event != null) {
              m_scanner.putBack(@event);
            }
            // load NoNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesNoNamespace);
            continue;
          }
          if (next == 1) {
            if (@event != null) {
              m_scanner.putBack(@event);
            }
            // load XmlNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesXmlNamespace);
            continue;
          }
          if (next == 2) {
            if (@event != null) {
              m_scanner.putBack(@event);
            }
            // load XsiNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesXsiNamespace);
            continue;
          }
          if (next == 3) {
            if (@event != null) {
              m_scanner.putBack(@event);
            }
            // load XsdNamespace
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureNamesXsdNamespace);
            continue;
          }
          Debug.Assert(next >= 4);
          uri = null;
        }
        else {
          throw new Exception();
        }
        doNamespace(uri, allLocalNames);
        ++next;
      }
      while ((@event = nextElement()) != null || next < 4);
      expectEndElement();
      int n_uris = allLocalNames.Count;
      m_localNames = new int[n_uris][];
      for (int i = 0; i < n_uris; i++) {
        m_localNames[i] = allLocalNames[i];
      }
    }

    private void doNamespace(string uri, List<int[]> allLocalNames) {
      if (uri == null) {
        expectStartElement("Uri");
        uri = readStringContent();
      }
      int uriId = internUri(uri);
      EventDescription @event;
      List<string> localNamesList = new List<string>();
      while ((@event = nextElement()) != null) {
        if (!"Name".Equals(@event.Name)) {
          throw new Exception();
        }
        localNamesList.Add(readStringContent());
      }
      expectEndElement();
      int n_localNames = localNamesList.Count;
      int[] localNames = new int[n_localNames];
      for (int i = 0; i < n_localNames; i++) {
        string localName = localNamesList[i];
        localNames[i] = internName(localName);
      }
      allLocalNames.Insert(uriId, localNames);
    }

    private void processTypes() {
      HashSet<int> typables = new HashSet<int>();
      int serial;
      EventDescription @event;
      if ((@event = nextElement()) != null) {
        string typeTagName = @event.Name;
        if ("MakeTypable".Equals(typeTagName)) {
          do {
            serial = readIntContent();
            typables.Add(serial);
            if ((@event = nextElement()) != null) {
              if (!"MakeTypable".Equals(@event.Name)) {
                m_scanner.putBack(@event);
              }
              else {
                continue; // another <MakeTypable>
              }
            }
            break;
          }
          while (true);
          m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureTypesBytes);
        }
        else {
          // "AnyType" or one of ArcheTypes
          m_scanner.putBack(@event);
          if (!"AnyType".Equals(typeTagName)) {
            m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureTypesBytes);
          }
        }
      }
      else {
        m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureTypesBytes);
      }

      List<int> typesInTypes = new List<int>();
      serial = 0;
      @event = nextElement();
      if ("AnyType".Equals(@event.Name)) {
        int tp = doComplexType(XmlUriConst.W3C_2001_XMLSCHEMA_URI, "anyType");
        Debug.Assert(tp == 0);
        m_types[tp + EXISchemaLayout.TYPE_NUMBER] = serial++;
        m_grammarsInTypes.Add(tp + EXISchemaLayout.TYPE_GRAMMAR);
        m_typePositions.Add(tp);
        if ((@event = nextElement()) == null || !"AnySimpleType".Equals(@event.Name)) {
          throw new Exception();
        }
        tp = doSimpleType("AnySimpleType", typesInTypes, false);
        m_types[tp + EXISchemaLayout.TYPE_NUMBER] = serial++;
        m_grammarsInTypes.Add(tp + EXISchemaLayout.TYPE_GRAMMAR);
        m_typePositions.Add(tp);
        for (; (@event = nextElement()) != null; serial++) {
          string typeTagName = @event.Name;
          if (serial == 0 || "ComplexType".Equals(typeTagName)) {
            tp = doComplexType();
          }
          else {
            tp = doSimpleType(typeTagName, typesInTypes, typables.Contains(serial));
          }
          m_types[tp + EXISchemaLayout.TYPE_NUMBER] = serial;
          m_grammarsInTypes.Add(tp + EXISchemaLayout.TYPE_GRAMMAR);
          m_typePositions.Add(tp);
        }
      }
      else {
        // REVISIT: Load built-in types implicitly. 
      }
      expectEndElement();
      int len = typesInTypes.Count;
      for (int i = 0; i < len; i++) {
        int pos = typesInTypes[i];
        m_types[pos] = m_typePositions[m_types[pos]];
      }
      typesInTypes.Clear();
    }

    private void doTypeCommon(int tp, bool forcedTypable) {
      doTypeCommon(tp, (string)null, (string)null, forcedTypable);
    }

    private void doTypeCommon(int tp, string uri, string localName, bool forcedTypable) {
      EventDescription @event;
      if ((@event = nextElement()) == null) {
        throw new Exception();
      }

      if ("Uri".Equals(@event.Name)) {
        uri = readStringContent();
        expectStartElement("Name");
        localName = readStringContent();
        if ((@event = nextElement()) == null) {
          throw new Exception();
        }
      }
      int uriId, localNameId;
      uriId = localNameId = -1;
      if (localName != null) {
        Debug.Assert(uri != null);
        if ((uriId = indexOfUri(uri)) == -1) {
          throw new Exception();
        }
        if ((localNameId = indexOfLocalName(localName, uriId)) == -1) {
          throw new Exception();
        }
      }
      m_types[tp + EXISchemaLayout.TYPE_NAME] = localNameId;
      m_types[tp + EXISchemaLayout.TYPE_URI] = uriId;

      bool typable = forcedTypable;
      if ("Typable".Equals(@event.Name)) {
        typable = true;
        expectEndElement();
        if ((@event = nextElement()) == null) {
          throw new Exception();
        }
      }
      m_types[tp + EXISchemaLayout.TYPE_TYPABLE] = typable ? 1 : 0;

      if (!"Grammar".Equals(@event.Name)) {
        throw new Exception();
      }
      int gram = readIntContent();
      m_types[tp + EXISchemaLayout.TYPE_GRAMMAR] = gram;
    }

    private int doComplexType() {
      return doComplexType((string)null, (string)null);
    }

    private int doComplexType(string uri, string localName) {
      Debug.Assert(localName == null && uri == null || "anyType".Equals(localName) && XmlUriConst.W3C_2001_XMLSCHEMA_URI.Equals(uri));
      int tp = m_n_types;
      ensureTypes(EXISchemaLayout.SZ_COMPLEX_TYPE);
      m_n_types += EXISchemaLayout.SZ_COMPLEX_TYPE;

      doTypeCommon(tp, uri, localName, false);
      int contentDatatype = 0;
      EventDescription @event;
      if ((@event = nextElement()) != null) {
        if (!"ContentDatatype".Equals(@event.Name)) {
          throw new Exception();
        }
        contentDatatype = readIntContent();
        contentDatatype = m_typePositions[contentDatatype];
      }
      m_types[tp + EXISchemaLayout.TYPE_AUX] = contentDatatype;
      expectEndElement();
      return tp;
    }

    private int doSimpleType(string typeTagName, List<int> typesInTypes, bool forcedTypable) {
      int tp = m_n_types;
      ensureTypes(EXISchemaLayout.SZ_SIMPLE_TYPE);
      m_n_types += EXISchemaLayout.SZ_SIMPLE_TYPE;

      ++m_n_stypes;
      int baseType;
      int pos = tp + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_BASE_TYPE);
      if ("AnySimpleType".Equals(typeTagName)) {
        doTypeCommon(tp, XmlUriConst.W3C_2001_XMLSCHEMA_URI, "anySimpleType", forcedTypable);
        baseType = -1;
      }
      else {
        doTypeCommon(tp, forcedTypable);
        expectStartElement("BaseType");
        baseType = readIntContent();
        Debug.Assert(baseType > 0);
        typesInTypes.Add(pos);
      }
      m_types[pos] = baseType;

      int auxValue = EXISchemaLayout.TYPE_TYPE_OFFSET_MASK;

      sbyte variety = EXISchema.ATOMIC_SIMPLE_TYPE;
      if ("ListType".Equals(typeTagName)) {
        variety = EXISchema.LIST_SIMPLE_TYPE;
        doListType(tp, typesInTypes);
        expectEndElement();
      }
      else if ("UnionType".Equals(typeTagName)) {
        variety = EXISchema.UNION_SIMPLE_TYPE;
        expectEndElement();
      }
      else if ("AnySimpleType".Equals(typeTagName)) {
        variety = EXISchema.UR_SIMPLE_TYPE;
        expectEndElement();
      }
      else {
        do {
          EventDescription @event;
          if ("StringType".Equals(typeTagName)) {
            auxValue = doStringType(tp, auxValue);
            if ((@event = nextElement()) != null) {
              if ("RestrictedCharset".Equals(@event.Name)) {
                auxValue = doRestrictedCharset(auxValue);
              }
              else {
                m_scanner.putBack(@event);
              }
            }
          }
          else if ("BooleanType".Equals(typeTagName)) {
            auxValue = doBooleanType(tp, auxValue);
            expectEndElement();
            break;
          }
          else if ("DecimalType".Equals(typeTagName)) {
          }
          else if ("IntegerType".Equals(typeTagName)) {
            auxValue = doIntegerType(tp, auxValue);
          }
          else if ("FloatType".Equals(typeTagName)) {
          }
          else if ("DurationType".Equals(typeTagName)) {
          }
          else if ("DateTimeType".Equals(typeTagName)) {
          }
          else if ("TimeType".Equals(typeTagName)) {
          }
          else if ("DateType".Equals(typeTagName)) {
          }
          else if ("GYearMonthType".Equals(typeTagName)) {
          }
          else if ("GYearType".Equals(typeTagName)) {
          }
          else if ("GMonthDayType".Equals(typeTagName)) {
          }
          else if ("GDayType".Equals(typeTagName)) {
          }
          else if ("GMonthType".Equals(typeTagName)) {
          }
          else if ("HexBinaryType".Equals(typeTagName)) {
          }
          else if ("Base64BinaryType".Equals(typeTagName)) {
          }
          else if ("AnyURIType".Equals(typeTagName)) {
          }
          else if ("QNameType".Equals(typeTagName)) {
            expectEndElement();
            break;
          }
          else {
            Debug.Assert(false);
          }
          if ((@event = nextElement()) != null) {
            if ("Enumeration".Equals(@event.Name)) {
              doEnumerations();
              auxValue |= EXISchemaLayout.SIMPLE_TYPE_HAS_ENUMERATED_VALUES_MASK;
            }
            else {
              throw new Exception();
            }
          }
          expectEndElement();
        }
        while (false);
      }
      auxValue |= (byte)variety;
      m_types[tp + EXISchemaLayout.TYPE_AUX] = auxValue;
      return tp;
    }

    private int doRestrictedCharset(int auxValue) {
      int rcsCount = 0;
      EventDescription @event;
      while ((@event = nextElement()) != null) {
        string typeTagName = @event.Name;
        if ("StartChar".Equals(typeTagName)) {
          int startChar = readIntContent();
          if ((@event = nextElement()) == null || !"EndChar".Equals(@event.Name)) {
            throw new Exception();
          }
          int endChar = readIntContent();
          if (endChar < startChar) {
            throw new Exception();
          }
          for (int ch = startChar; ch <= endChar; ch++) {
            ++rcsCount;
            ensureTypes(1);
            m_types[m_n_types++] = ch;
          }
        }
        else if ("Char".Equals(typeTagName)) {
          int ch = readIntContent();
          ++rcsCount;
          ensureTypes(1);
          m_types[m_n_types++] = ch;
        }
        else {
          throw new Exception();
        }
      }
      if (rcsCount == 0 || 255 < rcsCount) {
        throw new Exception();
      }
      expectEndElement();
      return auxValue | (rcsCount << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_N_RESTRICTED_CHARSET_OFFSET);
    }

    private void doEnumerations() {
      ensureTypes(1);
      int countPosition = m_n_types++;
      int count = 0;
      EventDescription @event;
      for (; (@event = nextElement()) != null; ++count) {
        ensureTypes(1);
        int valuePosition = m_n_types++;
        string stringValue;
        int variant;

        string typeTagName = @event.Name;
        if ("String".Equals(typeTagName)) {
          stringValue = readStringContent();
          variant = addVariantStringValue(stringValue);
        }
        else if ("Integer".Equals(typeTagName)) {
          BigInteger bigInteger = readBigIntegerContent();
          variant = doIntegralVariantValue(bigInteger, 0);
        }
        else if ("Decimal".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!DecimalValueScriber.instance.doProcess(stringValue, m_scribble, new StringBuilder(), new StringBuilder())) {
            throw new Exception();
          }
          DecimalValueScriber.canonicalizeValue(m_scribble);
          variant = addVariantDecimalValue(DecimalValueScriber.getSign(m_scribble), 
            DecimalValueScriber.getIntegralDigits(m_scribble), DecimalValueScriber.getReverseFractionalDigits(m_scribble));
        }
        else if ("Float".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!FloatValueScriber.instance.doProcess(stringValue, m_scribble, new StringBuilder())) {
            throw new Exception();
          }
          FloatValueScriber.canonicalizeValue(m_scribble);
          variant = addVariantFloatValue(FloatValueScriber.getMantissa(m_scribble), FloatValueScriber.getExponent(m_scribble));
        }
        else if ("Duration".Equals(typeTagName)) {
          stringValue = readStringContent();
          variant = addVariantDurationValue(XmlConvert.ToTimeSpan(stringValue));
        }
        else if ("DateTime".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!DateTimeValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          variant = addVariantDateTimeValue(m_scribble.dateTime);
        }
        else if ("Time".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!TimeValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          variant = addVariantDateTimeValue(m_scribble.dateTime);
        }
        else if ("Date".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!DateValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          variant = addVariantDateTimeValue(m_scribble.dateTime);
        }
        else if ("GYearMonth".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!GYearMonthValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          variant = addVariantDateTimeValue(m_scribble.dateTime);
        }
        else if ("GYear".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!GYearValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          variant = addVariantDateTimeValue(m_scribble.dateTime);
        }
        else if ("GMonthDay".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!GMonthDayValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          variant = addVariantDateTimeValue(m_scribble.dateTime);
        }
        else if ("GDay".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!GDayValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          variant = addVariantDateTimeValue(m_scribble.dateTime);
        }
        else if ("GMonth".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!GMonthValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          variant = addVariantDateTimeValue(m_scribble.dateTime);
        }
        else if ("HexBinary".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!HexBinaryValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          int byteCount = m_scribble.intValue1;
          sbyte[] binaryValue = new sbyte[byteCount];
          Array.Copy(m_scribble.binaryValue, 0, binaryValue, 0, byteCount);
          variant = addVariantBinaryValue(binaryValue, EXISchema.VARIANT_HEXBIN);
        }
        else if ("Base64Binary".Equals(typeTagName)) {
          stringValue = readStringContent();
          if (!Base64BinaryValueScriber.instance.process(stringValue, EXISchema.NIL_NODE, (EXISchema)null, m_scribble, (Scriber)null)) {
            throw new Exception();
          }
          int byteCount = m_scribble.intValue1;
          sbyte[] binaryValue = new sbyte[byteCount];
          Array.Copy(m_scribble.binaryValue, 0, binaryValue, 0, byteCount);
          variant = addVariantBinaryValue(binaryValue, EXISchema.VARIANT_BASE64);
        }
        else {
          throw new Exception();
        }
        m_types[valuePosition] = variant;
      }
      if (count == 0) {
        throw new Exception();
      }
      m_types[countPosition] = count;
      expectEndElement(); // </Enumeration>
    }

    private int doStringType(int tp, int auxValue) {
      EventDescription @event;
      int whiteSpace = EXISchema.WHITESPACE_PRESERVE;
      if ((@event = nextElement()) != null) {
        if ("Replace".Equals(@event.Name)) {
          whiteSpace = EXISchema.WHITESPACE_REPLACE;
          expectEndElement();
        }
        else if ("Collapse".Equals(@event.Name)) {
          whiteSpace = EXISchema.WHITESPACE_COLLAPSE;
          expectEndElement();
        }
        else {
          m_scanner.putBack(@event);
        }
      }
      auxValue |= whiteSpace << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_STRING_WHITESPACE_OFFSET;
      return auxValue;
    }

    private void doListType(int tp, List<int> typesInTypes) {
      expectStartElement("ItemType");
      int itemType = readIntContent();
      int pos = tp + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_ITEM_TYPE);
      m_types[pos] = itemType;
      typesInTypes.Add(pos);
    }

    private int doBooleanType(int tp, int auxValue) {
      EventDescription @event;
      bool patterned = false;
      if ((@event = nextElement()) != null) {
        if ("Patterned".Equals(@event.Name)) {
          patterned = true;
          expectEndElement();
        }
        else {
          throw new Exception();
        }
      }
      if (patterned) {
        auxValue |= EXISchemaLayout.SIMPLE_TYPE_ATOMIC_BOOLEAN_HAS_PATTERN_MASK;
      }
      return auxValue;
    }

    private int doIntegerType(int tp, int auxValue) {
      EventDescription @event;
      int integralWidth = EXISchema.INTEGER_CODEC_DEFAULT;
      int minInclusiveFacet = EXISchema.NIL_VALUE;
      if ((@event = nextElement()) != null) {
        if ("NonNegative".Equals(@event.Name)) {
          integralWidth = EXISchema.INTEGER_CODEC_NONNEGATIVE;
          expectEndElement(); // </NonNegative>
        }
        else if ("NBit".Equals(@event.Name)) {
          integralWidth = readIntContent();
          if (integralWidth < 0 || 12 < integralWidth) {
            throw new Exception();
          }
          if ((@event = nextElement()) != null) {
            if ("MinInteger".Equals(@event.Name)) {
              BigInteger minValue = readBigIntegerContent();
              minInclusiveFacet = doIntegralVariantValue(minValue, integralWidth);
            }
            else {
              throw new Exception();
            }
          }
          else {
            throw new Exception();
          }
        }
        else {
          m_scanner.putBack(@event);
        }
      }
      m_types[tp + (EXISchemaLayout.SZ_TYPE + EXISchemaLayout.SIMPLE_TYPE_FACET_MININCLUSIVE)] = minInclusiveFacet;
      auxValue |= integralWidth << EXISchemaLayout.SIMPLE_TYPE_ATOMIC_INTEGER_WIDTH_OFFSET;
      return auxValue;
    }

    private void processElements() {
      EventDescription @event;
      int uriId, localNameId;
      for (uriId = localNameId = -1; (@event = nextElement()) != null;) {
        string elementTagName = @event.Name;
        if (uriId == -1 && !"Uri".Equals(elementTagName)) {
          throw new Exception();
        }
        if ("Uri".Equals(elementTagName)) {
          string uri = readStringContent();
          if ((uriId = indexOfUri(uri)) == -1) {
            throw new Exception();
          }
          expectStartElement("Name");
          string localName = readStringContent();
          if ((localNameId = indexOfLocalName(localName, uriId)) == -1) {
            throw new Exception();
          }
        }
        else {
          if ("GlobalElement".Equals(elementTagName)) {
            doElement(uriId, localNameId, true);
          }
          else if ("LocalElement".Equals(elementTagName)) {
            doElement(uriId, localNameId, false);
          }
          else {
            throw new Exception();
          }
        }
      }
      expectEndElement();
    }

    private void doElement(int uri, int name, bool isGlobal) {
      int elem = m_n_elems;
      ensureElems(EXISchemaLayout.SZ_ELEM);
      m_n_elems += EXISchemaLayout.SZ_ELEM;

      m_elems[elem + EXISchemaLayout.INODE_NAME] = name;
      m_elems[elem + EXISchemaLayout.INODE_URI] = uri;

      expectStartElement("Type");
      int serial = readIntContent();
      int tp = m_typePositions[serial];
      if (isGlobal) {
        tp = (0 - tp) - 1; // minus one in case tp was 0
      }
      m_elems[elem + EXISchemaLayout.INODE_TYPE] = tp;
      bool nillable;
      EventDescription @event;
      if ((@event = nextElement()) != null) {
        if (!"Nillable".Equals(@event.Name)) {
          throw new Exception();
        }
        nillable = true;
        expectEndElement();
      }
      else {
        nillable = false;
      }
      m_elems[elem + EXISchemaLayout.ELEM_NILLABLE] = nillable ? 1 : 0;
      expectEndElement();
    }

    private void processAttributes() {
      EventDescription @event;
      int uriId, localNameId;
      for (uriId = localNameId = -1; (@event = nextElement()) != null;) {
        string attributeTagName = @event.Name;
        if (uriId == -1 && !"Uri".Equals(attributeTagName)) {
          throw new Exception();
        }
        if ("Uri".Equals(attributeTagName)) {
          string uri = readStringContent();
          if ((uriId = indexOfUri(uri)) == -1) {
            throw new Exception();
          }
          expectStartElement("Name");
          string localName = readStringContent();
          if ((localNameId = indexOfLocalName(localName, uriId)) == -1) {
            throw new Exception();
          }
        }
        else {
          if ("GlobalAttribute".Equals(attributeTagName)) {
            doAttribute(uriId, localNameId, true);
          }
          else if ("LocalAttribute".Equals(attributeTagName)) {
            doAttribute(uriId, localNameId, false);
          }
          else {
            throw new Exception();
          }
        }
      }
      expectEndElement();
    }

    private void doAttribute(int uri, int name, bool isGlobal) {
      int attr = m_n_attrs;
      ensureAttrs(EXISchemaLayout.SZ_ATTR);
      m_n_attrs += EXISchemaLayout.SZ_ATTR;

      m_attrs[attr + EXISchemaLayout.INODE_NAME] = name;
      m_attrs[attr + EXISchemaLayout.INODE_URI] = uri;

      expectStartElement("Type");
      int serial = readIntContent();
      int tp = m_typePositions[serial];
      if (isGlobal) {
        tp = (0 - tp) - 1; // minus one in case tp was 0
      }
      m_attrs[attr + EXISchemaLayout.INODE_TYPE] = tp;
      expectEndElement();
    }

    private void processGrammars() {
      List<int> grammarsInGrammars = new List<int>();
      List<int> grammarsInProductions = new List<int>();
      int serial = 0;
      EventDescription @event;

      if ((@event = nextElement()) != null) {
        string grammarTagName = @event.Name;
        m_scanner.putBack(@event);
        if (!"Fixture".Equals(grammarTagName)) {
          m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureGrammarsBytes);
        }
      }
      else {
        m_scanner = new SchemaScannerFacade(this, m_scanner, m_fixtureGrammarsBytes);
      }

      for (; (@event = nextElement()) != null; serial++) {
        if (!"Grammar".Equals(@event.Name) && !"Fixture".Equals(@event.Name)) {
          throw new Exception();
        }
        doGrammar(serial, grammarsInGrammars, grammarsInProductions);
      }
      expectEndElement(); // </Grammars>
      for (int i = 0; i < grammarsInGrammars.Count; i++) {
        int pos = grammarsInGrammars[i];
        m_grammars[pos] = m_gramPositions[m_grammars[pos]];
      }
      for (int i = 0; i < grammarsInProductions.Count; i++) {
        int pos = grammarsInProductions[i];
        m_productions[pos] = m_gramPositions[m_productions[pos]];
      }
    }

    private void doGrammar(int serial, List<int> grammarsInGrammars, List<int> grammarsInProductions) {
      ++m_grammarCount;
      bool hasEndElement = false;
      expectStartElement("Productions");
      List<int> productionsInGrammar = new List<int>();
      EventDescription @event;
      while ((@event = nextElement()) != null) {
        string productionTagName = @event.Name;
        if ("EndElement".Equals(productionTagName)) {
          hasEndElement = true;
          expectEndElement();
          continue;
        }
        else {
          int eventType;
          if ("AttributeWildcard".Equals(productionTagName)) {
            eventType = EXISchema.EVENT_AT_WILDCARD;
            expectEndElement();
          }
          else if ("ElementWildcard".Equals(productionTagName)) {
            eventType = EXISchema.EVENT_SE_WILDCARD;
            expectEndElement();
          }
          else if ("CharactersMixed".Equals(productionTagName)) {
            eventType = EXISchema.EVENT_CH_UNTYPED;
            expectEndElement();
          }
          else if ("CharactersTyped".Equals(productionTagName)) {
            eventType = EXISchema.EVENT_CH_TYPED;
            expectEndElement();
          }
          else if ("Attribute".Equals(productionTagName)) {
            int val = readIntContent(); // attribute serial
            int attr = EXISchemaLayout.SZ_ATTR * val;
            if (attr < 0 || m_n_attrs <= attr) {
              throw new Exception();
            }
            eventType = addEvent(EXISchema.EVENT_TYPE_AT, attr);
          }
          else if ("Element".Equals(productionTagName)) {
            int val = readIntContent(); // element serial
            int elem = EXISchemaLayout.SZ_ELEM * val;
            if (elem < 0 || m_n_elems <= elem) {
              throw new Exception();
            }
            eventType = addEvent(EXISchema.EVENT_TYPE_SE, elem);
          }
          else if ("AttributeWildcardNS".Equals(productionTagName)) {
            string val = readStringContent();
            eventType = addEvent(EXISchema.EVENT_TYPE_AT_WILDCARD_NS, val);
          }
          else if ("ElementWildcardNS".Equals(productionTagName)) {
            string val = readStringContent();
            eventType = addEvent(EXISchema.EVENT_TYPE_SE_WILDCARD_NS, val);
          }
          else {
            throw new Exception();
          }

          if ((@event = nextElement()) == null || !"Grammar".Equals(@event.Name)) {
            throw new Exception();
          }
          int subsequentGram = readIntContent();
          if (subsequentGram < 0) {
            throw new Exception();
          }

          long productionComposite = (((long)(eventType + (0 - EXISchema.MIN_EVENT_ID)) << 32) | (uint)subsequentGram);
          int prod, _prod;
          try {
            _prod = m_productionMap[productionComposite];
          }
          catch (KeyNotFoundException) {
            _prod = -1;
          }
          if (_prod != -1) {
            prod = _prod;
          }
          else {
            prod = m_n_productions;
            ensureProduction();
            m_n_productions += EXISchemaLayout.SZ_PRODUCTION;
            m_productionMap[productionComposite] = prod;

            m_productions[prod + EXISchemaLayout.PRODUCTION_EVENT] = eventType;
            m_productions[prod + EXISchemaLayout.PRODUCTION_GRAMMAR] = subsequentGram;
            grammarsInProductions.Add(prod + EXISchemaLayout.PRODUCTION_GRAMMAR);
          }
          productionsInGrammar.Add(prod);
        }
      }
      expectEndElement(); // </Productions>
      bool hasContentGrammar = false;
      bool hasEmptyGrammar = false;
      int contentGrammar, emptyGrammar;
      contentGrammar = emptyGrammar = -1;
      if ((@event = nextElement()) != null) {
        if (!"ContentGrammar".Equals(@event.Name)) {
          throw new Exception();
        }
        hasContentGrammar = true;
        contentGrammar = readIntContent();
        if ((@event = nextElement()) != null) {
          if (!"EmptyGrammar".Equals(@event.Name)) {
            throw new Exception();
          }
          hasEmptyGrammar = true;
          emptyGrammar = readIntContent();
        }
      }
      Debug.Assert(!hasEmptyGrammar || hasContentGrammar);

      int gram = m_n_grammars;
      int sz = EXISchemaLayout.SZ_GRAMMAR;
      if (hasContentGrammar) {
        ++sz;
        if (hasEmptyGrammar) {
          ++sz;
        }
      }
      int pos = gram + sz;
      int n_prods = productionsInGrammar.Count;
      sz += n_prods;
      ensureGrammar(sz);
      m_n_grammars += sz;

      m_grammars[gram + EXISchemaLayout.GRAMMAR_NUMBER] = serial;
      int fieldInt = n_prods;
      if (hasEndElement) {
        fieldInt |= EXISchemaLayout.GRAMMAR_HAS_END_ELEMENT_MASK;
      }
      if (hasContentGrammar) {
        fieldInt |= EXISchemaLayout.GRAMMAR_HAS_CONTENT_GRAMMAR_MASK;
        int posContentGrammar = gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_CONTENT_GRAMMAR);
        m_grammars[posContentGrammar] = contentGrammar;
        grammarsInGrammars.Add(posContentGrammar);
        if (hasEmptyGrammar) {
          fieldInt |= EXISchemaLayout.GRAMMAR_HAS_EMPTY_GRAMMAR_MASK;
          int posEmptyGrammar = gram + (EXISchemaLayout.SZ_GRAMMAR + EXISchemaLayout.GRAMMAR_EXT_EMPTY_GRAMMAR);
          m_grammars[posEmptyGrammar] = emptyGrammar;
          grammarsInGrammars.Add(posEmptyGrammar);
        }
      }
      m_grammars[gram + EXISchemaLayout.GRAMMAR_N_PRODUCTION] = fieldInt;

      // Populate productions
      for (int i = 0; i < n_prods; i++, pos++) {
        m_grammars[pos] = productionsInGrammar[i];
      }

      m_gramPositions.Add(gram);
      expectEndElement(); // </Grammar>
    }

    private EventType expectStartElement(string name) {
      EventDescription @event = nextElement();
      if (@event == null || !name.Equals(@event.Name)) {
        throw new Exception();
      }
      return @event.getEventType();
    }

    private void expectEndElement() {
      EventDescription @event = nextElement();
      if (@event != null) {
        throw new Exception();
      }
      else {
        @event = m_scanner.nextEvent();
        if (@event.EventKind != EventDescription_Fields.EVENT_EE) {
          throw new Exception();
        }
      }
    }

    private EventDescription nextElement() {
      EventDescription @event;
      while ((@event = m_scanner.nextEvent()) != null) {
        switch (@event.EventKind) {
          case EventDescription_Fields.EVENT_CH:
            Characters characters = @event.Characters;
            if (characters.length != 0) {
              int limit = characters.startIndex + characters.length;
              for (int i = characters.startIndex; i < limit; i++) {
                char c = characters.characters[i];
                if (c != ' ' && c != '\t' && c != '\n' && c != '\r') {
                  throw new Exception();
                }
              }
            }
            goto case Nagasena.Proc.Common.EventDescription_Fields.EVENT_SD;
          case EventDescription_Fields.EVENT_SD:
          case EventDescription_Fields.EVENT_NS:
          case EventDescription_Fields.EVENT_PI:
          case EventDescription_Fields.EVENT_CM:
          case EventDescription_Fields.EVENT_ER:
          case EventDescription_Fields.EVENT_DTD:
            // The above events are simply ignored.
            continue;
          case EventDescription_Fields.EVENT_SE:
            if (!"urn:publicid:nagasena".Equals(@event.URI)) {
              throw new Exception();
            }
            return @event;
          default:
            m_scanner.putBack(@event);
            return null;
        }
      }
      throw new Exception();
    }

    /// <summary>
    /// Read an int value that terminates with EE.
    /// </summary>
    private int readIntContent() {
      return Convert.ToInt32(readStringContent());
    }

    /// <summary>
    /// Read a BigInteger value that terminates with EE.
    /// </summary>
    private BigInteger readBigIntegerContent() {
      return Convert.ToInt64(readStringContent());
    }

    /// <summary>
    /// Read a string value that terminates with EE.
    /// </summary>
    private string readStringContent() {
      string value = "";
      EventDescription @event;
      while ((@event = m_scanner.nextEvent()) != null) {
        switch (@event.EventKind) {
          case EventDescription_Fields.EVENT_CH:
            value = value + @event.Characters.makeString();
            continue;
          case EventDescription_Fields.EVENT_EE:
            return value;
          case EventDescription_Fields.EVENT_NS:
          case EventDescription_Fields.EVENT_PI:
          case EventDescription_Fields.EVENT_CM:
          case EventDescription_Fields.EVENT_ER:
          case EventDescription_Fields.EVENT_DTD:
            continue;
          // Unexpected events. 
          case EventDescription_Fields.EVENT_SD:
          case EventDescription_Fields.EVENT_ED:
          case EventDescription_Fields.EVENT_SE:
          case EventDescription_Fields.EVENT_AT:
          case EventDescription_Fields.EVENT_TP:
          case EventDescription_Fields.EVENT_NL:
          default:
            throw new Exception();
        }
      }
      throw new Exception();
    }

    private abstract class SchemaScanner {
      public EventDescription @event;

      internal SchemaScanner() {
        @event = null;
      }

      public abstract EventDescription nextEvent();

      public void putBack(EventDescription @event) {
        Debug.Assert(this.@event == null);
        this.@event = @event;
      }

      public static SchemaScanner newScanner(Scanner scanner) {
        return new SchemaScannerAnonymousInnerClassHelper(scanner);
      }

      private class SchemaScannerAnonymousInnerClassHelper : SchemaScanner {
        private Scanner m_scanner;

        public SchemaScannerAnonymousInnerClassHelper(Scanner scanner) {
          m_scanner = scanner;
        }

        public override EventDescription nextEvent() {
          if (@event != null) {
            EventDescription _nextEvent = @event;
            @event = null;
            return _nextEvent;
          }
          return m_scanner.nextEvent();
        }
      }
    }

    private sealed class SchemaScannerFacade : SchemaScanner {
      private readonly EXISchemaReader outerInstance;

      internal readonly Scanner m_fragmentScanner;
      internal readonly SchemaScanner m_schemaScanner;

      internal SchemaScannerFacade(EXISchemaReader outerInstance, SchemaScanner schemaScanner, byte[] bts) {
        this.outerInstance = outerInstance;
        m_schemaScanner = schemaScanner;

        EXIDecoder decoder = new EXIDecoder();
        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = null;
        try {
          decoder.GrammarCache = new GrammarCache(GrammarSchema.EXISchema, GrammarOptions.STRICT_OPTIONS);
          decoder.Fragment = true;
          scanner = decoder.processHeader();
        }
        catch (Exception) {
        }

        m_fragmentScanner = scanner;
        try {
          EventDescription @event;
          @event = m_fragmentScanner.nextEvent();
          Debug.Assert(@event.EventKind == EventDescription_Fields.EVENT_SD);
        }
        catch (IOException) {

        }
      }

      public override EventDescription nextEvent() {
        if (@event != null) {
          EventDescription _nextEvent = @event;
          @event = null;
          return _nextEvent;
        }
        EventDescription nextEvent;
        if ((nextEvent = m_fragmentScanner.nextEvent()) != null && nextEvent.EventKind != EventDescription_Fields.EVENT_ED) {
          return nextEvent;
        }
        else {
          return m_schemaScanner.nextEvent();
        }
      }
    }

  }

}