/*
 * Sample application that uses the Metadata system
 * actually identify files. Very simple application
 * that probably can be refined.
 */

package org.magicdb.magic.application;


import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Properties;
import org.magicdb.magic.IdentifierMagicDB;

public class Main
{

  private static String sWild = "";

  /** Structure / class containing parsed configuration
   *  information for running the program
   *
   */
  public static class Configuration
  {
    /** Indicates if the verification should be done recursively */
    public boolean recursive = false;
    /** Indicates if we are in debug mode */
    public boolean debug = false;
    /** Indicates if we are in brief or standard mode */
    public boolean brief = false;
    /** Indicate the root or file to check. */
    public File resource;

    /** This is the filename to identify */
    public String filename = null;

    /** This is the directory to identify */
    public String directory = null;

    /** Read count */
    public long readCount = 0;
    /** Identified count */
    public long identifiedCount = 0;

  }

  /** This is the status code for termination in case of error */
  public static final int ERR_STATUS = 1;

  public static String[] arguments;

  /** Fills a string with prepended spaces */
  public static final String fillTo(String s, int count)
  {
    StringBuffer buffer = new StringBuffer(s);
    for (int i = 0; i < count; i++)
    {
      buffer.insert(0, " ");
    }
    return buffer.toString();
  }

  /** Fills a string with prepended spaces */
  public static final String fillToRight(String s, int count)
  {
    StringBuffer buffer = new StringBuffer(s);
    for (int i = s.length(); i < count; i++)
    {
      buffer.append(" ");
    }
    return buffer.toString();
  }

  private static String replaceWildcards(String wild)
  {
    StringBuffer buffer = new StringBuffer();

    char[] chars = wild.toCharArray();

    for (int i = 0; i < chars.length; ++i)
    {
      if (chars[i] == '*')
        buffer.append(".*");
      else if (chars[i] == '?')
        buffer.append(".");
      else if ("+()^$.{}[]|\\".indexOf(chars[i]) != -1)
        buffer.append('\\').append(chars[i]);
      else
        buffer.append(chars[i]);
    }

    return buffer.toString();

  }// end replaceWildcards method


  /** Extract for one file
   * @throws IOException
   * @throws FileNotFoundException */
  private static void extractFromResource(IdentifierMagicDB extractor, Configuration config, File fs) throws FileNotFoundException, IOException
  {
    String name = fs.getAbsolutePath();
    // Cannot read this file
    try
    {
      if (fs.canRead() == false)
      {
        System.err.println("Error: Could not read " + name + ", skipping.");
        return;
      }
    } catch (SecurityException e)
    {
      System.err.println("Error: Could not read " + name + ", skipping.");
      return;
    }
    System.out.println("");
    if (config.brief == true)
    {
      System.out.print(fillToRight(fs.getName(), 32));
    } else
    {
      System.out.println(">> " + name);
    }
    Properties metadata = new Properties();
    RandomAccessFile ds = new RandomAccessFile(fs,"r");
    extractor.loadMetadata(ds, metadata);
    ds.close();
    config.readCount++;
    // Print the properties for this file
    if (metadata.size() > 0)
    {
      if (metadata.containsKey(IdentifierMagicDB.MIME_KEY)
          || (metadata.containsKey(IdentifierMagicDB.COMMENT_KEY)))

      {
        config.identifiedCount++;
      }

      if (config.brief)
      {
        String s = "";
        if (metadata.containsKey(IdentifierMagicDB.COMMENT_KEY))
        {
          s = metadata.get(IdentifierMagicDB.COMMENT_KEY).toString();
          System.out.print(s);
        }
        if (metadata.containsKey(IdentifierMagicDB.MIME_KEY))
        {
          s = metadata.get(IdentifierMagicDB.MIME_KEY).toString();
          System.out.print(", " + s);
        }
        if (metadata.containsKey(IdentifierMagicDB.TITLE_KEY))
        {
          System.out.println();
          s = metadata.get(IdentifierMagicDB.TITLE_KEY).toString();
          System.out.print(fillTo(s, 16));
        }
      } else
      {
        Enumeration keys = metadata.keys();

        while (keys.hasMoreElements())
        {
          String s = (String) keys.nextElement().toString();
          System.out.print(Main.fillTo(s, 16));
          System.out.print('=');
          System.out.println(metadata.get(s).toString());

        }
     }
    }
  }


