using System;
using System.IO;
using System.Reflection;

namespace Nagasena.Schema {

  internal abstract class CommonSchema {

    internal static EXISchema loadCompiledSchema(String fileName) {
      String[] resourceNames = Assembly.GetExecutingAssembly().GetManifestResourceNames();
      int i;
      for (i = 0; i < resourceNames.Length; i++) {
        if (resourceNames[i].EndsWith(fileName))
          break;
      }
      if (i < resourceNames.Length) {
        Stream inputStream = Assembly.GetExecutingAssembly().GetManifestResourceStream(resourceNames[i]);
        return EXISchema.readIn(inputStream);
      }
      return null;
    }

  }

}
