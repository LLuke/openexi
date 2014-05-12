using System;
using System.IO;
using NUnit.Framework;

using Org.System.Xml.Sax;

using EXIDecoder = Nagasena.Proc.EXIDecoder;
using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EventDescription_Fields = Nagasena.Proc.Common.EventDescription_Fields;
using EventType = Nagasena.Proc.Common.EventType;
using GrammarOptions = Nagasena.Proc.Common.GrammarOptions;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using Scanner = Nagasena.Proc.IO.Scanner;
using EXISchema = Nagasena.Schema.EXISchema;
using TestBase = Nagasena.Schema.TestBase;
using EXISchemaFactoryTestUtil = Nagasena.Scomp.EXISchemaFactoryTestUtil;

namespace Nagasena.Sax {

  [TestFixture]
  public class JTLMTest : TestBase {

    private static readonly AlignmentType[] Alignments = new AlignmentType[] { 
      AlignmentType.bitPacked, 
      AlignmentType.byteAligned, 
      AlignmentType.preCompress, 
      AlignmentType.compress 
    };

    private static readonly string[] publish100_centennials_0 = {
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=AA0036", 
      "500", 
      "Unknown", 
      "0", 
      "PLANNED", 
      "0", 
      "NOTSET", 
      "0", 
      "2006-08-29T16:59:59.073Z", 
      "2006-07-19T16:34:53.0Z", 
      "0", 
      "ADA_MEDIUM", 
      "36.25587463", 
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=AD0040", 
      "500", 
      "Unknown", 
      "0", 
      "PLANNED", 
      "0", 
      "NOTSET", 
      "0", 
      "2006-08-29T16:59:59.183Z", 
      "2006-07-19T16:35:36.0Z", 
      "0", 
      "ADA_MEDIUM", 
      "36.02297973" 
    };

    private static readonly string[] publish911_centennials_0 = {
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=LS0012",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:48.259Z",
      "2006-07-20T00:20:52.0Z",
      "0",
      "MISSILE_MEDIUM",
      "36.15631484",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=MS0025",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:48.39Z",
      "2006-07-20T00:22:24.0Z",
      "0",
      "MISSILE_MEDIUM",
      "36.46515274",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=MS0003",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:48.52Z",
      "2006-07-20T00:21:57.0Z",
      "0",
      "CHEMICAL_PRODUCTS",
      "36.09661865",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=RR0019",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:48.64Z",
      "2006-07-20T00:14:35.0Z",
      "0",
      "BUILDING_CONCRETE",
      "36.13935089",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0274",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:48.75Z",
      "2006-07-20T00:18:32.0Z",
      "0",
      "BUILDING_METAL",
      "36.12839126",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0242",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:48.88Z",
      "2006-07-20T00:17:27.0Z",
      "0",
      "BOAT",
      "36.15649795",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0221",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.0Z",
      "2006-07-20T00:13:03.0Z",
      "0",
      "BUILDING_CONCRETE",
      "36.10599517",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0203",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.131Z",
      "2006-07-20T00:07:39.0Z",
      "0",
      "ASSEMBLY_AREA_TROOPS_AND_VEHICLES",
      "36.19093322",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=AD0058",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.251Z",
      "2006-07-19T16:36:11.0Z",
      "0",
      "PATROL",
      "36.09344863",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=AA0005",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.371Z",
      "2006-07-19T16:35:47.0Z",
      "0",
      "BUILDING_CONCRETE",
      "36.06555175",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=RS0018",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.501Z",
      "2006-07-19T16:35:30.0Z",
      "0",
      "ADA_MEDIUM",
      "36.02374267",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0048",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.621Z",
      "2006-07-20T00:09:39.0Z",
      "0",
      "BUILDING_CONCRETE",
      "36.21646118",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0020",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.741Z",
      "2006-07-20T00:09:10.0Z",
      "0",
      "BUILDING_CONCRETE",
      "36.27273941",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BR0002",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.852Z",
      "2006-07-20T00:15:16.0Z",
      "0",
      "BUILDING_CONCRETE",
      "36.18189239",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0319",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:49.972Z",
      "2006-07-20T00:17:03.0Z",
      "0",
      "BRIDGE_VEHICLE_STEEL",
      "36.1946907",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0186",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:50.102Z",
      "2006-07-20T00:12:12.0Z",
      "0",
      "ADA_MEDIUM",
      "36.18514633",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=AD0115",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:50.212Z",
      "2006-07-20T00:08:30.0Z",
      "0",
      "BUILDING_CONCRETE",
      "36.10503387",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0141",
      "500",
      "Unknown",
      "0",
      "PLANNED",
      "0",
      "NOTSET",
      "0",
      "2006-08-29T16:54:50.332Z",
      "2006-07-20T00:19:45.0Z",
      "0",
      "BUILDING_CONCRETE",
      "36.0941658",
      "http://10.32.40.2:9093/AFATDS/?TargetNumber=BD0111",
      "500",
      "Unknown"
    };

