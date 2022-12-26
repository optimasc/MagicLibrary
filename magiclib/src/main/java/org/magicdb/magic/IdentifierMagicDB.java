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


import java.io.DataInput;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;

import com.willcode4beer.infix.InfixPostfixEvaluator;
import java.util.Properties;

/** Implements a ressource identifier plugin based on a magic
 *  text file database that contains signature information. It supports
 *  identifying data from both Input streams that supports {@link InputStream#mark(int) }
 *  or {@link RandomAccessFile}
 *
 *  <p>The following steps should be used to use this library
 *   <ul>
 *    <li>Call one of the constructors to create an instance of the class.</li>
 *    <li>Call {@link #initLibrary()} to parse the magic text file database </li>
 *    <li>When a resource needs to be identified, call {@link #loadMetadata(java.io.DataInput, java.util.Properties)}  to
 *       identify it. The returned property table contains the different key values</li>
 *    <li>Call {@link #doneLibrary()} to free allocated resources</li>
 *
 *   </ul>
 * </p>
 *
 */
public class IdentifierMagicDB
{
    /** This is the first line of the MAGIC file */
    private static final String MAGIC_HEADER = "# FILE_ID DB";
    /** This is start the second line of the MAGIC file */
    private static final String MAGIC_DATE_HEADER = "# Date:";
    /** This is start the third line of the MAGIC file */
    private static final String MAGIC_SOURCE_HEADER = "# Source:";

    private LineNumberReader magicFile;

    /** Contains the data input stream */
//    private DataInputStream dataInput;

    /** This is the main list of magic entries that have been parsed */
    private LinkedList entries;
    private InputStream magicInputStream;

    /** Cache of the streamLength */
    private long streamLength;

    public static final String PLUGIN_ID = "org.magicdb.magic";

    /** Property key return name for title information. */
    public static final String TITLE_KEY = "title";
    /** Property key return name for creator/author information. */
    public static final String AUTHOR_KEY = "creator";
    /** Property key return name for file extension/file suffix information. */
    public static final String FILE_SUFFIX_KEY = "ext";
    /** Property key return name for sampling frequency information (in Hz) (Sound). */
    public static final String SAMPLING_RATE_KEY = "freq";
    /** Property key return name for image resolution (Image). */
    public static final String IMAGE_SIZE_KEY = "res";
    /** Property key return name for MIME type information. */
    public static final String MIME_KEY = "mime";
    /** Property key return name for Frame rate in frames per seconds (MovingImage). */
    public static final String FRAME_RATE_KEY = "frameRate";
    /** Property key return name for number of audio channels (Sound). */
    public static final String AUDIO_CHANNELS_KEY = "chn";
    /** Property key return name for FFID (File format identifier). */
    public static final String FFID_KEY = "fid";
    /** Property key return name for resource/file format comment in English. */
    public static final String COMMENT_KEY = "comment";


    // The following tables are used to convert from the magic property names
    // to the universal property names of filelib.

    // THE THREE TABLES ARE SYNCHRONIZED, CHANGING ONE TABLE WITHOUT ADAPTING THE
    // OTHER WILL CAUSE PROBLEMS.

    private  String[] INTERNAL_PROPERTY_TABLE =
    {
        FILE_SUFFIX_KEY,
        FFID_KEY,
        TITLE_KEY,
        AUTHOR_KEY,
        AUDIO_CHANNELS_KEY,
        SAMPLING_RATE_KEY,
        IMAGE_SIZE_KEY,
        MIME_KEY,
        FRAME_RATE_KEY
    };

    // Indicates if this property is an expression or not.
    private  Class[] INTERNAL_PROPERTY_EXPRESSION =
    {
        // filename suffix
        null,
        // FFID
        null,
        // title
        null,
        // creator
        null,
        // channels
        Integer.TYPE,
        // sampling rate
        Integer.TYPE,
        // res
        null,
        // mime
        null,
        // frameRate
        Float.TYPE
    };

    private String[] STANDARD_PROPERTY_TABLE = INTERNAL_PROPERTY_TABLE;
/*    {
        MetadataTerms.PROPERTY_FORMAT_SUFFIX,
        MetadataTerms.PROPERTY_FORMAT_FFID,
        MetadataTerms.PROPERTY_TITLE,
        MetadataTerms.PROPERTY_CREATOR,
        MetadataTerms.PROPERTY_FORMAT_SOUND_CHANNELS,
        MetadataTerms.PROPERTY_FORMAT_SOUND_SAMPLERATE,
        MetadataTerms.PROPERTY_FORMAT_IMAGE_RESOLUTION,
        MetadataTerms.PROPERTY_FORMAT_MIME,
        MetadataTerms.PROPERTY_FORMAT_VISUAL_FRAMERATE
    };*/

