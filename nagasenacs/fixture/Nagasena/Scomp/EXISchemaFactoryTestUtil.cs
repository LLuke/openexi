using System;
using System.IO;
using System.Text;

using HeaderOptionsOutputType = Nagasena.Proc.HeaderOptionsOutputType;
using SchemaId = Nagasena.Proc.Common.SchemaId;
using Transmogrifier = Nagasena.Sax.Transmogrifier;
using EXISchema = Nagasena.Schema.EXISchema;
using EmptySchema = Nagasena.Schema.EmptySchema;

namespace Nagasena.Scomp {

  public class EXISchemaFactoryTestUtil {

    public static EXISchema getEXISchema() {
      return getEXISchema((string)null, (Object)null);
    }

    /// <summary>
    /// Loads schema then compiles it into EXISchema.
    /// Schema file is resolved relative to the specified class.
    /// </summary>
    internal static EXISchema getEXISchema(string fileName, Object obj) {
      if (fileName == null) {
        return EmptySchema.EXISchema;
      }

      Uri uri = getAbsoluteUri(fileName, obj);

      FileStream fos;
      fos = new FileStream(uri.LocalPath, FileMode.Open);

      EXISchema compiled;
      compiled = loadSchema(fos);

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

    /*
    private static sbyte[] writeEXIGrammar(EXISchema schema, StringBuilder stringBuilder) {
      ByteArrayOutputStream outputStream;
      outputStream = new ByteArrayOutputStream();
      schema.writeXml(outputStream, false);
      sbyte[] grammarXml = outputStream.toByteArray();
      outputStream.close();
      if (stringBuilder != null) {
        stringBuilder.Remove(0, stringBuilder.Length);
        stringBuilder.Append(StringHelperClass.NewString(grammarXml, "UTF-8"));
        //System.out.println(stringBuilder.toString());
      }
      outputStream = new ByteArrayOutputStream();
      Transmogrifier transmogrifier = new Transmogrifier();
      transmogrifier.setGrammarCache(GrammarCache4Grammar.GrammarCache, new SchemaId("nagasena:grammar"));
      transmogrifier.OutputOptions = HeaderOptionsOutputType.all;
      transmogrifier.OutputStream = outputStream;
      ByteArrayInputStream inputStream = new ByteArrayInputStream(grammarXml);
      transmogrifier.encode(new InputSource(inputStream));
      inputStream.close();
      sbyte[] grammarBytes = outputStream.toByteArray();
      outputStream.close();
      return grammarBytes;
    }

    private static EXISchema readEXIGrammar(sbyte[] grammarBytes, EXISchemaReader schemaReader) {
      InputStream inputStream;
      inputStream = new ByteArrayInputStream(grammarBytes);
      if (schemaReader == null) {
        schemaReader = new EXISchemaReader();
      }
      return schemaReader.parse(inputStream);
    }
    */

  }
}