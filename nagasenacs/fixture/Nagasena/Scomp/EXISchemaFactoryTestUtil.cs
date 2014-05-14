using System;
using System.IO;
using System.Text;

using Org.System.Xml.Sax;

using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using SchemaId = Nagasena.Proc.Common.SchemaId;
using Transmogrifier = Nagasena.Sax.Transmogrifier;
using EXISchema = Nagasena.Schema.EXISchema;
using EmptySchema = Nagasena.Schema.EmptySchema;

namespace Nagasena.Scomp {

  public class EXISchemaFactoryTestUtil {

    private static readonly byte[] m_inputBufferBytes = new byte[1024];

    public static EXISchema getEXISchema() {
      return getEXISchema((string)null, (Object)null);
    }

    internal static EXISchema getEXISchema(string fileName, Object obj) {
      return getEXISchema(fileName, obj, new EXISchemaFactoryTestUtilContext());
    }

    /// <summary>
    /// Loads schema then compiles it into EXISchema.
    /// Schema file is resolved relative to the specified class.
    /// </summary>
    internal static EXISchema getEXISchema(string fileName, Object obj, EXISchemaFactoryTestUtilContext context) {
      if (fileName == null) {
        return EmptySchema.EXISchema;
      }

      Uri uri = getAbsoluteUri(fileName, obj);

      FileStream fos;
      fos = new FileStream(uri.LocalPath, FileMode.Open);

      MemoryStream baos = new MemoryStream();
      int n_bytesRead;
      while ((n_bytesRead = fos.Read(m_inputBufferBytes, 0, m_inputBufferBytes.Length)) != 0) {
        baos.Write(m_inputBufferBytes, 0, n_bytesRead);
      }
      fos.Close();
      byte[] grammarBytes = baos.ToArray();
      baos.Close();

      EXISchema compiled = readEXIGrammar(grammarBytes, context.schemaReader);
      compiled = readEXIGrammar(writeEXIGrammar(compiled, context.stringBuilder), context.schemaReader);
      Stream serialized = serializeSchema(compiled);
      return loadSchema(serialized);
    }

    private static EXISchema loadSchema(Stream @is) {
      EXISchema schema = null;
      try {
        schema = EXISchema.readIn(@is);
      }
      finally {
        @is.Close();
      }
      return schema;
    }

    private static Stream serializeSchema(EXISchema schema) {
      Stream @is = null;
      MemoryStream bos = new MemoryStream();
      try {
        schema.writeOut(bos);
        bos.Flush();
        @is = new MemoryStream(bos.ToArray());
      }
      finally {
        bos.Close();
      }
      return @is;
    }

    /// <summary>
    /// Serialize bytes to a file. </summary>
    /// <param name="bts"> </param>
    /// <param name="baseFileName"> a file that exists in the destination </param>
    /// <param name="fileName"> name of the file the bytes are output to </param>
    /// <param name="cls"> </param>
    public static void serializeBytes(byte[] bts, string baseFileName, string fileName, Object obj) {
      Uri uri = getAbsoluteUri(baseFileName, obj);
      uri = new Uri(uri, fileName);

      FileStream fos;
      fos = new FileStream(uri.LocalPath, FileMode.Create);

      fos.Write(bts, 0, bts.Length);
      fos.Close();
    }

    internal static Uri getAbsoluteUri(String fileName, Object obj) {
      if (obj == null) {
        obj = new EXISchemaFactoryTestUtil();
      }
      String codeBase = obj.GetType().Assembly.CodeBase;
      Uri baseUri = new Uri(codeBase);

      String basePath;
      if (fileName.StartsWith("/")) {
        basePath = "testdata/whatever";
        fileName = fileName.Substring(1);
      }
      else {
        basePath = "testdata/" + obj.GetType().Namespace.Replace(".", "/") + "/whatever";
      }
      baseUri = new Uri(baseUri, basePath);
      return new Uri(baseUri, fileName);
    }

    private static byte[] writeEXIGrammar(EXISchema schema, StringBuilder stringBuilder) {
      MemoryStream outputStream;
      outputStream = new MemoryStream();
      schema.writeXml(outputStream, false);
      byte[] grammarXml = outputStream.ToArray();
      outputStream.Close();
      if (stringBuilder != null) {
        stringBuilder.Remove(0, stringBuilder.Length);
        stringBuilder.Append(System.Text.Encoding.UTF8.GetString(grammarXml));
        //Console.Out.WriteLine(stringBuilder.ToString());
      }
      outputStream = new MemoryStream();
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(GrammarCache4Grammar.GrammarCache, new SchemaId("nagasena:grammar"));
      transmogrifier.OutputOptions = HeaderOptionsOutputType.all;
      transmogrifier.OutputStream = outputStream;
      MemoryStream inputStream = new MemoryStream(grammarXml);
      transmogrifier.encode(new InputSource<Stream>(inputStream));
      inputStream.Close();
      byte[] grammarBytes = outputStream.ToArray();
      outputStream.Close();
      return grammarBytes;
    }

    private static EXISchema readEXIGrammar(byte[] grammarBytes, EXISchemaReader schemaReader) {
      MemoryStream inputStream;
      inputStream = new MemoryStream(grammarBytes);
      if (schemaReader == null) {
        schemaReader = new EXISchemaReader();
      }
      return schemaReader.parse(inputStream);
    }

  }
}