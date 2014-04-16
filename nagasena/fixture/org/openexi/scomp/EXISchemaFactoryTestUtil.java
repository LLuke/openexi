package org.openexi.scomp;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.xml.sax.*;

import org.openexi.proc.HeaderOptionsOutputType;
import org.openexi.proc.common.SchemaId;
import org.openexi.sax.Transmogrifier;
import org.openexi.schema.EXISchema;

public class EXISchemaFactoryTestUtil {

  public static EXISchema getEXISchema(Class<?> cls, EXISchemaFactoryErrorHandler compilerErrorHandler)
    throws Exception {
    return getEXISchema((String)null, cls, compilerErrorHandler);
  }

  /**
   * Loads schema then compiles it into EXISchema.
   * Schema file is resolved relative to the specified class.
   */
  public static EXISchema getEXISchema(String fileName, Class<?> cls,
      EXISchemaFactoryErrorHandler compilerErrorHandler)
      throws Exception {
    return getEXISchema(fileName != null ? new String[] { fileName } : new String[0],
        cls, new EXISchemaFactoryTestUtilContext(compilerErrorHandler));
  }

  public static EXISchema getEXISchema(String fileName, Class<?> cls,
      EXISchemaFactoryTestUtilContext context) throws Exception {
    return getEXISchema(fileName != null ? new String[] { fileName } : new String[0],
        cls, context);
  }

  static EXISchema getEXISchema(String[] fileNames, Class<?> cls,
      EXISchemaFactoryTestUtilContext context)
          throws Exception {
    EXISchemaFactory schemaCompiler = new EXISchemaFactory();
    schemaCompiler.setCompilerErrorHandler(context.errorHandler);
    schemaCompiler.setEntityResolver(context.entityResolver);
    
    URL url = null;
    final InputSource[] inputSources = new InputSource[fileNames.length];
    for (int i = 0; i < fileNames.length; i++) {
      final String fileName = fileNames[i];
      if ((url = cls.getResource(fileName)) != null) {
        InputSource inputSource = new InputSource(url.openStream());
        inputSource.setSystemId(url.toString());
        inputSources[i] = inputSource;
      }
      else
        throw new RuntimeException("File '" + fileName + "' not found.");
    }
    
    EXISchema compiled;
    if ((compiled = schemaCompiler.compile(inputSources)) != null) {
      //saveXsc(compiled, url);
      compiled = readEXIGrammar(writeEXIGrammar(compiled, context.stringBuilder), context.schemaReader);
      InputStream serialized = serializeSchema(compiled);
      return loadSchema(serialized);
    }
    return null;
  }
  
  @SuppressWarnings("unused")
  private static void saveXsc(EXISchema schema, URL url) throws Exception {
    if (url != null) {
      String urlString = url.toString();
      urlString = urlString.substring(0, urlString.lastIndexOf('.')) + ".xsc";
      String filePath = new URL(urlString).getFile();
      DataOutputStream out = new DataOutputStream(new FileOutputStream(filePath));
      schema.writeOut(out);
      out.close();
    }
  }

  private static EXISchema loadSchema(InputStream is) throws IOException, ClassNotFoundException {
    EXISchema schema = null;
    DataInputStream dis = new DataInputStream(is);
    try {
      schema = EXISchema.readIn(dis);
    }
    finally {
      if (dis != null) dis.close();
    }
    return schema;
  }
  
  private static InputStream serializeSchema(EXISchema schema) throws IOException {
    InputStream is = null;
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    DataOutputStream dos = new DataOutputStream(bos);
    try {
      schema.writeOut(dos);
      dos.flush();
      bos.flush();
      is = new ByteArrayInputStream(bos.toByteArray());
    }
    finally {
      dos.close();
      bos.close();
    }
    return is;
  }
  
  /**
   * Serialize bytes to a file.
   * @param bts
   * @param baseFileName a file that exists in the destination
   * @param fileName name of the file the bytes are output to
   * @param cls
   */
  public static void serializeBytes(byte[] bts, String baseFileName, String fileName, Class<?> cls) 
     throws IOException, URISyntaxException {
    URI uri = cls.getResource(baseFileName).toURI();
    uri = uri.resolve(fileName);
    
    FileOutputStream fos;
    fos = new FileOutputStream(uri.toURL().getFile());
    
    fos.write(bts);
    fos.close();
  }
  
  private static byte[] writeEXIGrammar(EXISchema schema, StringBuilder stringBuilder) throws Exception  {
    ByteArrayOutputStream outputStream;
    outputStream = new ByteArrayOutputStream();
    schema.writeXml(outputStream, false);
    byte[] grammarXml = outputStream.toByteArray();
    outputStream.close();
    if (stringBuilder != null) {
      stringBuilder.delete(0, stringBuilder.length());
      stringBuilder.append(new String(grammarXml, "UTF-8"));
      //System.out.println(stringBuilder.toString());
    }
    outputStream = new ByteArrayOutputStream();
    Transmogrifier transmogrifier = new Transmogrifier();
    transmogrifier.setGrammarCache(GrammarCache4Grammar.getGrammarCache(), new SchemaId("nagasena:grammar"));
    transmogrifier.setOutputOptions(HeaderOptionsOutputType.all);
    transmogrifier.setOutputStream(outputStream);
    ByteArrayInputStream inputStream = new ByteArrayInputStream(grammarXml);
    transmogrifier.encode(new InputSource(inputStream));
    inputStream.close();
    byte[] grammarBytes = outputStream.toByteArray();
    outputStream.close();
    return grammarBytes;
  }
  
  private static EXISchema readEXIGrammar(byte[] grammarBytes, EXISchemaReader schemaReader) throws Exception {
    final InputStream inputStream;
    inputStream = new ByteArrayInputStream(grammarBytes);
    if (schemaReader == null)
      schemaReader = new EXISchemaReader();
    return schemaReader.parse(inputStream);
  }
  
}