    /** Creates an instance of the identifier class based
     *  on the internally defined Magic DB file.
     */
    public IdentifierMagicDB()
    {
      super();
      this.magicInputStream = getClass().getResourceAsStream("/res/magic.db");
    }


    /** Creates an instance of the identifier class based
     *  on the specified Magic DB file.
     *
     *  @param magicInputStream  The magic DB to use
     */
    public IdentifierMagicDB(InputStream magicInputStream)
    {
      super();
      this.magicInputStream = magicInputStream;
    }


    /** Returns the current line number of the magic file parsing */
    public int getCurrentLineNumber()
    {
        return magicFile.getLineNumber();
    }


    /** This routine is used to convert a string that contains
     *  escape sequences to a string that only contains characters.
     *
     * @param s Input string possibly containing escape sequences
     * @return The string where each escape sequence has been replaced
     *  by the correct characters.
     */
    private String escapeString(String s)
    {
        int i,j;
        char c = 0;
        String temp;
        String outStr = "";

        i=0;
        while (i < s.length())
        {
            if ((s.charAt(i)=='\\') && (i < s.length()))
            {
                i++;
                switch (s.charAt(i))
                {
                    case '#' :
                        c = '#';
                        break;
                    case 'a' :
                        c = (char)7;
                        break;
                    case 'b' :
                        c = (char)8;
                        break;
                    case 'f' :
                        c = (char)12;
                        break;
                    case 'n' :
                        c = (char)10;
                        break;
                    case 'r' :
                        c = (char)13;
                        break;
                    case 't' :
                        c = (char)9;
                        break;
                    case 'v' :
                        c = (char)11;
                        break;
                    case '?' :
                        c = '?';
                        break;
                    case '\'' :
                        c = '\'';
                        break;
                    case '"' :
                        c = '"';
                        break;
                    case '\\' :
                        c = '\\';
                        break;
                    case ' ' :
                        c = ' ';
                        break;
                    case '<' :
                        c = '<';
                        break;
                    // Hexadecimal values
                    case 'x':
                        temp = "0x" + s.charAt(i+1);
                        i++;
                        if ((i+1) < s.length())
                        {
                            temp = temp + s.charAt(i+1);
                        }
                        i++;
                        j = Integer.decode(temp).intValue();
                        c = (char)j;
                        break;
                    default:
                        if ((s.charAt(i) >= '0') && (s.charAt(i) <= '7'))
                        {
                            temp = "0" + s.charAt(i);
                            if ((i+1) < s.length() && (Character.isDigit(s.charAt(i+1))))
                            {
                                i++;
                                temp = temp + s.charAt(i);
                                if ((i+1) < s.length() && (Character.isDigit(s.charAt(i+1))))
                                {
                                   i++;
                                   temp = temp + s.charAt(i);
                                }
                            }
                            j = Integer.decode(temp).intValue();
                            c = (char)j;
                        }
                } // end switch
            } // endif \ character
            else
            {
                c = s.charAt(i);
            }
            outStr = outStr + c;
            i++;
        }
        return outStr;
    }

