
package iPlayer;


import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;

public class VideoPlayer extends MIDlet implements CommandListener,
             PlayerListener {
  private Display display;
  private List itemList;
  private Form form;
  private Command stop, pause, start;
  private Hashtable items, itemsInfo;
  private Player player;

  public VideoPlayer() {
    display = Display.getDisplay(this);
    itemList = new List("Select an item to play", List.IMPLICIT);
    stop = new Command("Stop", Command.STOP, 1);
    pause = new Command("Pause", Command.ITEM, 1);
    start = new Command("Start", Command.ITEM, 1);
    form = new Form("Playing video");
    form.addCommand(stop);
    form.addCommand(pause);
    form.setCommandListener(this);
    items = new Hashtable();
    itemsInfo = new Hashtable();

    items.put("SpringWaterFall...", "file://Video/example.mpg");
    itemsInfo.put("SpringWaterFall...", "video/mpeg");

    items.put("helloboy...", "file://helloboy.mpg");
    itemsInfo.put("helloboy...", "video/mpeg");

    items.put("pilgrim...", "file://pilgrim.mpg");
    itemsInfo.put("pilgrim...", "video/mpeg");

    items.put("pirates...", "file://pirates.mpg");
    itemsInfo.put("pirates...", "video/mpeg");

    items.put("pythag1...", "file://pythag1.mpg");
    itemsInfo.put("pythag1...", "video/mpeg");

    items.put("CarelessEnglish...", "file://CarelessEnglish.mpg");
    itemsInfo.put("CarelessEnglish...", "video/mpeg");
  }

  public void startApp() {
    for(Enumeration en = items.keys(); en.hasMoreElements();) {
      itemList.append((String)en.nextElement(), null);
    }
    itemList.setCommandListener(this);
    display.setCurrent(itemList);
  }

  public void pauseApp() {
    try {
      if(player != null) player.stop();
    } catch(Exception e) {}
  }

  public void destroyApp(boolean unconditional) {
    if(player != null) player.close();
  }

  public void commandAction(Command c, Displayable d){
    if(d instanceof List) {
      List list = ((List)d);
      String key = list.getString(list.getSelectedIndex());
      try {
        playAudio((String)items.get(key), key);
      } catch (Exception e) {
        System.err.println("Unable to play: " + e);
        e.printStackTrace();
      }
    } else if(d instanceof Form){
      try {
        if(c == stop){
          player.close();
          display.setCurrent(itemList);
          form.removeCommand(start);
          form.addCommand(pause);
        } else if(c == pause){
          player.stop();
          form.removeCommand(pause);
          form.addCommand(start);
        } else if(c == start){
          player.start();
          form.removeCommand(start);
          form.addCommand(pause);
        }
      } catch(Exception e) {
        System.err.println(e);
      }
    }
  }

  private void playAudio(String locator, String key) throws Exception {
    String file = locator.substring(locator.indexOf("file://") + 6,
    locator.length());
    player = Manager.createPlayer(getClass().getResourceAsStream(file),
             (String)itemsInfo.get(key));
    player.addPlayerListener(this);
    player.setLoopCount(-1);
    player.prefetch();
    player.realize();
    player.start();
  }

  public void playerUpdate(Player player, String event, Object eventData) {
    if(event.equals(PlayerListener.STARTED) && new Long(0L).equals((Long)
       eventData)) {
      VideoControl vc = null;
      if((vc = (VideoControl)player.getControl("VideoControl")) != null) {
        Item videoDisp = (Item)vc.initDisplayMode(vc.USE_GUI_PRIMITIVE, null);
        form.append(videoDisp);
      }
      display.setCurrent(form);
    } else if(event.equals(PlayerListener.CLOSED)) {
      form.deleteAll();
    }
  }
}