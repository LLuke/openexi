using System;

namespace Nagasena.Sax {

  using MessageResolver = Nagasena.Util.MessageResolver;

  public class TransmogrifierRuntimeException : Exception {

    private const long serialVersionUID = -3795226789155748241L;

    /// <summary>
    /// Unhandled SAX parser property.
    /// </summary>
    public const int UNHANDLED_SAXPARSER_PROPERTY = 1;

    /// <summary>
    /// Failure to obtain an instance of XML parser.
    /// </summary>
    public const int XMLREADER_ACCESS_ERROR = 2;

    /// <summary>
    /// SAXParserFactory for use with Transmogrifier must be aware of namespaces.
    /// </summary>
    public const int SAXPARSER_FACTORY_NOT_NAMESPACE_AWARE = 3;


    private static readonly MessageResolver m_msgs = new TransmogrifierRuntimeExceptionMessages();

    private readonly int m_code;
    private readonly string m_message;

    private Exception m_exception = null;

    /// <summary>
    /// Constructs a new TransmogrifierRuntimeException. </summary>
    /// <param name="code"> int value  that represents the type of the exception </param>
    /// <param name="texts"> one or more strings that describe the exception </param>
    internal TransmogrifierRuntimeException(int code, string[] texts) {
      m_code = code;
      m_message = m_msgs.getMessage(code, texts);
    }

    /// <summary>
    /// Returns a code that represents the type of the exception. </summary>
    /// <returns> error code </returns>
    public virtual int Code {
      get {
        return m_code;
      }
    }

    /// <summary>
    /// Returns a message that describes the exception. </summary>
    /// <returns> error message </returns>
    public override string Message {
      get {
        return m_message;
      }
    }

    /// <summary>
    /// Returns an Exception object. </summary>
    /// <returns> the error as an Exception instance </returns>
    public virtual Exception Exception {
      get {
        return m_exception;
      }
      set {
        m_exception = value;
      }
    }


  }

}