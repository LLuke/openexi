package org.openexi.scomp;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.SchemaId;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.openexi.schema.EXISchema;
import org.xml.sax.InputSource;

public class EXISchemaWriter {
  
  final Transmogrifier m_transmogrifier;
  
  public EXISchemaWriter() {
    m_transmogrifier = new Transmogrifier();
    try {
      m_transmogrifier.setGrammarCache(GrammarCache4Grammar.getGrammarCache(), new SchemaId("nagasena:grammar"));
      m_transmogrifier.setOutputOptions(HeaderOptionsOutputType.all);
    } catch (EXIOptionsException e) {
      assert false;
      throw new RuntimeException(e);
    }
  }

  /**
   * Write out EXISchema to the specified OutputStream in EXI format
   */
  public void serialize(EXISchema schema, OutputStream outputStream) throws IOException {
    serialize(schema, outputStream, (StringBuilder)null);
  }
  
  /**
   * @y.exclude
   */
  public void serialize(EXISchema schema, OutputStream outputStream, StringBuilder stringBuilder) throws IOException {
    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
    schema.writeXml(baos, false);
    final byte[] grammarXml = baos.toByteArray();
    if (stringBuilder != null) {
      stringBuilder.delete(0, stringBuilder.length());
      stringBuilder.append(new String(grammarXml, "UTF-8"));
      //System.out.println(stringBuilder.toString());
    }
    baos.close();
    m_transmogrifier.setOutputStream(outputStream);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(grammarXml);
    try {
      m_transmogrifier.encode(new InputSource(inputStream));
    } catch (TransmogrifierException e) {
      throw new RuntimeException(e);
    }
    finally {
      inputStream.close();
      outputStream.close();
    }
  }

}
