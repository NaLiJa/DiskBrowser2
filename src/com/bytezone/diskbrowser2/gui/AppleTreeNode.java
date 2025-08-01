package com.bytezone.diskbrowser2.gui;

import static com.bytezone.utility.Utility.formatText;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bytezone.filesystem.AppleContainer;
import com.bytezone.filesystem.AppleFile;
import com.bytezone.filesystem.AppleFilePath;
import com.bytezone.filesystem.AppleFileSystem;
import com.bytezone.utility.Utility;

import javafx.scene.image.Image;

// -----------------------------------------------------------------------------------//
public class AppleTreeNode
// -----------------------------------------------------------------------------------//
{
  private static IconMaker icons = new IconMaker ();

  private File localFile;         // local folder or local file with valid extension
  private Path path;
  private int extensionNo;

  private AppleFile appleFile;
  private AppleFileSystem appleFileSystem;

  private final String name;
  private final String prefix;
  private final String suffix;

  private final String sortString;

  // ---------------------------------------------------------------------------------//
  public AppleTreeNode (AppleFileSystem appleFileSystem)
  // ---------------------------------------------------------------------------------//
  {
    this.appleFileSystem = appleFileSystem;

    if (appleFileSystem.isHybridComponent ())    // one of two file systems in FsHybrid
      name = appleFileSystem.getFileSystemType ().toString ();    // DOS, CPM etc
    else
      name = appleFileSystem.getFileName ();

    sortString = name.toLowerCase ();
    suffix = "";
    prefix = sortString;
  }

  // ---------------------------------------------------------------------------------//
  public AppleTreeNode (AppleFile appleFile)
  // ---------------------------------------------------------------------------------//
  {
    this.appleFile = appleFile;

    appleFileSystem = appleFile.getEmbeddedFileSystem ();         // usually null

    name = appleFile.getFileName ();

    sortString = name.toLowerCase ();
    suffix = "";
    prefix = sortString;
  }

  // ---------------------------------------------------------------------------------//
  // File will be either a local folder or a local file with a valid suffix. A folder's
  // children will be populated, but a file will only be converted to an AppleFileSystem
  // when the tree node is expanded or selected. See checkForFileSystem() below.
  // ---------------------------------------------------------------------------------//
  public AppleTreeNode (File file)
  // ---------------------------------------------------------------------------------//
  {
    assert !file.isHidden ();

    this.path = file.toPath ();
    this.localFile = file;

    if (path.getNameCount () == 0)
      name = path.toString ();
    else
      name = path.getName (path.getNameCount () - 1).toString ();

    sortString = name.toLowerCase ();

    if (file.isDirectory ())
    {
      suffix = "";
      prefix = sortString;
      extensionNo = -1;
    }
    else
    {
      suffix = AppleTreeView.fileSystemFactory.getSuffix (file.getName ());
      prefix = sortString.substring (0, name.length () - suffix.length ());
      extensionNo = AppleTreeView.fileSystemFactory.getSuffixNumber (file.getName ());
    }
  }

  // ---------------------------------------------------------------------------------//
  // Called from:
  //   AppleTreeItem.getChildren()  - if the node is OPENED before it is SELECTED
  //   AppleTreeView.itemSelected() - if the node is SELECTED before it is OPENED
  // ---------------------------------------------------------------------------------//
  void checkForFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    if (isLocalFile () && appleFileSystem == null)
      appleFileSystem = AppleTreeView.fileSystemFactory.getFileSystem (path);
  }

  // ---------------------------------------------------------------------------------//
  String getName ()
  // ---------------------------------------------------------------------------------//
  {
    return name;
  }

  // ---------------------------------------------------------------------------------//
  int getExtensionNo ()
  // ---------------------------------------------------------------------------------//
  {
    return extensionNo;
  }

  // ---------------------------------------------------------------------------------//
  String getCatalogLine ()
  // ---------------------------------------------------------------------------------//
  {
    return name;
  }

  // ---------------------------------------------------------------------------------//
  AppleFile getAppleFile ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFile;
  }

  // ---------------------------------------------------------------------------------//
  AppleFileSystem getAppleFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFileSystem;
  }

  // ---------------------------------------------------------------------------------//
  File getLocalFile ()
  // ---------------------------------------------------------------------------------//
  {
    return localFile;
  }

  // ---------------------------------------------------------------------------------//
  Path getPath ()
  // ---------------------------------------------------------------------------------//
  {
    return path;
  }

  // ---------------------------------------------------------------------------------//
  Image getImage ()
  // ---------------------------------------------------------------------------------//
  {
    if (isLocalDirectory () || isAppleFolder ())    // must precede appleFile
      return icons.folderImage;

    if (isCompressedLocalFile ())
      return icons.zipImage;

    if (appleFile != null)                          // must precede isAppleFileSystem()
    {
      if (isAppleFileSystem ())
        return icons.diskImage;
      return icons.getImage (appleFile);
    }

    if (isLocalFile () || isAppleFileSystem ())
      return icons.diskImage;

    System.out.println ("No image");                // should never happen
    return null;
  }

  // ---------------------------------------------------------------------------------//
  boolean isLocalFile ()
  // ---------------------------------------------------------------------------------//
  {
    return localFile != null && localFile.isFile ();
  }

  // ---------------------------------------------------------------------------------//
  boolean isCompressedLocalFile ()
  // ---------------------------------------------------------------------------------//
  {
    return localFile != null && (suffix.equals ("zip") || suffix.equals ("gz"));
  }

  // ---------------------------------------------------------------------------------//
  boolean isLocalDirectory ()
  // ---------------------------------------------------------------------------------//
  {
    return localFile != null && localFile.isDirectory ();
  }

  // ---------------------------------------------------------------------------------//
  boolean isAppleFile ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFile != null;
  }

  // ---------------------------------------------------------------------------------//
  boolean isAppleFileSystem ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFileSystem != null;       // includes embedded FS
  }

  // ---------------------------------------------------------------------------------//
  boolean isAppleFolder ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFile != null && appleFile.isFolder ();
  }

  // ---------------------------------------------------------------------------------//
  boolean isAppleForkedFile ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFile != null && appleFile.isForkedFile ();
  }

  // Data or Resource
  // ---------------------------------------------------------------------------------//
  //  boolean isAppleFork ()
  //  // ---------------------------------------------------------------------------------//
  //  {
  //    return appleFile != null && appleFile.isFork ();
  //  }

  // ---------------------------------------------------------------------------------//
  boolean isAppleContainer ()
  // ---------------------------------------------------------------------------------//
  {
    if (appleFileSystem != null)
      return true;

    return appleFile != null && appleFile instanceof AppleContainer;
    //        && (appleFile instanceof AppleContainer || appleFile.isForkedFile ());
  }

  // ---------------------------------------------------------------------------------//
  boolean isAppleDataFile ()
  // ---------------------------------------------------------------------------------//
  {
    return appleFile != null && !isAppleContainer ();
  }

  // ---------------------------------------------------------------------------------//
  boolean hasSubdirectories ()
  // ---------------------------------------------------------------------------------//
  {
    if (appleFile instanceof AppleFilePath afp)
      return afp.getFullFileName ().indexOf (afp.getSeparator ()) > 0;

    return false;
  }

  // ---------------------------------------------------------------------------------//
  List<AppleTreeNode> listAppleFiles ()
  // ---------------------------------------------------------------------------------//
  {
    List<AppleTreeNode> children = new ArrayList<> ();

    if (appleFileSystem != null)
    {
      for (AppleFile file : appleFileSystem.getFiles ())
        if (displayFile (file))
          children.add (new AppleTreeNode (file));

      for (AppleFileSystem fs : appleFileSystem.getFileSystems ())
        children.add (new AppleTreeNode (fs));
    }

    if (appleFile != null)
    {
      if (appleFile instanceof AppleContainer ac)
      {
        for (AppleFile file : ac.getFiles ())
          if (displayFile (file))
            children.add (new AppleTreeNode (file));

        for (AppleFileSystem fs : ac.getFileSystems ())
          children.add (new AppleTreeNode (fs));
      }

      //      if (appleFile.isForkedFile ())
      //        for (AppleFile file : ((AppleForkedFile) appleFile).getForks ())
      //          children.add (new AppleTreeNode (file));
    }

    return children;
  }

  // ---------------------------------------------------------------------------------//
  private boolean displayFile (AppleFile file)
  // ---------------------------------------------------------------------------------//
  {
    String fileName = file.getFileName ();

    if (fileName.length () == 0)
      return false;

    for (char c : fileName.toCharArray ())
      if (c < 32 || c == 0x7F)
        return false;

    return file.hasData ();
  }

  // ---------------------------------------------------------------------------------//
  private long getLocalFileSize ()
  // ---------------------------------------------------------------------------------//
  {
    try
    {
      if (path == null)
      {
        System.out.println ("null path");
        return -1;
      }
      return Files.size (path);
    }
    catch (IOException e)
    {
      e.printStackTrace ();
    }

    return -1;
  }

  // ---------------------------------------------------------------------------------//
  String getSortString ()
  // ---------------------------------------------------------------------------------//
  {
    return sortString;
  }

  // ---------------------------------------------------------------------------------//
  private String toDetailedString ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    String fileSizeText = "";
    String suffixText = "";

    if (extensionNo >= 0)
    {
      fileSizeText = String.format ("%,d", getLocalFileSize ());
      suffixText = suffix.substring (1);
    }

    if (isLocalFile ())
    {
      text.append (String.format ("Path ............ %s%n", path.toString ()));
      text.append (String.format ("Name ............ %s%n", name));
      text.append (String.format ("Sort string ..... %s%n", sortString));
      text.append (String.format ("Prefix .......... %s%n", prefix));
      text.append (String.format ("Suffix .......... %s%n", suffixText));
      text.append (String.format ("Extension no .... %d%n", extensionNo));
      text.append (String.format ("File size ....... %s", fileSizeText));
    }

    if (isAppleFile ())
    {
      text.append ("\n\n");
      text.append (appleFile.toString ());
    }

    return text.toString ();
  }

  // ---------------------------------------------------------------------------------//
  void dump ()
  // ---------------------------------------------------------------------------------//
  {
    System.out.printf ("--------------------------------------------------------%n");
    System.out.println (getText ());
  }

  // ---------------------------------------------------------------------------------//
  String getText ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder (super.toString ());

    formatText (text, "LocalFile", localFile.getName ());

    formatText (text, "Path", path.getFileName ().toString ());
    formatText (text, "AppleFile", appleFile == null ? "null" : appleFile.getFileName ());
    formatText (text, "AppleFileSystem",
        appleFileSystem == null ? "null" : appleFileSystem.getFileName ());
    formatText (text, "Is AppleFile", isAppleFile ());
    formatText (text, "Is AppleFileSystem", isAppleFileSystem ());
    formatText (text, "Is AppleDataFile", isAppleDataFile ());
    formatText (text, "Is AppleContainer", isAppleContainer ());
    //    formatText (text, "Is AppleFork", isAppleFork ());
    formatText (text, "Is AppleForkedFile", isAppleForkedFile ());
    formatText (text, "Is AppleFolder", isAppleFolder ());
    formatText (text, "Is LocalDirectory", isLocalDirectory ());
    formatText (text, "Is LocalFile", isLocalFile ());
    formatText (text, "Is CmpLocalFile", isCompressedLocalFile ());
    formatText (text, "Has subdirectories", hasSubdirectories ());

    return Utility.rtrim (text);
  }

  // ---------------------------------------------------------------------------------//
  @Override
  public String toString ()
  // ---------------------------------------------------------------------------------//
  {
    if (appleFileSystem != null)
      return name;

    if (appleFile != null)
    {
      int totalBlocks = appleFile.getTotalBlocks ();
      if (totalBlocks > 0)
        return String.format ("%s %03d %s", appleFile.getFileTypeText (), totalBlocks,
            name);
    }

    return name;
  }
}
