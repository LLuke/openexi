package org.openexi.proc;

import java.io.InputStream;
import java.io.IOException;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptions;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.QName;
import org.openexi.proc.common.SchemaId;
import org.openexi.proc.grammars.DocumentGrammarState;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.proc.io.HeaderOptionsInputStream;
import org.openexi.proc.io.BitPackedScanner;
import org.openexi.proc.io.Scanner;
import org.openexi.proc.io.ScannerFactory;
import org.openexi.proc.io.StringTable;
import org.openexi.schema.EXISchema;
import org.openexi.schema.EmptySchema;

public class EXIDecoder {

  private Scanner m_scanner; 
  
  private GrammarCache m_grammarCache;
  private EXISchema m_schema;
  private EXISchemaResolver m_schemaResolver;
  
  private InputStream m_inputStream;

  private final EXIOptions m_exiOptions;
  private final EXIOptions m_exiHeaderOptions;
  private final HeaderOptionsDecoder m_optionsDecoder;
  
  private static final int DEFAULT_INFLATOR_BUF_SIZE = 8192; 
  private final int m_inflatorBufSize;
  
  private final DocumentGrammarState m_documentGrammarState;

  
  public EXIDecoder() {
    this(DEFAULT_INFLATOR_BUF_SIZE);
  }

  public EXIDecoder(int inflatorBufSize) {
    m_inflatorBufSize = inflatorBufSize;
    m_exiOptions = new EXIOptions();
    m_exiHeaderOptions = new EXIOptions();
    m_optionsDecoder = new HeaderOptionsDecoder();
    m_grammarCache = null;
    m_schema = null;
    m_schemaResolver = null;
    m_documentGrammarState = new DocumentGrammarState();
    m_scanner = ScannerFactory.createScanner(AlignmentType.bitPacked, m_inflatorBufSize, m_documentGrammarState);
    m_scanner.setSchema(m_schema, (QName[])null, 0);
    m_scanner.setStringTable(new StringTable(m_schema));
  }

  /**
   * Set an output stream to which encoded streams are written out.
   * @param ostream output stream
   */
  public final void setInputStream(InputStream istream) {
    m_inputStream = istream;
  }

  public final void setAlignmentType(AlignmentType alignmentType) throws EXIOptionsException {
    m_exiOptions.setAlignmentType(alignmentType);
    if (m_scanner.getAlignmentType() != alignmentType) {
      m_scanner = ScannerFactory.createScanner(alignmentType, m_inflatorBufSize, m_documentGrammarState);
      m_scanner.setSchema(m_schema, m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount());
      m_scanner.setStringTable(new StringTable(m_schema));
      m_scanner.setValueMaxLength(m_exiOptions.getValueMaxLength());
      m_scanner.setPreserveLexicalValues(m_exiOptions.getPreserveLexicalValues());
    }
  }
  
  public final void setFragment(boolean isFragment) {
    m_exiOptions.setFragment(isFragment);
  }
  
  public final void setEXISchema(GrammarCache grammarCache) throws EXIOptionsException {
    m_exiOptions.setGrammarOptions(grammarCache.grammarOptions);
    if (m_grammarCache != grammarCache) {
      m_grammarCache = grammarCache;
      final EXISchema schema;
      if ((schema = m_grammarCache.getEXISchema()) != m_schema) {
        m_schema = schema;
        m_scanner.setSchema(m_schema, m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount());
        m_scanner.setStringTable(new StringTable(m_schema));
      }
    }
  }
  
  public final void setEXISchemaResolver(EXISchemaResolver schemaResolver) {
    m_schemaResolver = schemaResolver;
  }
  
  public final void setBlockSize(int blockSize) throws EXIOptionsException {
    m_exiOptions.setBlockSize(blockSize);
  }
  
  public final void setValueMaxLength(int valueMaxLength) {
    m_exiOptions.setValueMaxLength(valueMaxLength);
    m_scanner.setValueMaxLength(valueMaxLength);
  }

  public final void setValuePartitionCapacity(int valuePartitionCapacity) {
    m_exiOptions.setValuePartitionCapacity(valuePartitionCapacity);
  }
  
  public final void setPreserveLexicalValues(boolean preserveLexicalValues) throws EXIOptionsException {
    m_exiOptions.setPreserveLexicalValues(preserveLexicalValues);
    m_scanner.setPreserveLexicalValues(preserveLexicalValues);
  }
  
