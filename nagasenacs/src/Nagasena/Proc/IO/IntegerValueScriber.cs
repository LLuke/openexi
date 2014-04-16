using System;
using System.Diagnostics;
using System.Numerics;
using System.IO;

using QName = Nagasena.Proc.Common.QName;
using ExiUriConst = Nagasena.Proc.Util.ExiUriConst;
using EXISchema = Nagasena.Schema.EXISchema;

namespace Nagasena.Proc.IO {

  internal sealed class IntegerValueScriber : ValueScriberBase {

    public static readonly IntegerValueScriber instance;
    static IntegerValueScriber() {
      instance = new IntegerValueScriber();
    }

    private IntegerValueScriber() : base(new QName("exi:integer", ExiUriConst.W3C_2009_EXI_URI)) {
    }

    public override short CodecID {
      get {
        return Scriber.CODEC_INTEGER;
      }
    }

    public override int getBuiltinRCS(int simpleType, Scriber scriber) {
      return BuiltinRCS.RCS_ID_INTEGER;
    }

    ////////////////////////////////////////////////////////////

    public override bool process(string value, int tp, EXISchema schema, Scribble scribble, Scriber scriber) {
      if (!trimWhitespaces(value)) {
        return false;
      }
      int pos = startPosition;
      bool useIntValue = true;
      int intValue = 0;
      long longValue = 0;
      bool isNegative = false;
      char c = value[pos];
      if (c == '-' || c == '+') {
        ++pos;
        if (c != '+') {
          isNegative = true;
        }
      }
      bool foundNonZero = false;
      int st = -1;
      for (int n_digits = 0; pos < limitPosition; pos++) {
        switch (c = value[pos]) {
          case '0':
            if (!foundNonZero) {
              continue;
            }
            else {
              if (++n_digits < 10) {
                intValue *= 10;
              }
              else {
                if (n_digits == 10) {
                  longValue = 10 * (long)intValue;
                  useIntValue = false;
                }
                else {
                  if (n_digits < 19) {
                    longValue *= 10;
                  }
                  else {
                    longValue = long.MinValue;
                    ++pos;
                    goto posLoopBreak;
                  }
                }
              }
            }
            break;
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            if (!foundNonZero) {
              foundNonZero = true;
              st = pos;
            }
            int deposit = ((int)c) - 48;
            if (++n_digits < 10) {
              intValue = 10 * intValue + deposit;
            }
            else {
              if (n_digits == 10) {
                longValue = 10 * (long)intValue + deposit;
                useIntValue = false;
              }
              else {
                if (n_digits < 19) {
                  longValue = 10 * longValue + deposit;
                }
                else {
                  longValue = long.MinValue;
                  ++pos;
                  goto posLoopBreak;
                }
              }
            }
            break;
          default:
            return false;
        }
      }
      posLoopBreak:
      if (pos != limitPosition) {
        Debug.Assert(longValue == long.MinValue);
        for (; pos != limitPosition; pos++) {
          c = value[pos];
          if (c < '0' || '9' < c) {
            return false;
          }
        }
      }
      Debug.Assert((st >= 0 && foundNonZero) || (!foundNonZero && longValue == 0 && intValue == 0 && useIntValue));
      if (!foundNonZero && isNegative) {
        isNegative = false;
      }

      if (schema.isIntegralSimpleType(tp)) {
        switch (scribble.intValue2 = schema.getWidthOfIntegralSimpleType(tp)) {
          /// scribble.booleanValue is relevant only to default integer representation.
          /// Values "true" and "false" represent signs of minus and plus. 
          case EXISchema.INTEGER_CODEC_DEFAULT:
            break;
          case EXISchema.INTEGER_CODEC_NONNEGATIVE:
            if (isNegative) {
              return false;
            }
            if (scribble.booleanValue2 = useIntValue) {
              scribble.intValue1 = intValue;
            }
            else if ((scribble.longValue = longValue) == long.MinValue) {
              scribble.stringValue1 = value.Substring(st, limitPosition - st);
            }
            return true;
          default:
            if (!useIntValue && longValue == long.MinValue) {
              return false;
            }
            if (isNegative) {
              if (useIntValue) {
                intValue = 0 - intValue;
              }
              else {
                longValue = 0 - longValue;
              }
            }
            int minInclusiveFacet = schema.getMinInclusiveFacetOfIntegerSimpleType(tp);
            switch (schema.getTypeOfVariant(minInclusiveFacet)) {
              case EXISchema.VARIANT_INT:
                int minInclusiveIntValue = schema.getIntValueOfVariant(minInclusiveFacet);
                if (useIntValue) {
                  if (intValue < minInclusiveIntValue || (intValue -= minInclusiveIntValue) > NBIT_INTEGER_RANGES[scribble.intValue2]) {
                    return false;
                  }
                }
                else {
                  if (longValue < minInclusiveIntValue || (longValue -= minInclusiveIntValue) > NBIT_INTEGER_RANGES[scribble.intValue2]) {
                    return false;
                  }
                  intValue = (int)longValue;
                }
                break;
              case EXISchema.VARIANT_LONG:
                long minInclusiveLongValue = schema.getLongValueOfVariant(minInclusiveFacet);
                if (useIntValue) {
                  if (intValue < minInclusiveLongValue || (intValue -= (int)minInclusiveLongValue) > NBIT_INTEGER_RANGES[scribble.intValue2]) {
                    return false;
                  }
                }
                else {
                  if (longValue < minInclusiveLongValue || (longValue -= minInclusiveLongValue) > NBIT_INTEGER_RANGES[scribble.intValue2]) {
                    return false;
                  }
                  intValue = (int)longValue;
                }
                break;
              default:
                return false;
            }
            scribble.intValue1 = intValue;
            return true;
        }
      }
      scribble.intValue2 = EXISchema.INTEGER_CODEC_DEFAULT;
      scribble.booleanValue1 = isNegative;
      if (scribble.booleanValue2 = useIntValue) {
        scribble.intValue1 = intValue;
      }
      else if ((scribble.longValue = longValue) == long.MinValue) {
        scribble.stringValue1 = value.Substring(st, limitPosition - st);
      }
      return true;
    }

