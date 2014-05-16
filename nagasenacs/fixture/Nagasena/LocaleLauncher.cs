using System;
using System.Configuration;
using System.Globalization;
using System.Threading;
using NUnit.Framework;

namespace Nagasena {

  public class LocaleLauncher {

    [SetUp]
    public virtual void setUpLocale() {
      String localeName;
      if ((localeName = ConfigurationManager.AppSettings["testLocale"]) == null) {
        localeName = "en";
      }
      CultureInfo cultureInfo = new CultureInfo(localeName);
      Thread.CurrentThread.CurrentCulture = cultureInfo;
      Thread.CurrentThread.CurrentUICulture = cultureInfo;
    }

  }
}
