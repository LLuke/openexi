using System;
using Nagasena.Util;

namespace Nagasena.Proc.IO {

  /// <summary>
  /// ScriberException represents an exception inherent to Scribers.
  /// </summary>
  public sealed class ScriberRuntimeException : Exception {

    /// <summary>
    /// Manifested size and the actual size of the binary data do not match.
    /// </summary>
    public const int BINARY_DATA_SIZE_MISMATCH = 1;
    /// <summary>
    /// Manifested binary data size is too large for compress or preCompress alignment types.
    /// </summary>
    public const int BINARY_DATA_SIZE_TOO_LARGE = 2;
    /// <summary>
    /// Prefix cannot be null.
    /// </summary>
    public const int PREFIX_IS_NULL = 3;

    private static readonly MessageResolver m_msgs = new ScriberRuntimeExceptionMessages();

    private static readonly string[] NO_TEXTS = new string[] { };

    private readonly int m_code;
    private readonly string m_message;

    public ScriberRuntimeException(int code) : this(code, NO_TEXTS) {
    }

    /// <summary>
    /// Constructs a new ScriberException. </summary>
    /// <param name="code"> int value  that represents the type of the exception </param>
    /// <param name="texts"> one or more strings that describe the exception </param>
    public ScriberRuntimeException(int code, string[] texts) {
      m_code = code;
      m_message = m_msgs.getMessage(code, texts);
    }

    /// <summary>
    /// Returns the error code of the exception. </summary>
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

  }

}