    public void initLibrary() throws IOException
    {
        String inString;
        String[] tokens;
        InputStream inReader;
        char c;

        if (magicFile != null)
          return;

        MagicEntry extraEntry = null;
        MagicEntry currentEntry = null;
        entries = new LinkedList();

        if ((magicInputStream instanceof InputStream)==false)
            throw new IllegalArgumentException("Invalid initializartion");
        inReader=  (InputStream)magicInputStream;

        // Open the magicfile data
        magicFile = new LineNumberReader(new InputStreamReader(inReader));
        // Verify the magic header (The three first lines are checked)
        if (!((magicFile.readLine().equals(MAGIC_HEADER)) &&
           (magicFile.readLine().startsWith(MAGIC_DATE_HEADER)) &&
           (magicFile.readLine().startsWith(MAGIC_SOURCE_HEADER))))
        {
            // Throw EXCEPTION here!
            throw new IllegalArgumentException("Error: Invalid magic file");
        }
        // Start parsing the actual file.
        while (true)
        {
            inString = magicFile.readLine();
            if (inString == null)
                break;
            // Remove starting and ending white space.
            //inString = inString.trim();
            // Skip comment fields.
            if (inString.startsWith("#"))
                    continue;
            // Skip blank lines
            if (inString.length() == 0)
                    continue;

            // Now split by TAB Separators (one or more TAB separators
            // indicates exactly the same thing.
            tokens = inString.split("(\t)+");

            // There are at least three fields per line
            // otherwise the magic file is not correctly formatted.
            if (tokens.length < 3)
            {
                // Throw EXCEPTION here!
                throw new IllegalArgumentException("Error: Invalid token count near line "+Integer.toString(magicFile.getLineNumber()));
            }
            c = tokens[0].charAt(0);
            switch (c)
            {
               // Continuation line for identification
               case '&':
                   extraEntry = new MagicEntry();
                   // Skip the first character and get the numerical offset.
                   getOffset(tokens[0].substring(1),extraEntry);
                   if (currentEntry == null)
                   {
                        // Throw EXCEPTION here!
                        throw new IllegalArgumentException("Error: Invalid magic entry");
                   }
                   currentEntry.signatureLength += getType(tokens[1],extraEntry);
                   currentEntry.signatureLength += getOperator(tokens[2],extraEntry);
                   if (tokens.length > 3)
                      extraEntry.description = tokens[3];
                   currentEntry.matchEntries.add(extraEntry);
                   break;
               // Additional information line
               case '>':
                   extraEntry = new MagicEntry();
                   getOffset(tokens[0].substring(1),extraEntry);
                   if (currentEntry == null)
                   {
                        // Throw EXCEPTION here!
                        throw new IllegalArgumentException("Error: Invalid magic entry");
                   }
                  getType(tokens[1],extraEntry);
                  getOperator(tokens[2],extraEntry);
                  if (tokens.length > 3)
                     extraEntry.description = tokens[3];

                   currentEntry.extraMatchEntries.add(extraEntry);
                   break;
               default:
                 currentEntry = new MagicEntry();
                 currentEntry.lineNumber = magicFile.getLineNumber();
                 getOffset(tokens[0],currentEntry);
                 currentEntry.signatureLength += getType(tokens[1],currentEntry);
                 currentEntry.signatureLength += getOperator(tokens[2],currentEntry);
                 if (tokens.length > 3)
                    currentEntry.description = tokens[3];
                 entries.add(currentEntry);
                 break;

            }

        } // end while

    }

    /** This fills up the correct offset information in the MagicEntry
     *  class according to the string token containing the offset
     *  information.
     *
     * @param token The token should exclude the first continuation
     *   character (& or >) but can contain the indirect offset
     *   information.
     * @param entry
     */
    private void getOffset(String token, MagicEntry entry)
    {
        String s;
        String s1;
        int offset;
        int idx;
        int addValue;

        // Check if this is an indirect offset
        if (token.charAt(0) == '(')
        {
            if (token.indexOf(')')== -1)
            {
                // No ending parenthesis, so this offset
                // is not valid.

                // Throw EXCEPTION here!
                throw new IllegalArgumentException("Error: Invalid indirect offset in "+token);

            }
            s = token.substring(1,token.indexOf(')'));
            // Now we have a value without the parenthesis.
            idx = token.indexOf('.');
            // We must have a period indicating the data to read
            if (idx == -1)
            {
                // Throw EXCEPTION here!
                throw new IllegalArgumentException("Error: Invalid indirect offset in "+token);
            }
            // Get the type of the indirect offset
            char offsetType = token.charAt(idx+1);
            // Verify if this one of the valid types.
            entry.indirectOffsetType = MagicEntry.getIndirectType(offsetType);
            s1 = s.substring(0,idx-1);
            entry.offset = Integer.decode(s1).intValue();

            // If there is a plus sign, we must remove it...
            if (s.indexOf('+') != -1)
            {
                idx = s.indexOf('+');
                s1 = s.substring(idx+1);
                // Get the operator and value to add (if present)
                entry.extraOffset = Integer.decode(s1).intValue();
            } else
            if (s.indexOf('-') != -1)
            {
                idx = s.indexOf('-');
                s1 = s.substring(idx);
                // Get the operator and value to add (if present)
                entry.extraOffset = Integer.decode(s1).intValue();

            } else
              entry.extraOffset = 0;


        } else
          // Skip the first character and get the numerical offset.
          entry.offset = Integer.decode(token.substring(0)).intValue();
    }


    /** This fills up the correct type information in the MagicEntry
     *  class according to the string token containing and also sets
     *  the AND value accordingly.
     *
     * @param token The token should contain the type information
     * @param entry Magic entry to fill up with information
     */
    private int getType(String token, MagicEntry entry)
    {
        String s;
        String s1;
        int idx = token.indexOf('&');
        if (idx == -1)
        {
          // Non numeric values cannot have an AND operator
          if (entry.isNumeric() == false)
          {
            // Throw EXCEPTION here!
            throw new IllegalArgumentException("Error: & Operation is not allowed on string types.");
          }
          entry.andValue = MagicEntry.DEFAULT_AND_VALUE;
          entry.type =  MagicEntry.getType(token);
          return MagicEntry.getTypeSize(entry.type);
        }

        // We have an and operator.
        s = token.substring(idx+1);
        s1 = token.substring(0,idx);
        entry.type = MagicEntry.getType(s1);
        entry.andValue = Integer.decode(s).intValue();
        return MagicEntry.getTypeSize(entry.type);
    }