    private static readonly string[] publish100_centennials_1;
    private static readonly string[] publish911_centennials_1;
    static JTLMTest() {
      int len;
      len = publish100_centennials_0.Length;
      publish100_centennials_1 = new string[len];
      for (int i = 0; i < len; i++) {
        string str = publish100_centennials_0[i];
        if (str.EndsWith(".0Z")) {
          str = str.Substring(0, str.Length - 3) + "Z"; // i.e. omit fractional digits of value 0
        }
        publish100_centennials_1[i] = str;
      }
      len = publish911_centennials_0.Length;
      publish911_centennials_1 = new string[len];
      for (int i = 0; i < len; i++) {
        string str = publish911_centennials_0[i];
        if (str.EndsWith(".0Z")) {
          str = str.Substring(0, str.Length - 3) + "Z"; // i.e. omit fractional digits of value 0
        }
        publish911_centennials_1[i] = str;
      }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Test cases
    ///////////////////////////////////////////////////////////////////////////

    /// <summary>
    /// EXI test cases of Joint Theater Logistics Management format.
    /// </summary>
    [Test]
    public virtual void testJTLM_publish100() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/JTLM/schemas/TLMComposite.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      foreach (AlignmentType alignment in Alignments) {
        Transmogrifier encoder = new Transmogrifier();
        EXIDecoder decoder = new EXIDecoder(999);
        Scanner scanner;
        InputSource inputSource;

        encoder.AlignmentType = alignment;
        decoder.AlignmentType = alignment;

        encoder.GrammarCache = grammarCache;
        MemoryStream baos = new MemoryStream();
        encoder.OutputStream = baos;

        Uri url = resolveSystemIdAsURL("/JTLM/publish100.xml");
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        inputSource = new InputSource<Stream>(inputStream, url.ToString());

        byte[] bts;
        int n_events, n_texts;

        encoder.encode(inputSource);
        inputStream.Close();

        bts = baos.ToArray();

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = new MemoryStream(bts);
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            if (n_texts % 100 == 0) {
              int n = n_texts / 100;
              Assert.AreEqual(publish100_centennials_1[n], exiEvent.Characters.makeString());
            }
            ++n_texts;
          }
        }
        Assert.AreEqual(10610, n_events);
      }
    }

    /// <summary>
    /// Decode EXI-encoded JTLM data.
    /// </summary>
    [Test]
    public virtual void testDecodeJTLM_publish100() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/JTLM/schemas/TLMComposite.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      String[] exiFiles = { 
        "/JTLM/publish100/publish100.bitPacked", 
        "/JTLM/publish100/publish100.byteAligned",
        "/JTLM/publish100/publish100.preCompress", 
        "/JTLM/publish100/publish100.compress" };

      for (int i = 0; i < Alignments.Length; i++) {
        AlignmentType alignment = Alignments[i];
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;

        Uri url = resolveSystemIdAsURL(exiFiles[i]);
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

        int n_events, n_texts;

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = inputStream;
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            string stringValue = exiEvent.Characters.makeString();
            if (stringValue.Length == 0 && exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH) {
              --n_events;
              continue;
            }
            if (n_texts % 100 == 0) {
              int n = n_texts / 100;
              Assert.AreEqual(publish100_centennials_0[n], stringValue);
            }
            ++n_texts;
          }
        }
        inputStream.Close();

        Assert.AreEqual(10610, n_events);
      }
    }

    /// <summary>
    /// Decode EXI-encoded JTLM data.
    /// </summary>
    [Test]
    public virtual void testDecodeJTLM_publish911() {
      EXISchema corpus = EXISchemaFactoryTestUtil.getEXISchema("/JTLM/schemas/TLMComposite.xsc", this);

      GrammarCache grammarCache = new GrammarCache(corpus, GrammarOptions.DEFAULT_OPTIONS);

      String[] exiFiles = { 
        "/JTLM/publish911/publish911.bitPacked",  
        "/JTLM/publish911/publish911.byteAligned",
        "/JTLM/publish911/publish911.preCompress",
        "/JTLM/publish911/publish911.compress" };

      for (int i = 0; i < Alignments.Length; i++) {
        AlignmentType alignment = Alignments[i];
        EXIDecoder decoder = new EXIDecoder();
        Scanner scanner;

        decoder.AlignmentType = alignment;

        Uri url = resolveSystemIdAsURL(exiFiles[i]);
        FileStream inputStream = new FileStream(url.LocalPath, FileMode.Open);
        InputSource inputSource = new InputSource<Stream>(inputStream, url.ToString());

        int n_events, n_texts;

        decoder.GrammarCache = grammarCache;
        decoder.InputStream = inputStream;
        scanner = decoder.processHeader();

        EventDescription exiEvent;
        n_events = 0;
        n_texts = 0;
        while ((exiEvent = scanner.nextEvent()) != null) {
          ++n_events;
          if (exiEvent.EventKind == EventDescription_Fields.EVENT_CH) {
            string stringValue = exiEvent.Characters.makeString();
            if (stringValue.Length == 0 && exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH) {
              --n_events;
              continue;
            }
            if (n_texts % 100 == 0) {
              int n = n_texts / 100;
              Assert.AreEqual(publish911_centennials_0[n], stringValue);
            }
            ++n_texts;
          }
        }
        inputStream.Close();

        Assert.AreEqual(96576, n_events);
      }
    }

  }

}