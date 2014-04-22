using System.IO;
using System.Numerics;

using QName = Nagasena.Proc.Common.QName;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;
using EXISchemaConst = Nagasena.Schema.EXISchemaConst;
using XSDateTime = Nagasena.Schema.XSDateTime;

namespace Nagasena.Proc.IO {

  internal sealed class DateTimeValueScriber : DateTimeValueScriberBase {

    public static readonly DateTimeValueScriber instance;
    static DateTimeValueScriber() {
      instance = new DateTimeValueScriber();
    }

    private DateTimeValueScriber() : base(new QName("exi:dateTime", ExiUriConst.W3C_2009_EXI_URI)) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_DATETIME;
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
      int year, month, day, hour, minute, second;
      BigInteger? reverseFractionalSecond = null;
      int tz = XSDateTime.FIELD_UNDEFINED;
      nextPosition = startPosition; // OK, Let's start parsing
      if (!parseYearField(value, scribble) || nextPosition == limitPosition) {
        return false;
      }
      year = scribble.intValue1;
      if (value[nextPosition++] != '-' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseMonthField(value, scribble) || nextPosition == limitPosition) {
        return false;
      }
      month = scribble.intValue1;
      if (value[nextPosition++] != '-' || nextPosition == limitPosition) {
        return false;
      }
      if (!parseDayField(value, year, month, scribble) || nextPosition == limitPosition) {
        return false;
      }
      day = scribble.intValue1;
      if (value[nextPosition++] != 'T' || nextPosition == limitPosition) {
        return false;
      }
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
          else if (!(reverseFractionalSecond = parseFractionalSecondField(value)).HasValue) {
            return false;
          }
          else if (reverseFractionalSecond.Value.Sign == 0) {
            reverseFractionalSecond = null;
          }
        }
        if (nextPosition != limitPosition) {
          if (!parseTimezoneField(value, scribble) || nextPosition != limitPosition) {
            return false;
          }
          tz = scribble.intValue1;
        }
      }
      scribble.dateTime = new XSDateTime(year, month, day, hour, minute, second, reverseFractionalSecond, tz, EXISchemaConst.DATETIME_TYPE);
      return true;
    }

    internal override void scribeDateTimeValue(XSDateTime dateTime, Stream channelStream, Scriber scriber) {
      writeYear(dateTime.year, channelStream, scriber);
      writeMonthDay(dateTime.month, dateTime.day, channelStream, scriber);
      writeTime(dateTime.hour, dateTime.minute, dateTime.second, dateTime.reverseFractionalSecond, channelStream, scriber);
      writeTimeZone(dateTime.timeZone, channelStream, scriber);
    }

  }

}