     /** This fills up the correct operator and value information in the
      * MagicEntry class according to the string token. The value shall
      * be an Object type indicating the value.
      *
     * @param token The token should contain the operator information
     * @param entry Magic entry to fill up with information
     */
    private long getOperator(String token, MagicEntry entry) throws UnsupportedEncodingException
    {
        String s;
        String s1;
        String s2;
        int signatureLength = 0;
        // Read the operator before, since we will be converting the string
        String convertedString = escapeString(token);
        char operator = token.charAt(0);

        // Special case for the ANY VALUE operator.
        if ((operator == 'x') && (token.length()==1))
        {
           entry.comparisonOperator = token.charAt(0);
           int length = MagicEntry.getTypeSize(entry.type);
           // Allocate default string length of 255 bytes
           if (length == 0)
               length = 255;
           entry.value = ByteBuffer.allocate(length);
           return 0;
        }


        switch (operator)
        {
            case '=':
                if (entry.isNumeric())
                {
                    entry.value = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                    entry.value.putLong(Long.decode(convertedString.substring(1)).longValue());
                } else
                {
                    // If this is a case insensitive string, upper case it
                    // immediately.
                    s2 = convertedString.substring(1);
                    if (entry.type == MagicEntry.TYPE_ISTRING)
                    {
                        s2 = s2.toUpperCase();
                    }
                    signatureLength += s2.length();
                    entry.value = ByteBuffer.wrap(s2.getBytes("ISO8859_1 "));
                }
                entry.comparisonOperator = token.charAt(0);
                break;
            // Special case for string types
            case '>':
                // Non numeric values cannot have an AND operator
                if (entry.isNumeric())
                {
                    entry.value = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                    entry.value.putLong(Long.decode(convertedString.substring(1)).longValue());
                } else
                {
                    // If this is a case insensitive string, upper case it
                    // immediately.
                    s2 = convertedString.substring(1);
                    if (entry.type == MagicEntry.TYPE_ISTRING)
                    {
                        s2 = s2.toUpperCase();
                    }
                    // Only the first character is valid.
                    String s3 = s2.substring(0, 1);
                    signatureLength += s3.length();
                    entry.value = ByteBuffer.wrap(s3.getBytes("ISO8859_1"));
                }
                entry.comparisonOperator = token.charAt(0);
                break;

            case '!':
            case '<':
            case '&':
            case '^':
                // Non numeric values starting with one of our reserved
                // characters.
                if (entry.isNumeric() == false)
                {
                    // If this is a case insensitive string, upper case it
                    // immediately.
                    s2 = convertedString.substring(0);
                    if (entry.type == MagicEntry.TYPE_ISTRING)
                    {
                        s2 = s2.toUpperCase();
                    }
                    entry.value = ByteBuffer.wrap(s2.getBytes("ISO8859_1"));
                    signatureLength += s2.length();
                    entry.comparisonOperator = '=';
                } else
                {
                    entry.comparisonOperator = token.charAt(0);
                    entry.value = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                    entry.value.putLong(Long.decode(convertedString.substring(1)).longValue());
                }
                break;
            default:
                // Default is equal comparison
                entry.comparisonOperator = '=';
                if (entry.isNumeric())
                {
                    entry.value = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                    entry.value.putLong(Long.decode(convertedString).longValue());
                } else
                {
                    // If this is a case insensitive string, upper case it
                    // immediately.
                    s2 = convertedString.substring(0);
                    if (entry.type == MagicEntry.TYPE_ISTRING)
                    {
                        s2 = s2.toUpperCase();
                    }
                    signatureLength += s2.length();
                    entry.value = ByteBuffer.wrap(s2.getBytes("ISO8859_1"));
                }
                break;
        }
        return signatureLength;
    }


    /** This method should be called once the identifier system
     *  is no longer required.
     *
     * @throws java.io.IOException
     */
    public void doneLibrary() throws IOException
    {
        // Close the magic file that we have previously locked
        magicFile.close();
    }


