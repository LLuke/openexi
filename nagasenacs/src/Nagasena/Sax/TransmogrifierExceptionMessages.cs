using System;

using Nagasena.Util;

namespace Nagasena.Sax {

  /// <summary>
  /// This class is not intended for public use though it is qualified
  /// as 'public'.
  /// </summary>
  internal sealed class TransmogrifierExceptionMessages : MessageResolver {

    protected override String FileName {
      get {
        return "TransmogrifierExceptionXMsg.xml";
      }
    }

  }

}