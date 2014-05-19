using System;
using Nagasena.Util;

namespace Nagasena.Schema {

  /// <summary>
  /// Thrown when any programming errors are detected while accessing
  /// SchemaCorpus. This exception is meant to be dealt with during
  /// development time as opposed to run-time. </summary>
  /// <exclude/>
  /// <seealso cref="EXISchema"></seealso>
  public class EXISchemaRuntimeException : Exception {

    /// <summary>
    /// The index is out of bounds. </summary>
    public const int INDEX_OUT_OF_BOUNDS = 1;

    private static readonly MessageResolver m_msgs = new EXISchemaRuntimeExceptionMessages();

    private readonly int m_code;
    private readonly string m_message;

    /// <summary>
    /// Constructor.
    /// </summary>
    internal EXISchemaRuntimeException(int code, string[] texts) {
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

  }
}