    /** This routinr is used to seek in the specified file. It takes
     *  care of all indirect and offset information.
     *
     * @param entry The MagicEntry associated containing the information
     *   where to seek to.
     * @param input The input steam.
     * @return true if the seek was successful, otherwise false.
     */
    private boolean seekInFile(MagicEntry entry, DataInput input) throws IOException
    {
        long offset;
        long length;

        /** Use the cached entry instead of calling streamLength() directly */
        length = streamLength;
        offset = 0;
        // The offset is greater than the length to seek in
        if (entry.offset > length)
        {
            return false;
        }

        // Normal offset
        if (entry.offset >= 0)
        {
            streamSeek(input,entry.offset);
        } else
        // Normal offset from end of file
        {
            if ((length+entry.offset) < 0)
                return false;
            streamSeek(input,length+entry.offset);
        }

        // Now is this an indirect offset
        if (entry.extraOffset != -1)
        {
            switch (entry.indirectOffsetType)
            {
                case MagicEntry.TYPE_BYTE:
                    offset = input.readUnsignedByte();
                    break;
                case MagicEntry.TYPE_LESHORT:
                     // get 2 bytes, unsigned 0..255
                    int low = input.readByte() & 0xff;
                    int high = input.readByte() & 0xff;

                    // combine into a signed short.
                    offset = (int)( high << 8 | low ) & 0x0000FFFF;
                    break;
                case MagicEntry.TYPE_LELONG:
                    // get 4 unsigned byte components, and accumulate into an int.
                    int accum = 0;
                    for ( int shiftBy=0; shiftBy<32; shiftBy+=8 )
                    {
                        accum |= ( input.readByte () & 0xff ) << shiftBy;
                    }
                    offset = accum & (long)0x00000000FFFFFFFF;
                    break;
                case MagicEntry.TYPE_BESHORT:
                    offset = input.readUnsignedShort();
                    break;
                case MagicEntry.TYPE_BELONG:
                    offset = input.readInt() & (long)0x00000000FFFFFFFF;
                    break;
                // This is currently impossible
                default:
                    throw new IllegalArgumentException("Unknown magic entry type.");
            }

            // Validate the indirect offsets
            if ((offset + entry.extraOffset) > length)
               return false;
            if ((offset + entry.extraOffset) < 0)
               return false;
            streamSeek(input,offset + entry.extraOffset);

        } // endif extraoffset is valid
        return true;
    }


    /** Return the maxiumum string length to read from a description containing
     *  a string specified.
     */
    private int getFormatStringLength(String s)
    {
       int startIndex;
       int endIndex;
       int index;
       int i;
       String outstr;
       // Get the [] separators
       startIndex = s.indexOf("[");
       endIndex = s.indexOf("]");
       if ((startIndex != -1) && (endIndex != -1) && (startIndex < endIndex))
       {
         // Retrieve the substring
         String substr = s.substring(startIndex,endIndex+1);
         index = substr.indexOf("%s");
         // Maximum length of the string.
         if (index != -1)
            return 255;
         index = substr.indexOf("%.");
         i = index+2;
         outstr = "";
         while (i < substr.length() && Character.isDigit(substr.charAt(i)))
         {
            outstr = outstr + substr.charAt(i);
            i++;
         }

         if (substr.charAt(i) != 's')
            throw new IllegalArgumentException("Error: Invalid string specifier in "+s);


         // Radix 10
         return Integer.parseInt(outstr,10);

       }
       return 255;
    }

