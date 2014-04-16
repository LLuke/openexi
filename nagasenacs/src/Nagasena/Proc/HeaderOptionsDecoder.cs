using System;
using System.Diagnostics;
using System.IO;

using AlignmentType = Nagasena.Proc.Common.AlignmentType;
using EventDescription = Nagasena.Proc.Common.EventDescription;
using EXIOptions = Nagasena.Proc.Common.EXIOptions;
using EXIOptionsException = Nagasena.Proc.Common.EXIOptionsException;
using EventType = Nagasena.Proc.Common.EventType;
using QName = Nagasena.Proc.Common.QName;
using SchemaId = Nagasena.Proc.Common.SchemaId;
using EXIEventSchemaNil = Nagasena.Proc.Events.EXIEventSchemaNil;
using GrammarCache = Nagasena.Proc.Grammars.GrammarCache;
using OptionsGrammarCache = Nagasena.Proc.Grammars.OptionsGrammarCache;
using HeaderOptionsInputStream = Nagasena.Proc.IO.HeaderOptionsInputStream;
using BitPackedScanner = Nagasena.Proc.IO.BitPackedScanner;
using Scanner = Nagasena.Proc.IO.Scanner;
using ScannerFactory = Nagasena.Proc.IO.ScannerFactory;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc {

  internal class HeaderOptionsDecoder {

    private static readonly GrammarCache m_grammarCache;
    static HeaderOptionsDecoder() {
      m_grammarCache = OptionsGrammarCache.GrammarCache;
    }

    private readonly BitPackedScanner m_scanner;

    internal HeaderOptionsDecoder() {
      m_scanner = ScannerFactory.createHeaderOptionsScanner();
      EXISchema schema = m_grammarCache.EXISchema;
      m_scanner.setSchema(schema, (QName[])null, 0);
      m_scanner.StringTable = Scanner.createStringTable(m_grammarCache);
      try {
        m_scanner.prepare();
      }
      catch (IOException) {
        Debug.Assert(false);
      }
    }

    public virtual HeaderOptionsInputStream decode(EXIOptions options, Stream istream) {
      m_scanner.EXIOptionsInputStream = istream;
      m_scanner.setGrammar(m_grammarCache.retrieveRootGrammar(false, m_scanner.eventTypesWorkSpace), m_grammarCache.grammarOptions);
      m_scanner.reset();

      short eventVariety;
      do {
        EventDescription exiEvent;
        EventType eventType;
        exiEvent = m_scanner.nextEvent();
        if ((eventVariety = exiEvent.EventKind) == Nagasena.Proc.Common.EventDescription_Fields.EVENT_SE) {
          string name = exiEvent.Name;
          string uri = exiEvent.URI;
          if (ExiUriConst.W3C_2009_EXI_URI.Equals(uri)) {
            if ("byte".Equals(name)) {
              options.AlignmentType = AlignmentType.byteAligned;
            }
            else if ("pre-compress".Equals(name)) {
              options.AlignmentType = AlignmentType.preCompress;
            }
            else if ("compression".Equals(name)) {
              options.AlignmentType = AlignmentType.compress;
            }
            else if ("strict".Equals(name)) {
              options.Strict = true;
            }
            else if ("comments".Equals(name)) {
              options.PreserveComments = true;
            }
            else if ("pis".Equals(name)) {
              options.PreservePIs = true;
            }
            else if ("dtd".Equals(name)) {
              options.PreserveDTD = true;
            }
            else if ("prefixes".Equals(name)) {
              options.PreserveNS = true;
            }
            else if ("lexicalValues".Equals(name)) {
              options.setDatatypeRepresentationMap((QName[])null, 0);
              options.PreserveLexicalValues = true;
            }
            else if ("schemaId".Equals(name)) {
              exiEvent = m_scanner.nextEvent();
              eventType = exiEvent.getEventType();
              if (eventType.itemType == EventType.ITEM_SCHEMA_NIL) {
                if (((EXIEventSchemaNil)exiEvent).Nilled) {
                  options.SchemaId = new SchemaId(null);
                  continue;
                }
                else {
                  exiEvent = m_scanner.nextEvent();
                  eventType = exiEvent.getEventType();
                }
              }
              Debug.Assert(eventType.itemType == EventType.ITEM_SCHEMA_CH);
              options.SchemaId = new SchemaId(exiEvent.Characters.makeString());
            }
            else if ("blockSize".Equals(name)) {
              exiEvent = m_scanner.nextEvent();
              Debug.Assert(exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH);
              options.BlockSize = Convert.ToInt32(exiEvent.Characters.makeString());
            }
            else if ("valueMaxLength".Equals(name)) {
              exiEvent = m_scanner.nextEvent();
              Debug.Assert(exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH);
              options.ValueMaxLength = (Convert.ToInt32(exiEvent.Characters.makeString()));
            }
            else if ("valuePartitionCapacity".Equals(name)) {
              exiEvent = m_scanner.nextEvent();
              Debug.Assert(exiEvent.getEventType().itemType == EventType.ITEM_SCHEMA_CH);
              options.ValuePartitionCapacity = (Convert.ToInt32(exiEvent.Characters.makeString()));
            }
            else if ("datatypeRepresentationMap".Equals(name)) {
              EventDescription typeName = m_scanner.nextEvent();
              Debug.Assert(typeName.getEventType().itemType == EventType.ITEM_SCHEMA_WC_ANY);
              exiEvent = m_scanner.nextEvent();
              Debug.Assert(exiEvent.getEventType().itemType == EventType.ITEM_EE);
              EventDescription codecName = m_scanner.nextEvent();
              Debug.Assert(codecName.getEventType().itemType == EventType.ITEM_SCHEMA_WC_ANY);
              exiEvent = m_scanner.nextEvent();
              Debug.Assert(exiEvent.getEventType().itemType == EventType.ITEM_EE);
              options.appendDatatypeRepresentationMap(typeName, codecName);
            }
            else if ("fragment".Equals(name)) {
              options.Fragment = true;
            }
            else if ("selfContained".Equals(name)) {
              options.InfuseSC = true;
            }
          }
        }
      }
      while (eventVariety != Nagasena.Proc.Common.EventDescription_Fields.EVENT_ED);
      
      return (HeaderOptionsInputStream)m_scanner.BitInputStream;
    }

  }

}