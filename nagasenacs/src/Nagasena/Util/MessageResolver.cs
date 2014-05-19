using System;
using System.Collections;
using System.IO;
using System.Reflection;
using System.Resources;
using System.Text;
using System.Xml;
using System.Globalization;

namespace Nagasena.Util {

  /// <exclude/>
  public abstract class MessageResolver {

    private const String REPLACEMENT_TEXT_NULL = "<null/>";

    private static readonly NameTable m_nameTable = new NameTable();
  
    private readonly Hashtable m_messageTable;
    private StreamWriter m_pStream;

    protected abstract String FileName {
      get;
    }

    public MessageResolver() : this(null) {
    }

    public MessageResolver(System.Globalization.CultureInfo locale) {
      if (locale == null) {
        locale = System.Globalization.CultureInfo.CurrentCulture;
      }
      Stream iStream = null;
      Assembly assembly = Assembly.GetCallingAssembly();
      Object[] attributes = assembly.GetCustomAttributes(false);
      for (int i = 0; i < attributes.Length; i++) {
        if (attributes[i] is NeutralResourcesLanguageAttribute) {
          String neutralLanguage = ((NeutralResourcesLanguageAttribute)attributes[i]).CultureName;
          if (neutralLanguage.Equals(locale.Name))
            iStream = resolveResourceAsStream(FileName, assembly);
        }
      }
      if (iStream == null) {
        Assembly satellite = null;
        try {
          satellite = assembly.GetSatelliteAssembly(locale);
        }
#if DEBUG
        catch (System.IO.IOException ioe) {
          System.Console.Error.WriteLine("culture='" + locale.Name + "'");
          System.Console.Error.WriteLine(ioe.Message);
          System.Console.Error.Flush();
#else
      catch (System.IO.IOException) {
#endif
        }
        if (satellite != null) {
          iStream = resolveResourceAsStream(FileName, satellite);
        }
        if (iStream == null) {
          iStream = resolveResourceAsStream(FileName, assembly);
        }
      }
      m_messageTable = new Hashtable();
      loadMessages(iStream);
    }

    /// <summary>
    /// Retrieve a resource out of an assembly given a file name.</summary>
    /// <param name="fileName">file name</param>
    /// <param name="assembly">assembly that contains the resource</param>
    /// <returns></returns>
    private Stream resolveResourceAsStream(String fileName, Assembly assembly) {
      Stream iStream = null;
      if (assembly != null && fileName != null) {
        String[] resourceNames = assembly.GetManifestResourceNames();
        for (int i = 0; i < resourceNames.Length; i++) {
          if (resourceNames[i].EndsWith(fileName)) {
            iStream = assembly.GetManifestResourceStream(resourceNames[i]);
            break;
          }
        }
      }
      return iStream;
    }

    public virtual void setStreamWriter(StreamWriter pStream) {
      m_pStream = pStream;
    }

    public virtual System.String getMessage(int code) {
      System.String msg = null;
      try {
        msg = getMessage(code, null);
      }
      catch (System.Resources.MissingManifestResourceException e) {
        if (m_pStream != null) {
          m_pStream.WriteLine(e.Message);
          m_pStream.WriteLine(e.StackTrace);
        }
      }
      return msg;
    }
    
    public virtual System.String getMessage(int code, System.String[] texts) {
      texts = texts != null ? texts : new String[0];
      String templ;
      if ((templ = (String)m_messageTable[code]) == null) {
        throw new ApplicationException(
          "No message available for error code '" + code + "' in '" + FileName + "'.");
      }
      String msg   = templ;
      if (templ.Length > 0 && texts.Length > 0) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.AppendFormat(templ, texts);
        msg = stringBuilder.ToString(/**/);
      }
      return msg != null ? msg : "";
    }

    private void loadMessages(Stream iStream) {
      lock (m_nameTable) {
        XmlTextReader xmlReader = new XmlTextReader(iStream, m_nameTable);
        while (xmlReader.Read()) {
          switch (xmlReader.NodeType) {
            case XmlNodeType.Element:
              if ("msg".Equals(xmlReader.Name)) {
                while (xmlReader.MoveToNextAttribute()) {
                  if ("id".Equals(xmlReader.Name)) {
                    String id = xmlReader.Value;
                    if (id != null && xmlReader.MoveToElement()) {
                      String innerXml = xmlReader.ReadInnerXml();
                      innerXml = innerXml.Replace("''", "'");
                      m_messageTable.Add(System.Int32.Parse(id, NumberFormatInfo.InvariantInfo), innerXml);
                    }
                    break;
                  }
                }
              }
              break;
          }
        }
      }
    }

  }

}