    private ByteBuffer readData(DataInput input, MagicEntry entry) throws IOException
    {
        String s1;
        int i;
        byte bt;
        long value;
        int readLength;
        byte[] b1;
        ByteBuffer readValue = null;

            switch (entry.type)
            {
                    case MagicEntry.TYPE_BYTE:
                        if (streamAvailable(input) < 1)
                        {
                            throw new EOFException();
                        }
                        readValue = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                        readValue.order(ByteOrder.BIG_ENDIAN);
                        value = (long) (input.readByte() & entry.andValue);
                        readValue.putLong(value);
                        break;
                    case MagicEntry.TYPE_STRING:
                    case MagicEntry.TYPE_ISTRING:
                       byte[] b;
                        // If this is a comparison operation of x or >
                        // then the limit is specified in the comment string.
                        if ((entry.comparisonOperator == 'x') || (entry.comparisonOperator == '>'))
                        {
                              // Extract the number of bytes to actual read
                              readLength = getFormatStringLength(entry.description);
                              b1 = new byte[readLength];
                              i = 0;
                              // Read until readLength is reached
                              // or until a null character is found.
                              while (i < readLength)
                              {
                                if (streamAvailable(input) < 1)
                                {
                                    throw new EOFException();
                                }
                                 bt =  input.readByte();
                                 b1[i++] = bt;
                                 // The null character is always added to the array
                                 if (bt == 0)
                                    break;
                              }
                              b = new byte[i];
                              System.arraycopy(b1,0,b,0,i);
                        } else
                        {
                           int toRead = entry.value.limit();
                           b = new byte[toRead];
                           if (streamAvailable(input) < toRead)
                           {
                                throw new EOFException();
                           }
                           input.readFully(b);
                        }
                        // If this is a case insensitive comparison
                        // we must immediately uppercase the string
                        // before converting it to a byte array.
                        if (entry.type == MagicEntry.TYPE_ISTRING)
                        {
                            s1 = new String(b,"ISO8859_1");
                            s1 = s1.toUpperCase();
                            b = s1.getBytes("ISO8859_1");
                        }
                        readValue = ByteBuffer.wrap(b);
                        break;
                    case MagicEntry.TYPE_LESHORT:
                        if (streamAvailable(input) < 2)
                        {
                            throw new EOFException();
                        }
                         // get 2 bytes, unsigned 0..255
                        int low = input.readByte() & 0xff;
                        int high = input.readByte() & 0xff;

                        // combine into a signed short.
                        value  = (int)( high << 8 | low ) & 0x0000FFFF;
                        readValue = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                        readValue.order(ByteOrder.LITTLE_ENDIAN);
                        value = (long) (value & entry.andValue);
                        readValue.putLong(value);
                        break;
                    case MagicEntry.TYPE_LELONG:
                        if (streamAvailable(input) < 4)
                        {
                            throw new EOFException();
                        }
                        // get 4 unsigned byte components, and accumulate into an int.
                        int accum = 0;
                        for ( int shiftBy=0; shiftBy<32; shiftBy+=8 )
                        {
                            accum |= ( input.readByte () & 0xff ) << shiftBy;
                        }
                        readValue = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                        readValue.order(ByteOrder.LITTLE_ENDIAN);
                        value = (long) (accum & entry.andValue);
                        readValue.putLong(value);
                        break;
                    case MagicEntry.TYPE_BESHORT:
                        if (streamAvailable(input) < 2)
                        {
                            throw new EOFException();
                        }
                        readValue = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                        readValue.order(ByteOrder.BIG_ENDIAN);
                        value = (long)(input.readShort() & entry.andValue);
                        readValue.putLong(value);
                        break;
                    case MagicEntry.TYPE_BELONG:
                        if (streamAvailable(input) < 4)
                        {
                            throw new EOFException();
                        }
                        readValue = ByteBuffer.allocate(MagicEntry.LONG_SIZE / 8);
                        readValue.order(ByteOrder.BIG_ENDIAN);
                        value = (long)(input.readInt() & entry.andValue);
                        readValue.putLong(value);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown magic entry type.");

            }
           readValue.rewind();
           return readValue;
    }


    private boolean compareValues(ByteBuffer ope1, ByteBuffer ope2, MagicEntry entry)
    {

        boolean cmpResult = false;
        long l1;
        long l2;
        ope1.rewind();
        ope2.rewind();

        switch (entry.comparisonOperator)
        {
            // Any value is ok
            case 'x':
                cmpResult = true;
                break;
            case '=':
                if (entry.isNumeric())
                {
                    l1 = ope1.getLong();
                    l2 = ope2.getLong();
                    if (l1 == l2)
                        cmpResult = true;
                } else
                {
                  // String type where only the first character is compared
                  if (ope1.hasArray()==false)
                    throw new IllegalArgumentException("Comparison operator is not on array.");
                  if (ope2.hasArray()==false)
                    throw new IllegalArgumentException("Comparison operator is not on array.");
                    byte[] b1 = ope1.array();
                    byte[] b2 = ope2.array();
                    if (Arrays.equals(b1, b2)==true)
                        cmpResult=true;
                }
                break;
            case '!':
                if (entry.isNumeric())
                {
                    l1 = ope1.getLong();
                    l2 = ope2.getLong();
                    if (l1 != l2)
                        cmpResult = true;
                } else
                {
                  // String type where only the first character is compared
                  // String type where only the first character is compared
                  if (ope1.hasArray()==false)
                    throw new IllegalArgumentException("Comparison operator is not on array.");
                  if (ope2.hasArray()==false)
                    throw new IllegalArgumentException("Comparison operator is not on array.");
                    byte[] b1 = ope1.array();
                    byte[] b2 = ope2.array();
                    if (Arrays.equals(b1, b2)==false)
                        cmpResult=true;
                }
                break;
            case '>':
                if (entry.isNumeric())
                {
                    l1 = ope1.getLong();
                    l2 = ope2.getLong();
                    if (l1 > l2)
                        cmpResult = true;
                } else
                {
                   // String type where only the first character is compared
                  if (ope1.hasArray()==false)
                    throw new IllegalArgumentException("Comparison operator is not on array.");
                  if (ope2.hasArray()==false)
                    throw new IllegalArgumentException("Comparison operator is not on array.");
                    byte[] b1 = ope1.array();
                    byte[] b2 = ope2.array();
                    if (b1[0] > b2[0])
                        cmpResult=true;
                }
                break;
            case '<':
                 // Numeric only allowed for this type of comparison
              if (entry.isNumeric()==false)
                throw new IllegalArgumentException("Numeric only allowed for this type of comparison");
                if (ope1.getLong() < ope2.getLong())
                    cmpResult = true;
                break;
            case '&':
                 // Numeric only allowed for this type of comparison
              if (entry.isNumeric()==false)
                throw new IllegalArgumentException("Numeric only allowed for this type of comparison");
                if ((ope1.getLong() & ope2.getLong())!=0)
                    cmpResult = true;
                break;
            case '^':
                 // Numeric only allowed for this type of comparison
                if (entry.isNumeric()==false)
                  throw new IllegalArgumentException("Numeric only allowed for this type of comparison");
                if ((ope1.getLong() ^ ope2.getLong())!=0)
                    cmpResult = true;
                break;

        }
        ope1.rewind();
        ope2.rewind();
        return cmpResult;
    }


