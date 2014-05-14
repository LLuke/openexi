using System;
using System.IO;
using System.Text;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using EventTypeList = Nagasena.Proc.Common.EventTypeList;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using Transmogrifier = Nagasena.Sax.Transmogrifier;
using TransmogrifierException = Nagasena.Sax.TransmogrifierException;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using TestBase = Nagasena.Schema.TestBase;

namespace Nagasena.Scomp {

  [TestFixture]
  public class EXISchemaReaderTest : TestBase {

    public EXISchemaReaderTest() {
      m_stringBuilder = new StringBuilder();
      m_schemaReader = new EXISchemaReader();
    }

    private readonly StringBuilder m_stringBuilder;
    private readonly EXISchemaReader m_schemaReader;

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// String datatype with whiteSpace being "preserve".
    /// </summary>
    [Test]
    public virtual void testStringElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/stringElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "abc" + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("abc", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(EXISchema.WHITESPACE_PRESERVE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// String datatype with whiteSpace being "replace".
    /// </summary>
    [Test]
    public virtual void testStringElement_02() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/stringElement_02.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  abc  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("  abc  ", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(EXISchema.WHITESPACE_REPLACE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// String datatype with whiteSpace being "collapse".
    /// </summary>
    [Test]
    public virtual void testStringElement_03() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/stringElement_03.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  abc  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("  abc  ", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(EXISchema.WHITESPACE_COLLAPSE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// String datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedStringElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedStringElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "Nagoya" + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("Nagoya", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(EXISchema.WHITESPACE_PRESERVE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
      Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// String datatype with RCS.
    /// </summary>
    [Test]
    public virtual void testPatternedStringElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/patternedStringElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "Nagoya" + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("Nagoya", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(EXISchema.WHITESPACE_PRESERVE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
      Assert.AreEqual(53, schema.getRestrictedCharacterCountOfStringSimpleType(contentDatatype));
      int rcs = schema.getRestrictedCharacterOfSimpleType(contentDatatype);
      int[] types = schema.Types;
      Assert.AreEqual('A', types[rcs + 0]);
      Assert.AreEqual('M', types[rcs + 12]);
      Assert.AreEqual('N', types[rcs + 13]);
      Assert.AreEqual('Z', types[rcs + 25]);
      Assert.AreEqual('_', types[rcs + 26]);
      Assert.AreEqual('a', types[rcs + 27]);
      Assert.AreEqual('m', types[rcs + 39]);
      Assert.AreEqual('n', types[rcs + 40]);
      Assert.AreEqual('z', types[rcs + 52]);

      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// String datatype with both enumerated values and RCS.
    /// </summary>
    [Test]
    public virtual void testPatternedStringElement_02() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/patternedStringElement_02.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "Assange" + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("Assange", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.STRING_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(schema.getBaseTypeOfSimpleType(contentDatatype))));
      Assert.AreEqual(EXISchema.WHITESPACE_PRESERVE, schema.getWhitespaceFacetValueOfStringSimpleType(contentDatatype));
      Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
      Assert.AreEqual(52, schema.getRestrictedCharacterCountOfStringSimpleType(contentDatatype));
      int rcs = schema.getRestrictedCharacterOfSimpleType(contentDatatype);
      int[] types = schema.Types;
      Assert.AreEqual('A', types[rcs + 0]);
      Assert.AreEqual('M', types[rcs + 12]);
      Assert.AreEqual('N', types[rcs + 13]);
      Assert.AreEqual('Z', types[rcs + 25]);
      Assert.AreEqual('a', types[rcs + 26]);
      Assert.AreEqual('m', types[rcs + 38]);
      Assert.AreEqual('n', types[rcs + 39]);
      Assert.AreEqual('z', types[rcs + 51]);

      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);

      originalValue = "<foo:A xmlns:foo='urn:foo'>" + "Tokyo" + "</foo:A>\n";

      // Let's verify that enumeration is used, instead of RCS
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("Tokyo"));
       return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Boolean datatype without <Patterned/>
    /// </summary>
    [Test]
    public virtual void testBooleanElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/booleanElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] originalValues = {
        "  true  ", 
        "  false  ",
      };
      String[] resultValues = {
        "true", 
        "false",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      for (i = 0; i < xmlStrings.Length; i++) {
        Transmogrifier transmogrifier = new Transmogrifier();
        transmogrifier.GrammarCache = grammarCache;
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        EXIDecoder decoder = new EXIDecoder();
        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.BOOLEAN_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.IsFalse(schema.isPatternedBooleanSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// Boolean datatype without <Patterned/>
    /// </summary>
    [Test]
    public virtual void testBooleanElement_02() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/booleanElement_02.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] originalValues = {
        "  true  ", 
        "  false  ",
      };
      String[] resultValues = {
        "true", 
        "false",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + originalValues[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      for (i = 0; i < xmlStrings.Length; i++) {
        Transmogrifier transmogrifier = new Transmogrifier();
        transmogrifier.GrammarCache = grammarCache;
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        EXIDecoder decoder = new EXIDecoder();
        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.BOOLEAN_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.IsTrue(schema.isPatternedBooleanSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// Decimal datatype
    /// </summary>
    [Test]
    public virtual void testDecimalElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/decimalElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  12345.67890  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("12345.6789", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.DECIMAL_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Decimal datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedDecimalElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedDecimalElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 100.1234567\n",
          "101.2345678",
          "102.3456789"
      };
      String[] resultValues = {
          "100.1234567",
          "101.2345678",
          "102.3456789"
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.DECIMAL_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "123.456789" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("123.456789"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Float datatype
    /// </summary>
    [Test]
    public virtual void testFloatElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/floatElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  12.78e-2  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("1278E-4", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.FLOAT_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Double datatype
    /// </summary>
    [Test]
    public virtual void testFloatElement_02() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/floatElement_02.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1267.43233e12  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("126743233E7", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.DOUBLE_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Float datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedFloatElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedFloatElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 103.01\n",
          "105.01",
          "107.01"
      };
      String[] resultValues = {
          "10301E-2",
          "10501E-2",
          "10701E-2"
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.FLOAT_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "123.456789" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("123.456789"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Double datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedFloatElement_02() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedFloatElement_02.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r -1E4\n",
          "1267.43233E12",
          "12.78e-2",
          "12",
          "1200",
          "0",
          "-0",
          "INF",
          "-INF",
          "NaN",
          "0E3",
      };
      String[] resultValues = {
          "-1E4",
          "126743233E7",
          "1278E-4",
          "12",
          "12E2",
          "0",
          "0",
          "INF",
          "-INF",
          "NaN",
          "0",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.DOUBLE_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(11, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "123.456789" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("123.456789"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Integer datatype that uses signed integer representation. 
    /// </summary>
    [Test]
    public virtual void testIntegerElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1234567890  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("1234567890", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(EXISchema.INTEGER_CODEC_DEFAULT, schema.getWidthOfIntegralSimpleType(contentDatatype));
      Assert.AreEqual(EXISchema.NIL_VALUE, schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Integer datatype that uses unsigned integer representation. 
    /// </summary>
    [Test]
    public virtual void testIntegerElement_02a() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_02a.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1234567890  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("1234567890", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.POSITIVE_INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(EXISchema.INTEGER_CODEC_NONNEGATIVE, schema.getWidthOfIntegralSimpleType(contentDatatype));
      Assert.AreEqual(EXISchema.NIL_VALUE, schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Integer datatype that uses unsigned integer representation. 
    /// </summary>
    [Test]
    public virtual void testIntegerElement_02b() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_02b.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1234567890  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("1234567890", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.INT_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(EXISchema.INTEGER_CODEC_NONNEGATIVE, schema.getWidthOfIntegralSimpleType(contentDatatype));
      Assert.AreEqual(EXISchema.NIL_VALUE, schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Integer datatype that uses 6-bits representation with an int minInclusive value. 
    /// </summary>
    [Test]
    public virtual void testIntegerElement_03() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_03.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  78  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("78", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(6, schema.getWidthOfIntegralSimpleType(contentDatatype));
      int variant = schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype);
      Assert.AreEqual(15, schema.getIntValueOfVariant(variant));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Integer datatype that uses 6-bits representation with a long minInclusive value. 
    /// </summary>
    [Test]
    public virtual void testIntegerElement_04() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/integerElement_04.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  12678967543296  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("12678967543296", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      Assert.AreEqual(6, schema.getWidthOfIntegralSimpleType(contentDatatype));
      int variant = schema.getMinInclusiveFacetOfIntegerSimpleType(contentDatatype);
      Assert.AreEqual(12678967543233L, schema.getLongValueOfVariant(variant));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Integer datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedIntegerElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedIntegerElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 115\n",
          "9223372036854775807",
          "-9223372036854775808",
          "98765432109876543210",
          "987654321098765432",
          "-987654321098765432"
      };
      String[] resultValues = {
          "115", 
          "9223372036854775807",
          "-9223372036854775808",
          "98765432109876543210",
          "987654321098765432",
          "-987654321098765432"
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.INTEGER_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(6, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "123" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("123"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Duration datatype
    /// </summary>
    [Test]
    public virtual void testDurationElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/durationElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  P1Y2M3DT10H30M  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("  P1Y2M3DT10H30M  ", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.DURATION_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Duration datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedDurationElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedDurationElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r P1Y2M3DT10H30M\n",
          "P1Y2M4DT10H30M",
          "P1Y2M5DT10H30M"
      };
      string[] resultValues = {
        "P428DT10H30M",
        "P429DT10H30M",
        "P430DT10H30M"
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.DURATION_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "P1Y2M6DT10H30M" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("P1Y2M6DT10H30M"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// DateTime datatype
    /// </summary>
    [Test]
    public virtual void testDateTimeElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/dateTimeElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  2003-03-19T13:20:00-05:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("2003-03-19T13:20:00-05:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.DATETIME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// DateTime datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedDateTimeElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedDateTimeElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 2003-03-19T13:20:00-05:00\n",
          "2003-03-20T13:20:00-05:00",
          "2003-03-21T13:20:00-05:00",
          "2013-06-03T24:00:00-05:00",
          "2013-06-04T06:00:00Z",
          "2012-07-01T00:00:00Z",
      };
      String[] resultValues = {
          "2003-03-19T13:20:00-05:00",
          "2003-03-20T13:20:00-05:00",
          "2003-03-21T13:20:00-05:00",
          "2013-06-03T24:00:00-05:00",
          "2013-06-04T06:00:00Z",
          "2012-07-01T00:00:00Z",
      };
    
      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.DATETIME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(6, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "2011-03-11T14:46:18+09:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("2011-03-11T14:46:18+09:00"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Time datatype
    /// </summary>
    [Test]
    public virtual void testTimeElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/timeElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  13:20:00-05:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("13:20:00-05:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.TIME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Time datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedTimeElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedTimeElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 13:20:00-05:00\n",
          "13:22:00-05:00",
          "13:24:00-05:00",
      };
      String[] resultValues = {
          "13:20:00-05:00",
          "13:22:00-05:00",
          "13:24:00-05:00",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.TIME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "13:26:00-05:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("13:26:00-05:00"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Date datatype
    /// </summary>
    [Test]
    public virtual void testDateElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/dateElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  2003-03-19-05:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("2003-03-19-05:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.DATE_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Date datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedDateElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedDateElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 2003-03-19-05:00\n",
          "2003-03-21-05:00",
          "2003-03-23-05:00",
      };
      String[] resultValues = {
          "2003-03-19-05:00",
          "2003-03-21-05:00",
          "2003-03-23-05:00",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.DATE_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "2003-03-25-05:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("2003-03-25-05:00"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// GYearMonth datatype
    /// </summary>
    [Test]
    public virtual void testGYearMonthElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gYearMonthElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  2003-04-05:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("2003-04-05:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.G_YEARMONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// GYearMonth datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedGYearMonthElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGYearMonthElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 2003-04-05:00\n",
          "2003-06-05:00",
          "2003-08-05:00",
      };
      String[] resultValues = {
          "2003-04-05:00",
          "2003-06-05:00",
          "2003-08-05:00",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.G_YEARMONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "2003-10-05:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("2003-10-05:00"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// GYear datatype
    /// </summary>
    [Test]
    public virtual void testGYearElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gYearElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  1969+09:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("1969+09:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.G_YEAR_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// GYear datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedGYearElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGYearElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 1969+09:00\n",
          "1971+09:00",
          "1973+09:00",
          "0001",
          "0012",
          "0123",
          "12345",
      };
      String[] resultValues = {
          "1969+09:00",
          "1971+09:00",
          "1973+09:00",
          "0001",
          "0012",
          "0123",
          "12345",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.G_YEAR_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(7, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "1975+09:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("1975+09:00"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// GMonthDay datatype
    /// </summary>
    [Test]
    public virtual void testGMonthDayElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gMonthDayElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  --09-16+09:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("--09-16+09:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.G_MONTHDAY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// GMonthDay datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedGMonthDayElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGMonthDayElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r --09-16+09:00\n",
          "--09-18+09:00",
          "--09-20+09:00",
          "--02-28-10:00",
          "--03-31-10:00",
          "--02-29-10:00",
      };
      String[] resultValues = {
          "--09-16+09:00",
          "--09-18+09:00",
          "--09-20+09:00",
          "--02-28-10:00",
          "--03-31-10:00",
          "--02-29-10:00",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.G_MONTHDAY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(6, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "--09-22+09:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("--09-22+09:00"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// GDay datatype
    /// </summary>
    [Test]
    public virtual void testGDayElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gDayElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  ---16+09:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("---16+09:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.G_DAY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// GDay datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedGDayElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGDayElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r ---16+09:00\n",
          "---18+09:00",
          "---20+09:00",
      };
      String[] resultValues = {
          "---16+09:00",
          "---18+09:00",
          "---20+09:00",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.G_DAY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "---22+09:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("---22+09:00"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// GMonth datatype
    /// </summary>
    [Test]
    public virtual void testGMonthElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/gMonthElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  --07+09:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("--07+09:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.G_MONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// GMonth datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedGMonthElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGMonthElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r --07+09:00\n",
          "--09+09:00",
          "--11+09:00",
      };
      String[] resultValues = {
          "--07+09:00",
          "--09+09:00",
          "--11+09:00",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.G_MONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "--05+09:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("--05+09:00"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// GMonth datatype with obsolete enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedGMonthElement_02() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedGMonthElement_02.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r --07+09:00\n",
          "--09+09:00",
          "--11+09:00",
      };
      String[] resultValues = {
          "--07+09:00",
          "--09+09:00",
          "--11+09:00",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.G_MONTH_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(0, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "--05+09:00" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
    }

    /// <summary>
    /// HexBinary datatype
    /// </summary>
    [Test]
    public virtual void testHexBinaryElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/hexBinaryElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  6161616161  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("6161616161", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.HEXBINARY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// HexBinary datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedHexBinaryElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedHexBinaryElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r 6161616161\n",
          "6363636363",
          "6565656565",
      };
      String[] resultValues = {
          "6161616161",
          "6363636363",
          "6565656565",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.HEXBINARY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "6262626262" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("6262626262"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// Base64Binary datatype
    /// </summary>
    [Test]
    public virtual void testBase64BinaryElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/base64BinaryElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  YWFhYWE=  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("YWFhYWE=", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.BASE64BINARY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// Base64Binary datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedBase64BinaryElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedBase64BinaryElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r YWFhYWE=\n",
          "Y2NjY2M=",
          "ZWVlZWU=",
      };
      String[] resultValues = {
          "YWFhYWE=",
          "Y2NjY2M=",
          "ZWVlZWU=",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.BASE64BINARY_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "YmJiYmI=" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("YmJiYmI="));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// AnyURI datatype
    /// </summary>
    [Test]
    public virtual void testAnyURIElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/anyURIElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  urn:foo  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("  urn:foo  ", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.ANYURI_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// AnyURI datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedAnyURIElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedAnyURIElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r urn:foo\n",
          "urn:goo",
          "urn:hoo",
      };
      String[] resultValues = {
          "urn:foo",
          "urn:goo",
          "urn:hoo",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.ANYURI_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(3, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }

      string xmlString = "<foo:A xmlns:foo='urn:foo'>" + "urn:ioo" + "</foo:A>\n";

      // Make sure a value that does not match the enumeration fail to encode.
      transmogrifier.OutputStream = new MemoryStream();
      try {
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlString)));
      }
      catch (TransmogrifierException te) {
        Assert.AreEqual(TransmogrifierException.UNEXPECTED_CHARS, te.Code);
        Assert.IsTrue(te.Message.Contains("urn:ioo"));
        return;
      }
      Assert.Fail();
    }

    /// <summary>
    /// QName datatype
    /// </summary>
    [Test]
    public virtual void testQNameElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/qNameElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  foo:A  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("  foo:A  ", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.QNAME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

    /// <summary>
    /// QName datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedQNameElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedQNameElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r foo:A\n",
          "goo:A",
          "hoo:A" // undefined values are accepted 
      };
      String[] resultValues = {
          " \t\n foo:A\n",
          "goo:A",
          "hoo:A",
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.QNAME_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(0, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// NOTATION datatype with enumerated values.
    /// </summary>
    [Test]
    public virtual void testEnumeratedNotationElement_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/enumeratedNotationElement_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] values = {
          " \t\r foo:cat\n",
          "foo:dog",
          "foo:pig",
          "foo:monkey" // undefined values are accepted 
      };
      String[] resultValues = {
          " \t\n foo:cat\n",
          "foo:dog",
          "foo:pig",
          "foo:monkey" 
      };

      xmlStrings = new string[values.Length];
      for (int i = 0; i < values.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo'>" + values[i] + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;

      for (int i = 0; i < xmlStrings.Length; i++) {
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;
        int contentDatatype;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
        contentDatatype = scanner.currentState.contentDatatype;
        Assert.AreEqual(EXISchemaConst.NOTATION_TYPE, schema.getSerialOfType(schema.getBaseTypeOfSimpleType(contentDatatype)));
        Assert.AreEqual(0, schema.getEnumerationFacetCountOfAtomicSimpleType(contentDatatype));
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// Local attribute.
    /// </summary>
    [Test]
    public virtual void testAttributeLocal_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/attributeLocal_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] originalValues = {
        "  true  ", 
        "  false  ",
      };
      String[] resultValues = {
        "true", 
        "false",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' a='" + originalValues[i] + "'>" + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      for (i = 0; i < xmlStrings.Length; i++) {
        Transmogrifier transmogrifier = new Transmogrifier();
        transmogrifier.GrammarCache = grammarCache;
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        EXIDecoder decoder = new EXIDecoder();
        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// Use of Global attribute through attribute uses.
    /// </summary>
    [Test]
    public virtual void testAttributeGlobal_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/attributeGlobal_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] originalValues = {
        "  true  ", 
        "  false  ",
      };
      String[] resultValues = {
        "true", 
        "false",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' foo:a='" + originalValues[i] + "'>" + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      for (i = 0; i < xmlStrings.Length; i++) {
        Transmogrifier transmogrifier = new Transmogrifier();
        transmogrifier.GrammarCache = grammarCache;
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        EXIDecoder decoder = new EXIDecoder();
        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// Use of Global attribute through attribute wildcards.
    /// </summary>
    [Test]
    public virtual void testAttributeGlobal_02() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/attributeGlobal_02.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string[] xmlStrings;
      String[] originalValues = {
        "  true  ", 
        "  false  ",
      };
      String[] resultValues = {
        "true", 
        "false",
      };

      int i;
      xmlStrings = new string[originalValues.Length];
      for (i = 0; i < originalValues.Length; i++) {
        xmlStrings[i] = "<foo:A xmlns:foo='urn:foo' foo:a='" + originalValues[i] + "'>" + "</foo:A>\n";
      };

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      for (i = 0; i < xmlStrings.Length; i++) {
        Transmogrifier transmogrifier = new Transmogrifier();
        transmogrifier.GrammarCache = grammarCache;
        MemoryStream outputStream = new MemoryStream();
        transmogrifier.OutputStream = outputStream;
        transmogrifier.encode(new InputSource<Stream>(string2Stream(xmlStrings[i])));
        byte[] bts = outputStream.ToArray();

        EXIDecoder decoder = new EXIDecoder();
        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        Scanner scanner = decoder.processHeader();

        EventDescription exiEvent;
        int n_events = 0;

        EventType eventType;
        EventTypeList eventTypeList;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.IsNull(eventTypeList.EE);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_AT, exiEvent.EventKind);
        Assert.AreEqual(resultValues[i], exiEvent.Characters.makeString());
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_SCHEMA_AT_WC_NS, eventType.itemType);
        Assert.AreEqual("urn:foo", eventType.uri);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
        ++n_events;

        exiEvent = scanner.nextEvent();
        Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
        eventType = exiEvent.getEventType();
        Assert.AreSame(exiEvent, eventType);
        Assert.AreEqual(0, eventType.Index);
        eventTypeList = eventType.EventTypeList;
        Assert.AreEqual(1, eventTypeList.Length);
        ++n_events;

        Assert.AreEqual(5, n_events);
      }
    }

    /// <summary>
    /// Content datatype of complex types 
    /// </summary>
    [Test]
    public virtual void testContentDatatype_01() {
      EXISchema schema = EXISchemaFactoryTestUtil.getEXISchema("/reader/contentDatatype_01.xsc", this, 
        new EXISchemaFactoryTestUtilContext(m_stringBuilder, m_schemaReader));
      Assert.IsNotNull(schema);

      string originalValue = "<foo:A xmlns:foo='urn:foo'>" + "  2003-03-19-05:00  " + "</foo:A>\n";

      GrammarCache grammarCache = new GrammarCache(schema, GrammarOptions.STRICT_OPTIONS);

      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.GrammarCache = grammarCache;
      MemoryStream outputStream = new MemoryStream();
      transmogrifier.OutputStream = outputStream;
      transmogrifier.encode(new InputSource<Stream>(string2Stream(originalValue)));
      byte[] bts = outputStream.ToArray();

      EXIDecoder decoder = new EXIDecoder();
      decoder.GrammarCache = grammarCache;
      decoder.InputStream = new MemoryStream(bts);
      Scanner scanner = decoder.processHeader();

      EventDescription exiEvent;
      int n_events = 0;

      EventType eventType;
      EventTypeList eventTypeList;
      int contentDatatype;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SD, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.IsNull(eventTypeList.EE);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_SE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_CH, exiEvent.EventKind);
      Assert.AreEqual("2003-03-19-05:00", exiEvent.Characters.makeString());
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_SCHEMA_CH, eventType.itemType);
      contentDatatype = scanner.currentState.contentDatatype;
      Assert.AreEqual(EXISchemaConst.DATE_TYPE, schema.getSerialOfType(contentDatatype));
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_EE, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreEqual(EventType.ITEM_EE, eventType.itemType);
      ++n_events;

      exiEvent = scanner.nextEvent();
      Assert.AreEqual(EventDescription_Fields.EVENT_ED, exiEvent.EventKind);
      eventType = exiEvent.getEventType();
      Assert.AreSame(exiEvent, eventType);
      Assert.AreEqual(0, eventType.Index);
      eventTypeList = eventType.EventTypeList;
      Assert.AreEqual(1, eventTypeList.Length);
      ++n_events;

      Assert.AreEqual(5, n_events);
    }

  }

}