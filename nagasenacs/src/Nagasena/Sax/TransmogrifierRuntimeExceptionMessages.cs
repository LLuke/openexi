using System;
using System.Globalization;
using Nagasena.Util;

namespace Nagasena.Sax {

  /// <summary>
  /// This class is not intended for public use though it is qualified
  /// as 'public'.
  /// </summary>
  internal sealed class TransmogrifierRuntimeExceptionMessages : MessageResolver {

    protected override String FileName {
      get {
        return "TransmogrifierRuntimeExceptionXMsg.xml";
      }
    }

  }

}