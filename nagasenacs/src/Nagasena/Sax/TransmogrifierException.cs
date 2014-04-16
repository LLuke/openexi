using System;

namespace Nagasena.Sax {

  using Locator = Org.System.Xml.Sax.ILocator;
  using LocatorImpl = Org.System.Xml.Sax.Helpers.LocatorImpl;

  using MessageResolver = Nagasena.Util.MessageResolver;

  /// <summary>
  /// Exception handler for the Transmogrifier.
  /// </summary>
  public sealed class TransmogrifierException : Exception {

    private const long serialVersionUID = -4536662596727577640L;
    /// <summary>
    /// Unexpected Element.
    /// </summary>
    public const int UNEXPECTED_ELEM = 1;
    /// <summary>
    /// Unexpected Attribute.
    /// </summary>
    public const int UNEXPECTED_ATTR = 2;
    /// <summary>
    /// Unexpected Character Sequence.
    /// </summary>
    public const int UNEXPECTED_CHARS = 3;
    /// <summary>
    /// Unexpected Binary value.
    /// </summary>
    public const int UNEXPECTED_BINARY_VALUE = 4;
    /// <summary>
    /// Unhandled SAX parser feature.
    /// </summary>
    public const int UNHANDLED_SAXPARSER_FEATURE = 5;
    /// <summary>
    /// SAX error reported by XML parser.
    /// </summary>
    public const int SAX_ERROR = 6;
    /// <summary>
    /// Unexpected End of Element event.
    /// </summary>
    public const int UNEXPECTED_END_ELEM = 7;
    /// <summary>
    /// Unexpected End of Document event.
    /// </summary>
    public const int UNEXPECTED_ED = 8;
    /// <summary>
    /// Unexpected Start of Document event.
    /// </summary>
    public const int UNEXPECTED_SD = 9;
    /// <summary>
    /// Prefix is not bound.
    /// </summary>
    public const int PREFIX_NOT_BOUND = 10;
    /// <summary>
    /// Prefix is bound to another namespace.
    /// </summary>
    public const int PREFIX_BOUND_TO_ANOTHER_NAMESPACE = 11;
    /// <summary>
    /// Errors reported by Scriber.
    /// </summary>
    public const int SCRIBER_ERROR = 12;

    private static readonly MessageResolver m_msgs = new TransmogrifierExceptionMessages();

    private readonly int m_code;
    private readonly string m_message;

    private Exception m_exception = null;
    private Locator m_locator = null;

    /// <summary>
    /// Constructs a new TransmogrifierException. </summary>
    /// <param name="code"> int value  that represents the type of the exception </param>
    /// <param name="texts"> one or more strings that describe the exception </param>
    internal TransmogrifierException(int code, string[] texts) {
      m_code = code;
      m_message = m_msgs.getMessage(code, texts);
      m_locator = null;
    }

    /// <summary>
    /// Constructs a new TransmogrifierException. </summary>
    /// <param name="code"> int value  that represents the type of the exception </param>
    /// <param name="texts"> one or more strings that describe the exception </param>
    /// <param name="locator">  Locator for where the error occurred </param>
    internal TransmogrifierException(int code, string[] texts, LocatorImpl locator) {
      m_code = code;
      m_message = m_msgs.getMessage(code, texts);
      m_locator = locator;
    }

    /// <summary>
    /// Returns a code that represents the type of the exception. </summary>
    /// <returns> error code </returns>
    public int Code {
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
    public Exception Exception {
      get {
        return m_exception;
      }
      set {
        m_exception = value;
      }
    }


    /// <summary>
    /// Returns the locator that is associated with this compilation error. </summary>
    /// <returns> a Locator if available, otherwise null </returns>
    public Locator Locator {
      get {
        return m_locator;
      }
    }

  }

}