    private String getExtraInfo(DataInput input, MagicEntry entry) throws IOException
    {
      MagicEntry additionalEntry;
      ByteBuffer magicObject;
      String s;
      int i;

      if (seekInFile(entry,input)==false)
      {
         return null;
      }
      magicObject = readData(input, entry);
      if (compareValues(magicObject, entry.value, entry)==false)
         return null;

      if (entry.isNumeric())
        s = new PrintfFormat(entry.description).sprintf(magicObject.getLong());
      else
        s = new PrintfFormat(entry.description).sprintf(new String(magicObject.array()));

      return s;
    }


    private boolean compareMatchEntries(DataInput input, MagicEntry entry) throws IOException
    {

      MagicEntry additionalEntry;
      ByteBuffer magicObject;
      int i;

      // If no additional match entries, then simply return true
      if (entry.matchEntries == null)
          return true;

      // Check each match entry individually
      for (i = 0; i < entry.matchEntries.size(); i++)
      {
           additionalEntry =  (MagicEntry)entry.matchEntries.get(i);
           if (seekInFile(additionalEntry,input)==false)
           {
               return false;
           }
           magicObject = readData(input, additionalEntry);
           if (compareValues(magicObject, additionalEntry.value, additionalEntry)==false)
               return false;

      }
      return true;
    }


    private long streamAvailable(DataInput input) throws IOException
    {
        if (input instanceof InputStream)
        {
            InputStream is = (InputStream)input;
            return is.available();
        } else
        if (input instanceof RandomAccessFile)
        {
            RandomAccessFile r = (RandomAccessFile)input;
            return r.length()-r.getFilePointer();
        }
        return 0;
    }



    private long streamLength(DataInput input) throws IOException
    {
        if (input instanceof InputStream)
        {
            InputStream is = (InputStream)input;
            return is.available();
        } else
        if (input instanceof RandomAccessFile)
        {
            RandomAccessFile r = (RandomAccessFile)input;
            return r.length();
        }
        return 0;
    }

    /** This routine implements the seek method for both
     *  the RandomAccessFile and InputStream datatypes.
     */
    private void streamSeek(DataInput input, long pos) throws IOException
    {
        if (input instanceof InputStream)
        {
            InputStream is = (InputStream)input;
            if (pos == 0)
                is.reset();
            else
            {
                is.reset();
                is.skip(pos);
            }
        } else
        if (input instanceof RandomAccessFile)
        {
            RandomAccessFile r = (RandomAccessFile)input;
            r.seek(pos);
        }
    }


    /** This routine extracts the properties from an evaluated and concatenated
     *  string. We know that the main comment string contains the description of
     *  the file format, while everything within brackets contains information
     *  the key-value pairs for metadata.
     *
     * @param s The string to parse containing the properties in magic format
     * @return The properties of the resource
     */
    private void extractProperties(String s, Properties prop) throws IOException
    {
        String[] propTokens;
        int startPos;
        int endPos;
        int i;
        int j;
        int keyIdx;
        String s1;
        String key;
        String value;
        int indirective;
        String idinfo = "";
        String finalString = "";

        indirective = 0;

            startPos = s.indexOf('[');
            endPos = s.indexOf(']');
            // if startPos and endPos == -1 then no metadata section and return
            // the resulting string.
            if ((startPos == -1) && (endPos == -1))
            {
                finalString = s ;
            }
            if ((startPos >= 0) && (endPos >= 0) && (endPos > startPos))
            {
               for (i = 0; i < s.length(); i++)
               {
                  switch (s.charAt(i))
                  {
                   case '[':
                      indirective++;
                      break;
                   case ']':
                        if (i+1 >= s.length())
                        {
                          idinfo =idinfo+';';
                          break;
                        }
                        // There was a [ ] in the actual string
                        if  ((s.charAt(i+1) == ';') || (indirective == 1))
                           idinfo =idinfo+';';
                        indirective--;
                      break;
                   default:
                      if (indirective > 0)
                      {
                          idinfo =  idinfo + s.charAt(i);
                      }
                      else
                      {
                         finalString = finalString + s.charAt(i);
                      }
                      break;
                  } // end switch
               } // end for length
            }
            // Now split by ; characters
            propTokens = idinfo.split("(;)+");
            for (i = 0; i < propTokens.length; i++)
            {
                keyIdx = propTokens[i].indexOf('=');
                key = propTokens[i].substring(0,keyIdx);
                value = propTokens[i].substring(keyIdx+1);
                key = key.trim();
                value = value.trim();
                // Get the standard property name
                for (j = 0; j < INTERNAL_PROPERTY_TABLE.length; j++)
                {
                    if (key.equalsIgnoreCase(INTERNAL_PROPERTY_TABLE[j]))
                    {
                        // Evaluate the expression as required.
                        if (INTERNAL_PROPERTY_EXPRESSION[j]==Integer.TYPE)
                        {
                            try
                            {
                                value = Integer.toString(InfixPostfixEvaluator.evalInfixAsInt(value));
                            } catch (Exception e)
                            {
                                throw new NumberFormatException("Invalid numeric value");
                            }
                        }
                        else
                        if (INTERNAL_PROPERTY_EXPRESSION[j]==Float.TYPE)
                        {
                            try
                            {
                                value = Float.toString(InfixPostfixEvaluator.evalInfixAsFloat(value));
                            } catch (Exception e)
                            {
                                throw new NumberFormatException("Invalid numeric value");
                            }
                        }
                        key = STANDARD_PROPERTY_TABLE[j];
                        break;
                    }
                }
                prop.setProperty(key,value);
            }

        // Set format comment of file
        prop.setProperty(COMMENT_KEY, finalString);
    }

