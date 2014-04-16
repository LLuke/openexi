using System.IO;
using System.Numerics;

using QName = Nagasena.Proc.Common.QName;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  internal class TimeValueScriber : DateTimeValueScriberBase {

    public static readonly TimeValueScriber instance;
    static TimeValueScriber() {
      instance = new TimeValueScriber();
    }

    private TimeValueScriber() : base(new QName("exi:time", "http://www.w3.org/2009/exi")) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_TIME;
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
      int hour, minute, second;
      BigInteger reverseFractionalSecond = XSDateTime.BIGINTEGER_MINUSONE;
      int tz = XSDateTime.FIELD_UNDEFINED;
      nextPosition = startPosition; // OK, Let's start parsing
      if (!parseHourField(value, scribble) || nextPosition == limitPosition) {
        return false;
      }
      hour = scribble.intValue1;
      if (value[nextPosition++] != ':' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseMinuteField(value, scribble) || nextPosition == limitPosition) {
        return false;
      }
      minute = scribble.intValue1;
      if (hour == 24 && minute != 0) {
        return false;
      }
      if (value[nextPosition++] != ':' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseSecondField(value, scribble)) {
        return false;
      }
      second = scribble.intValue1;
      if (hour == 24 && second != 0) {
        return false;
      }
      if (nextPosition != limitPosition) {
        if (value[nextPosition] == '.') {
          if (++nextPosition == limitPosition) {
            return false;
          }
          else if ((reverseFractionalSecond = parseFractionalSecondField(value)) == XSDateTime.BIGINTEGER_MINUSONE) {
            return false;
          }
          else if (reverseFractionalSecond.Sign == 0) {
            reverseFractionalSecond = XSDateTime.BIGINTEGER_MINUSONE;
          }
        }
        if (nextPosition != limitPosition) {
          if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition) {
            return false;
          }
          tz = scribble.intValue1;
        }
      }
      scribble.dateTime = new XSDateTime(XSDateTime.FIELD_UNDEFINED, XSDateTime.FIELD_UNDEFINED, 
        XSDateTime.FIELD_UNDEFINED, hour, minute, second, reverseFractionalSecond, tz, EXISchemaConst.TIME_TYPE);
      return true;
    }

    ////////////////////////////////////////////////////////////

    internal override void scribeDateTimeValue(XSDateTime dateTime, Stream channelStream, Scriber scriber) {
      writeTime(dateTime.hour, dateTime.minute, dateTime.second, dateTime.reverseFractionalSecond, channelStream, scriber);
      writeTimeZone(dateTime.timeZone, channelStream, scriber);
    }

  }

}