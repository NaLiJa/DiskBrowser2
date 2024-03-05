package com.bytezone.diskbrowser2.gui;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import com.bytezone.appbase.FontManager;

import javafx.scene.control.TextArea;

//-----------------------------------------------------------------------------------//
class DBFontManager extends FontManager
//-----------------------------------------------------------------------------------//
{
  // ---------------------------------------------------------------------------------//
  @Override
  protected TextArea getTextArea ()
  // ---------------------------------------------------------------------------------//
  {
    StringBuilder text = new StringBuilder ();

    DataInputStream inputStream =
        new DataInputStream (DiskBrowserApp.class.getClassLoader ()
            .getResourceAsStream ("com/bytezone/diskbrowser2/gui/basic.txt"));

    try (BufferedReader in = new BufferedReader (new InputStreamReader (inputStream)))
    {
      String line;
      while ((line = in.readLine ()) != null)
        text.append (line + "\n");
    }
    catch (IOException e)
    {
      e.printStackTrace ();
    }

    if (text.length () > 0)
      text.deleteCharAt (text.length () - 1);

    return new TextArea (text.toString ());
  }
}