  /** Recursively calls this routine to populate the file listings to process
   * @throws IOException
   * @throws FileNotFoundException */
  private static void extractRecursive(IdentifierMagicDB extractor, Configuration config, File fs) throws FileNotFoundException, IOException
  {
    int i;
    String s;

    /* A single resource file has been specified */
    if (fs.isDirectory() == false)
    {

      return;
    /* A directory has been specified but with no recursivity */
    } else if ((fs.isDirectory() == true) && (config.recursive == false))
    {
      return;
    }

    String[] listStr = fs.list();
    File f;

    for (i = 0; i < listStr.length; i++)
    {
      s = fs.getPath() + fs.separator + listStr[i];
      f = new File(s);
      if ((f.isDirectory()))
      {
        /*                if (debug)
                        {
                          System.err.println("Populating "+f.toURI().toASCIIString());
                        }*/
        extractRecursive(extractor, config, f);
      } else if ((f.isFile()) && (f.isHidden() == false))
      {
        try {
        extractFromResource(extractor,config,f);
        } catch (IOException e)
        {
           System.err.println("Error: Could not read " + f.getName() + ", skipping.");
        }
      }
    }

  }

   /**
    * Display (to standard output) package details for provided Package.
    *
    * @param pkg Package whose details need to be printed to standard output.
    */
   private static void displayPackageDetails(final Package pkg)
   {
      final String name = pkg.getName();
      System.out.println(name);
      System.out.println("\tSpec Title/Version: " + pkg.getSpecificationTitle() + " " + pkg.getSpecificationVersion());
      System.out.println("\tSpec Vendor: " +  pkg.getSpecificationVendor());      System.out.println("\tImplementation: " + pkg.getImplementationTitle() + " " + pkg.getImplementationVersion());

      System.out.println("\tImplementation: " + pkg.getImplementationTitle() + " " + pkg.getImplementationVersion());
      System.out.println("\tImplementation Vendor: " + pkg.getImplementationVendor());
   }


  private static void dumpPackageInformation()
  {
 final Package[] packages = Package.getPackages();
      for (final Package pkg : packages)
      {
         final String name = pkg.getName();
         if (   !name.startsWith("sun") && !name.startsWith("java"))
         {
            displayPackageDetails(pkg);
         }
      }
  }

  /** Process command line parameters and return configuration
   *  information
   *
   * @param args
   * @param registry
   */
  private static void processParameters(Configuration config, String[] args)
  {
    int i;
    File f;

    if (args.length == 0)
    {
      System.out.println("MagicDB File Identifier - Copyright (c) Optima SC 2011");
      System.out.println(" file [-r] filespec");
      System.out.println(" options:");
      System.out.println("  -r  : Recursively scan directories");
      System.out.println("  -d  : Debug mode");
      System.out.println("  -cb : Check brief (less verbose)");
      System.out.println("  --version : Return version information on library");
      System.out
          .println("  filespec : Filemask and directory specification where to scan");
      System.out.println("Built in plugins");

      System.exit(ERR_STATUS);
    }
    // Determine the command line options
    for (i = 0; i < args.length; i++)
    {
      if (args[i].equals("-r"))
        config.recursive = true;
      else if (args[i].equals("-d"))
        config.debug = true;
      else if (args[i].equals("-cb"))
        config.brief = true;
      else if (args[i].equals("--version"))
      {
          // Return information on the magic library implementation */
          Package magicLibraryPackage = org.magicdb.magic.IdentifierMagicDB.class.getPackage();
          System.out.println(magicLibraryPackage.getImplementationTitle() + " " + magicLibraryPackage.getImplementationVersion());
          System.exit(0);
      }

    }

    if (config.debug)
    {
      System.err.println("Command-line Arguments:");
      for (i = 0; i < args.length; i++)
      {
        if (config.debug == true)
          System.err.println("(" + Integer.toString(i) + "): " + args[i]);
      }

    }

    // Determine the actual filenames
    for (i = 0; i < args.length; i++)
    {
      if (args[i].equals("-r"))
        continue;
      if (args[i].equals("-d"))
        continue;
      if (args[i].equals("-cb"))
        continue;
      {
        config.resource = new File(args[i]);
      }
    }
  }

  /**
   * @param args
   *          the command line arguments
   */
  public static void main(String[] args) throws IOException
  {

    BufferedInputStream inputStream = null;
    IdentifierMagicDB extractor = new IdentifierMagicDB();
    extractor.initLibrary();

    Configuration config = new Configuration();

    processParameters(config, args);

    sWild = replaceWildcards(sWild);

    if (config.debug)
    {
      System.err.println("Magic file parse complete.");
    }
    try
    {
      extractRecursive(extractor,config,config.resource);
    } catch (Exception e)
    {
      e.printStackTrace(System.err);
      System.err.println("Error: " + e.getMessage());
    }


    System.out.println();
    System.out.println("Finished.");
    System.out.println(Long.toString(config.identifiedCount) + "/"
        + Long.toString(config.readCount));
    double percentCount = (double) (config.identifiedCount / (double) config.readCount) * 100;
    System.out.println(Long.toString((long) percentCount) + "% identified.");
    extractor.doneLibrary();

  }

}