    /// <summary>
    /// Store an unsigned int value into scribble.
    /// (For use by EXIOptionsEncoder)
    /// </summary>
    public bool processUnsignedInt(int uintValue, Scribble scribble) {
      Debug.Assert(uintValue >= 0);
      scribble.intValue2 = EXISchema.INTEGER_CODEC_NONNEGATIVE;
      scribble.booleanValue2 = true;
      scribble.intValue1 = uintValue;
      return true;
    }

    public override void scribe(string value, Scribble scribble, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      scribeIntegerValue(scribble.booleanValue2, scribble.longValue, scribble.stringValue1, scribble.intValue1, scribble.intValue2, scribble.booleanValue1, channelStream, scriber);
    }

    ////////////////////////////////////////////////////////////

    private class IntegerValue {
      internal bool useIntValue;
      internal long longValue;
      internal string digits;
      internal int intValue;
      internal int width;
      internal bool isNegative;
      internal IntegerValue(bool useIntValue, long longValue, string digits, int intValue, int width, bool isNegative) {
        this.useIntValue = useIntValue;
        this.longValue = longValue;
        this.digits = digits;
        this.intValue = intValue;
        this.width = width;
        this.isNegative = isNegative;
      }
    }

    public override object toValue(string value, Scribble scribble, Scriber scriber) {
      return new IntegerValue(scribble.booleanValue2, scribble.longValue, scribble.stringValue1, scribble.intValue1, scribble.intValue2, scribble.booleanValue1);
    }

    public override void doScribe(object value, int localName, int uri, int tp, Stream channelStream, Scriber scriber) {
      IntegerValue integerValue = (IntegerValue)value;
      scribeIntegerValue(integerValue.useIntValue, integerValue.longValue, integerValue.digits, integerValue.intValue, integerValue.width, integerValue.isNegative, channelStream, scriber);
    }

    ////////////////////////////////////////////////////////////

    private void scribeIntegerValue(bool useIntValue, long longValue, string digits, int intValue, int width, bool isNegative, Stream ostream, Scriber scriber) {
      switch (width) {
        case EXISchema.INTEGER_CODEC_DEFAULT:
          scriber.writeBoolean(isNegative, ostream);
          if (useIntValue) {
            Debug.Assert(isNegative && intValue != 0 || !isNegative);
            scriber.writeUnsignedInteger32(isNegative ? intValue - 1 : intValue, ostream);
          }
          else if (longValue != long.MinValue) {
            scriber.writeUnsignedInteger64(isNegative ? longValue - 1 : longValue, ostream);
          }
          else {
            BigInteger bint = Convert.ToInt64(digits);
            if (isNegative) {
              bint = bint - BigInteger.One;
            }
            scriber.writeUnsignedInteger(bint, ostream);
          }
          break;
        case EXISchema.INTEGER_CODEC_NONNEGATIVE:
          if (useIntValue) {
            scriber.writeUnsignedInteger32(intValue, ostream);
          }
          else if (longValue != long.MinValue) {
            scriber.writeUnsignedInteger64(longValue, ostream);
          }
          else {
            scriber.writeUnsignedInteger(Convert.ToInt64(digits), ostream);
          }
          break;
        default:
          scriber.writeNBitUnsigned(intValue, width, ostream);
          break;
      }
    }

  }

}