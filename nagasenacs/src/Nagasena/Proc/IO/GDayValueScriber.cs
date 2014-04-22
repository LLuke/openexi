using System.IO;

using QName = Nagasena.Proc.Common.QName;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  internal sealed class GDayValueScriber : DateTimeValueScriberBase {

    public static readonly GDayValueScriber instance;
    static GDayValueScriber() {
      instance = new GDayValueScriber();
    }

    private GDayValueScriber() : base(new QName("exi:gDay", "http://www.w3.org/2009/exi")) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_GDAY;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return BuiltinRCS.RCS_ID_DATETIME;
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      if (!trimWhitespaces(value)) {
        return false;
      }
      int day;
      int tz = XSDateTime.FIELD_UNDEFINED;
      nextPosition = startPosition; // OK, Let's start parsing
      if (value[nextPosition++] != '-' || nextPosition == limitPosition || value[nextPosition++] != '-' || 
          nextPosition == limitPosition || value[nextPosition++] != '-' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseDayField(value, XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, scribble)) {
        return false;
      }
      day = scribble.intValue1;
      if (nextPosition != limitPosition) {
        if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition) {
          return false;
        }
        tz = scribble.intValue1;
      }
      scribble.dateTime = new XSDateTime(XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, day,
        XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        null, tz, EXISchemaConst.G_DAY_TYPE);
      return true;
    }

    ////////////////////////////////////////////////////////////

    internal override void scribeDateTimeValue(XSDateTime dateTime, Stream channelStream, Scriber scriber) {
      writeMonthDay(0, dateTime.day, channelStream, scriber);
      writeTimeZone(dateTime.timeZone, channelStream, scriber);
    }

  }

}