    /** From the specified input, try to identify the resource and return
     *  the filled property table.
     *
     * @param input The input that needs to be identified. The
     *   stream should be set at correct position when entering this method.
     * @param metadata The returned metadata, which can be one of the
     *   key values. The metadata table is NOT cleared in this method,
     *   it will simply overwrite existing properties.
     * @return true if at least one property was set, otherwise returns false
     * @throws IOException In case of I/O exception.
     */
    public boolean loadMetadata(DataInput input, Properties metadata)  throws IOException
    {
        MagicEntry entry;
        LinkedList foundEntries;
        int i;
        int largestIndex;
        ByteBuffer magicObject;
        MagicEntry entry1;
        MagicEntry entry2;
        String resultString;

        streamLength = streamLength(input);
        foundEntries = new LinkedList();

        for (i = 0; i < entries.size(); i++)
        {
            entry = (MagicEntry)entries.get(i);
            try
            {
                // Check, did the seek was ok? No, then continue
                // with next entry in the list.
                if (seekInFile(entry, input)==false)
                {
                    streamSeek(input, 0);
                    continue;
                }
            } catch (Exception e)
            {
                streamSeek(input,0);
                continue;
            }

            try
            {
                magicObject = readData(input, entry);
                if (compareValues(magicObject, entry.value, entry)==true)
                {
                   // Check all sub entry matches
                   if (compareMatchEntries(input, entry)==true)
                   {
                       foundEntries.add(entry);
                   } else
                       continue;
                }
             }
             // If this is an EOFException then this must surely not
             // be this filetype.
             catch (EOFException e)
             {
                 continue;
             }
        }

        // If we have found some matches. Do something about it.
        if (foundEntries.size() > 0)
        {
            // Now we shall classify according to the biggest signature length
            // to get the result which is the most probable.
            largestIndex = 0;
            for (i = 0; i < foundEntries.size(); i++)
            {
               entry1 = (MagicEntry)foundEntries.get(i);
               entry2 = (MagicEntry)foundEntries.get(largestIndex);
               if (entry1.signatureLength > entry2.signatureLength)
                   largestIndex = i;
            }

            // Get the final description string
            entry1 = (MagicEntry)foundEntries.get(largestIndex);
            resultString = entry1.description;
            if ((entry1.extraMatchEntries != null) && (entry1.extraMatchEntries.size() >0))
            {
                for (i = 0; i < entry1.extraMatchEntries.size();i++)
                {
                    resultString = resultString + getExtraInfo(input,(MagicEntry)entry1.extraMatchEntries.get(i));
                }
            }
            extractProperties(resultString, metadata);
            return true;
        }
            /*
             ResultString := EscapeToPascal(ResultString,code);
             if code <> 0 then
               begin
                 Error(e_InvalidString,LineNumber);
               end;
             { Now process the data, so that it is in the correct format }
             GetFileDescription := ProcessIdInfo(IdInfo, ResultString);
             { Now if the main entry was found then continue on }
             { with the sub entries                             }
{             If FoundTimes > 1 then
            Error(e_MultipleDefinitions,MagicEntry.MagicDbLineNr);}*/
        return false;

    }


}

/*

  $Log$

*/
