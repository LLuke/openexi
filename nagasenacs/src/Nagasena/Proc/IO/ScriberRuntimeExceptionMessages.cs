using System;
using System.Globalization;
using Nagasena.Util;

namespace Nagasena.Proc.IO {

  /// <summary>
  /// This class is not intended for public use though it is qualified
  /// as 'public'.
  /// </summary>
  public sealed class ScriberRuntimeExceptionMessages : MessageResolver {

    protected override String FileName {
      get {
        return "ScriberRuntimeExceptionXMsg.xml";
      }
    }

    internal ScriberRuntimeExceptionMessages() : this(null) {
    }

    internal ScriberRuntimeExceptionMessages(CultureInfo locale)
      : base(locale) {
    }

  }

}