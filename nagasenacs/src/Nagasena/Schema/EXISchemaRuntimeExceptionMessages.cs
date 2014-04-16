using System;
using System.Globalization;
using Nagasena.Util;

namespace Nagasena.Schema {

  /// <summary>
  /// This class is not intended for public use though it is qualified
  /// as 'public'.
  /// </summary>
  internal sealed class EXISchemaRuntimeExceptionMessages : MessageResolver {  
    
    protected override String FileName {
      get {
        return "SchemaCorpusRuntimeExceptionXMsg.xml";
      }
    }

    //internal EXISchemaRuntimeExceptionMessages() : this(null) {
    //}

    //internal EXISchemaRuntimeExceptionMessages(CultureInfo locale) : base(locale) {
    //}

  }

}