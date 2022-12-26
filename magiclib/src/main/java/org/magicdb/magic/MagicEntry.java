package org.magicdb.magic;
/*
 * Copyright (c) 2016 Optima SC Inc. Licensed under Apache License 1.1
 *
 * -----------------------------------------------------------------------------
 * THIS SOFTWARE IS NOT DESIGNED OR INTENDED FOR USE OR RESALE AS ON-LINE
 * CONTROL EQUIPMENT IN HAZARDOUS ENVIRONMENTS REQUIRING FAIL-SAFE
 * PERFORMANCE, SUCH AS IN THE OPERATION OF NUCLEAR FACILITIES, AIRCRAFT
 * NAVIGATION OR COMMUNICATION SYSTEMS, AIR TRAFFIC CONTROL, DIRECT LIFE
 * SUPPORT MACHINES, OR WEAPONS SYSTEMS, IN WHICH THE FAILURE OF THE
 * SOFTWARE COULD LEAD DIRECTLY TO DEATH, PERSONAL INJURY, OR SEVERE
 * PHYSICAL OR ENVIRONMENTAL DAMAGE ("HIGH RISK ACTIVITIES"). OPTIMA SC
 * SPECIFICALLY DISCLAIMS ANY EXPRESS OR IMPLIED WARRANTY OF FITNESS FOR
 * HIGH RISK ACTIVITIES.
 */

import java.util.*;
import java.nio.*;

/** Helper class for the IdentifierMagicDB class. This class
 *  contains the parsed magic.db file information.
 * 
 */
class MagicEntry 
{
  /** This is the offset in the file for this entry 
   *  A negative value indicates a search from the
   *  end of the file.
   */
  public long offset;
  
  /** This is the extra offset in the file for this entry if this 
   *  is an indirect offset. This value is set to -1 if there is
   *  no indirect offset.
   */
  public long extraOffset;
  
  /** This is the description associated with this entry. */
  public String description;
  
  /** This is the value to and with before doing the final comparison 
   *  If this value is 00000000FFFFFFFF then there is no value to and
   *  with (well, actually doing an AND operation on the read value 
   *  shall return the same value since this is an AND value).
   */
  public long andValue;
  
  /** This is the comparison value represented as an object type.
   */
  public ByteBuffer value;
  
  /** This is the comparison type to do on the operator. */
  public char comparisonOperator;
  
  /** This is the type of value to read to get the correct
   *  offset when there is an indirect offset
   */
  public int indirectOffsetType;
  
  /** This is the type of value to check. One of the below TYPE_XXXX
   *  constants below.
   */
  public int type;
  
  /** This is the line number associated with the start of this 
   *  entry.
   */
  public int lineNumber;
  
  /** This is a list of entries that should match the identification
   *  so that it is clearly identified. 
   */
  LinkedList matchEntries;
  
  
  /** This is a list of entries indicating extra information that should
   *  be displayed if this entry has been matched.
   */
  LinkedList extraMatchEntries;
  
  /** This is the calculated signature length. The signature length is 
   *  used to check the closes match if several signatures match the 
   *  entry.
   */
  int signatureLength;
  
  /** Default and value when there is no AND operator */
  public static final long DEFAULT_AND_VALUE = (long)0x00000000FFFFFFFFL;

  /** The field to read is invalid */
  public static final byte TYPE_INVALID = 0;
  /** The field to read is an 8-bit unsigned value */
  public static final byte TYPE_BYTE = 1;
  /** The field to read is an array of bytes */
  public static final byte TYPE_STRING = 2;
  /** The field to read is a 16-bit unsigned little-endian value */
  public static final byte TYPE_LESHORT = 3;
  /** The field to read is a 32-bit unsigned little-endian value */
  public static final byte TYPE_LELONG = 4;
  /** The field to read is a 16-bit unsigned big-endian value */
  public static final byte TYPE_BESHORT = 5;
  /** The field to read is a 32-bit unsigned big-endian value */
  public static final byte TYPE_BELONG = 6;
  /** The field to read is a case insensitive array of bytes */
  public static final byte TYPE_ISTRING = 7;

  /** Size determination in bits */
  public static final int SHORT_SIZE = 16;
  public static final int BYTE_SIZE = 8;
  public static final int LONG_SIZE = 64;
  public static final int INT_SIZE = 32;

  /** This is the type mapping array with a one to one mapping 
   *  with the type integer values.
   */
  private static final String[] TYPE_STRINGS = 
  {
      "byte",
      "string",
      "leshort",
      "lelong",
      "beshort",
      "belong",
      "string/c",
  };
          
  private static final int[] TYPE_SIZE_VALUES = 
  {
      0,
      BYTE_SIZE / 8,
      0,
      SHORT_SIZE / 8,
      LONG_SIZE / 8,
      SHORT_SIZE / 8,
      LONG_SIZE / 8,
      0,
  };
  
  private static final int[] TYPE_VALUES = 
  {
      TYPE_BYTE,
      TYPE_STRING,
      TYPE_LESHORT,
      TYPE_LELONG,
      TYPE_BESHORT,
      TYPE_BELONG,
      TYPE_ISTRING,
  };
  
  public MagicEntry()
  {
      // Create the instance of the lists...
      matchEntries = new LinkedList();
      extraMatchEntries = new LinkedList();
      extraOffset = -1;
      
  }

  /** Convert a string in the magic file format to the
   *  internal String representation format as used internally
   *  by Java.
   * 
   *  The conversion takes care of 
   */
  public byte[] convertValueString(String in)
  {
      int i;
      String s  = "";
      for (i = 0; i < in.length(); i++)
      {
          if (in.charAt(i) == '\\')
          {
              
          }
      }
      return null;
  }
  
  
  /** Returns true if the type of this value is a numeric
   *  value, otherwise returns false.
   */
  public boolean isNumeric()
  {
      if ((type == TYPE_STRING) || (type == TYPE_ISTRING))
          return false;
      return true;
  }
  
  /** Returns the type associated with this string value. */
  public static int getType(String type)
  {
      int i;
      
      for (i = 0; i < TYPE_VALUES.length; i++)
      {
        if (type.trim().equals(TYPE_STRINGS[i]))
        {
            return TYPE_VALUES[i];
        }
      }
      return -1;
  }
  
  /** Returns the native size associated with this type. */
  public static int getTypeSize(int typ)
  {
    return TYPE_SIZE_VALUES[typ];
  }
  
  /** Returns the indirect offset type according to the character
   *  specified. 
   * 
   * @param c
   * @return
   */
  public static int getIndirectType(char c)
  {
     switch (c)
     {
        case 'b':
        case 'B':
            return TYPE_BYTE; 
         case 'L':
             return TYPE_BELONG;
         case 'S':
             return TYPE_BESHORT;
         case 'l':
             return TYPE_LELONG;
         case 's':
             return TYPE_LESHORT;
         // Invalid value
         default:
             return TYPE_INVALID;
     }
  }
  
  
}

/*

  $Log$

*/
