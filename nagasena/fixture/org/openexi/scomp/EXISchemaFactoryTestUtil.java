package org.openexi.scomp;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.xml.sax.*;

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
    if (url == null)
      url = cls.getResource("/file.txt");
    
    EXISchema compiled;
    if ((compiled = schemaCompiler.compile(inputSources)) != null) {
      //saveXsc(compiled, url);
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      new EXISchemaWriter().serialize(compiled, byteArrayOutputStream, context.stringBuilder);
      final byte[] grammarBytes = byteArrayOutputStream.toByteArray();
      //saveGram(grammarBytes, url);
      compiled = readEXIGrammar(grammarBytes, context.schemaReader);
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

  @SuppressWarnings("unused")
  private static void saveGram(byte[] grammar, URL url) throws Exception {
    if (url != null) {
      String urlString = url.toString();
      urlString = urlString.substring(0, urlString.lastIndexOf('.')) + ".gram";
      String filePath = new URL(urlString).getFile();
      FileOutputStream out = new FileOutputStream(filePath);
      out.write(grammar);
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
  
  private static EXISchema readEXIGrammar(byte[] grammarBytes, EXISchemaReader schemaReader) throws Exception {
    final InputStream inputStream;
    inputStream = new ByteArrayInputStream(grammarBytes);
    if (schemaReader == null)
      schemaReader = new EXISchemaReader();
    return schemaReader.parse(inputStream);
  }
  
}