  /**
   * Set a datatype representation map.
   * @param dtrm a sequence of pairs of datatype qname and datatype representation qname
   * @param n_bindings the number of qname pairs
   */
  public final void setDatatypeRepresentationMap(QName[] dtrm, int n_bindings) throws EXIOptionsException {
    if (!QName.isSame(m_exiOptions.getDatatypeRepresentationMap(), m_exiOptions.getDatatypeRepresentationMapBindingsCount(), dtrm, n_bindings)) {
      m_exiOptions.setDatatypeRepresentationMap(dtrm, n_bindings);
      m_scanner.setSchema(m_schema, dtrm, n_bindings);
    }
  }

  public Scanner processHeader() throws IOException, EXIOptionsException {
    int val = m_inputStream.read();
    if (val == 36) {
      m_inputStream.read(); // 69
      m_inputStream.read(); // 88
      m_inputStream.read(); // 73
      val = m_inputStream.read();
    }
    final Scanner scanner;
    final GrammarCache grammarCache;
    final boolean isFragment; 
    HeaderOptionsInputStream bitInputStream = null;
    if ((val & 0x20) != 0) {
      final AlignmentType alignmentType;
      final EXISchema schema;
      m_exiHeaderOptions.init();
      HeaderOptionsInputStream inputStream;
      inputStream = m_optionsDecoder.decode(m_exiHeaderOptions, m_inputStream);
      final short grammarOptions = m_exiHeaderOptions.toGrammarOptions();
      final SchemaId schemaId;
      if ((schemaId = m_exiHeaderOptions.getSchemaId()) != null) {
        final String schemaIdValue;
        if ((schemaIdValue = schemaId.getValue()) == null) {
          schema = null;
          grammarCache = new GrammarCache(grammarOptions);
        }
        else if (schemaIdValue.length() == 0) {
          // REVISIT: test
          assert false;
          schema = EmptySchema.getEXISchema();
          grammarCache = new GrammarCache(schema, grammarOptions);
        }
        else {
          EXISchema specifiedSchema = null;
          if (m_schemaResolver != null && (specifiedSchema = m_schemaResolver.resolveSchema(schemaIdValue)) != null && specifiedSchema != m_schema) {
            grammarCache = new GrammarCache(specifiedSchema, grammarOptions);
            schema = specifiedSchema;
          }
          else { // Failed to resolve schemaIdValue into a schema
            // REVISIT: check the ID associated with m_schema (if any has been provided).
            schema = m_schema;
            if (m_grammarCache.grammarOptions != grammarOptions)
              grammarCache = new GrammarCache(schema, grammarOptions);
            else
              grammarCache = m_grammarCache;
          }
        }
      }
      else {
        schema = m_schema;
        if (m_grammarCache.grammarOptions != grammarOptions)
          grammarCache = new GrammarCache(schema, grammarOptions);
        else
          grammarCache = m_grammarCache;
      }
      if ((alignmentType = m_exiHeaderOptions.getAlignmentType()) == AlignmentType.bitPacked) {
        bitInputStream = inputStream;
      }
      scanner = ScannerFactory.createScanner(alignmentType, m_inflatorBufSize, m_documentGrammarState);
      scanner.setSchema(schema, m_exiHeaderOptions.getDatatypeRepresentationMap(), m_exiHeaderOptions.getDatatypeRepresentationMapBindingsCount());
      scanner.setStringTable(new StringTable(schema));
      scanner.setValueMaxLength(m_exiHeaderOptions.getValueMaxLength());
      scanner.setPreserveLexicalValues(m_exiHeaderOptions.getPreserveLexicalValues());
      scanner.setValueMaxLength(m_exiHeaderOptions.getValueMaxLength());

      scanner.setBlockSize(m_exiHeaderOptions.getBlockSize());
      scanner.getStringTable().setValuePartitionCapacity(m_exiHeaderOptions.getValuePartitionCapacity());
      isFragment = m_exiHeaderOptions.isFragment();
      scanner.setHeaderOptions(m_exiHeaderOptions);
    }
    else {
      scanner = m_scanner;
      scanner.setBlockSize(m_exiOptions.getBlockSize());
      scanner.getStringTable().setValuePartitionCapacity(m_exiOptions.getValuePartitionCapacity());
      isFragment = m_exiOptions.isFragment();
      scanner.setHeaderOptions(null);
      grammarCache = m_grammarCache;
    }
    scanner.reset();
    
    if (bitInputStream != null)
      ((BitPackedScanner)scanner).takeover(bitInputStream);
    else
      scanner.setInputStream(m_inputStream);
  
    scanner.setGrammar(grammarCache.retrieveDocumentGrammar(isFragment, m_documentGrammarState.eventTypesWorkSpace), grammarCache.grammarOptions);
    scanner.prepare();
    
    return scanner;
  }
  
}
