package iPlayer;

import javax.microedition.midlet.*;
import org.netbeans.microedition.lcdui.pda.FileBrowser;
import javax.microedition.lcdui.*;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import java.io.*;
import java.util.*;
import org.netbeans.microedition.lcdui.SplashScreen;

class iPlay extends MIDlet implements CommandListener, ItemCommandListener, Runnable{ //ItemStateListener

    public Form form, iPlayerSkin, videoPlayerSkin;
    public Player player;
    public Command command, bakFrmVidList, plyFrmVidList, unmuteCommand, shuffleOffCommand, previousCommand, nextCommand, upCommand, downCommand, playCommand, exitCommand, playlistCommand, browserCommand, backFrmPlaylist, playFromPlaylist, addToPlaylist, backFromBrowser, seekerCommand, muteCommand, shuffleCommand, volCommand, videoPlaylistCommand;
    public List playList, videoPlayList;
    public Vector playlistVector, videoListVector;
    public Image image, welcomeLogo, next, previous, up, down, play;
    public ImageItem imageItem, prevImgItem, nextImgItem, upImgItem, downImgItem, playImgItem;
    public Ticker iPlayBanner;    
    public Display display;
    public SplashScreen welcomeScreen;
    public InputStream is, stream;
    public Thread dThread;
    public Object dThreadLock = new Object();
    public Object pauseLock = new Object();    
    public VolumeControl vc;
    public StringItem timer;    
    public Random random = new Random();    
    public FileBrowser browser;
    public StringItem mute, unmute, shuffle, shuffleOff;
    public VideoControl vidctrl = null;
    public Spacer space;
    public Item videoDisp;

    public boolean paused, interrupted;
    public long dur;
    public boolean muteFlag = false, shuffleFlag = false;
    public int i, prev;
    public String string, url, nthUrl, title = "Choose a song from the playlist", nthTitle, RECORD_STORE_NAME = "adrms", mtime, ret, shuffleString = "Shuffle", muteString = "Mute";   

    public iPlay() {
    }

    public void initialize() {
    }

    public Display getDisplay() {
        display = Display.getDisplay(this);
        return display;
    }

    public void switchDisplay(Displayable toShow) {
        display = getDisplay();
        display.setCurrent(toShow);
    }

    public void startApp() {
        switchDisplay(sendSplashScreen());
    }

    public void pauseApp() {
    }

    public void destroyApp(boolean flag) {
    }

    public void exitMIDlet() {

        destroyApp(true);
        notifyDestroyed();
    }

    public void setParam(String url) {
        this.url = url;

        int idx = url.lastIndexOf('/');
        title = url.substring(idx + 1);
    }

    public void playSound() {
        if ((title == null) || (url == null)) {
            return;
        }

        // player was paused
        if (player != null) {
            // wake up paused thread
            synchronized (pauseLock) {
                paused = false;
                pauseLock.notify();
            }

            try {
                player.start();
            } catch (MediaException me) {
                me.printStackTrace();
            }
            return;
        }

        // start new player
        synchronized (dThreadLock) {
            stopSound();
            interrupted = false;
            paused = false;
            mtime = "";
            dThread = new Thread(this);
            dThread.start();
        }
    }

    public void stopSound() {
        synchronized (dThreadLock) {
            try {
                interrupted = true;

                // wake up thread if it is paused
                synchronized (pauseLock) {
                    pauseLock.notify();
                }

                if (dThread != null) {
                    dThreadLock.wait();
                }
            } catch (InterruptedException ie) {
                // nothing
            }
        }
    }

    void pauseSound() {
        try {
            if (player != null) {
                // pause player
                player.stop();
                paused = true;
            }
        } catch (MediaException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isPlaying() {
        return (player != null) && (player.getState() >= Player.STARTED);
    }

    public String timeFM(long val) {
        ret = "";
        int mval = (int) (val / 1000);
        int sec = mval / 1000;
        int min = sec / 60;

        if (min >= 10) {
            ret = ret + min + ":";
        } else if (min > 0) {
            ret = "0" + min + ":";
        } else {
            ret = "00:";
        }

        if (sec >= 10) {
            ret = ret + sec + ".";
        } else if (sec > 0) {
            ret = ret + "0" + sec + ".";
        } else {
            ret = ret + "00.";
        }

        mval = (mval % 1000) / 100;
        ret = ret + mval;

        return (ret);
    }

    public void run() {

        createPlayer();
       // player.addPlayerListener(this);

        if (player == null) {
            // can't create player
            synchronized (dThreadLock) {
                dThread = null;
                dThreadLock.notify();
                return;
            }
        }

        try {
            player.realize();
            dur = player.getDuration();
            if (dur != -1) {
                title = title + " [" + timeFM(dur) + "]";
            }
            player.start();
        } catch (Exception ex) {
        }

        // mtime update loop
        while (!interrupted) {
            try {
                mtime = timeFM(player.getMediaTime());
                iPlayerSkin.setTicker(new Ticker(title + " " + mtime));

                Thread.sleep(100);
            } catch (Exception ex) {
            }
            // pause the loop if player paused
            synchronized (pauseLock) {
                if (paused) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException ie) {
                        // nothing
                    }
                }
            }
        }
        // terminating player and the thread
        player.close();
        player = null;

        synchronized (dThreadLock) {
            dThread = null;
            dThreadLock.notify();
        }
    }

    public void createPlayer() {
        try {
            if (url.startsWith("resource")) {
                int idx = url.indexOf(':');
                String loc = url.substring(idx + 1);
                InputStream is = getClass().getResourceAsStream(loc);
                String ctype = guessContentType(url);
                player = Manager.createPlayer(is, ctype);
            } else if (url.startsWith("video")) {
                int idx = url.indexOf(':');
                String loc = url.substring(idx + 1);
                InputStream is = getClass().getResourceAsStream(loc);
                String ctype = guessContentType(url);
                player = Manager.createPlayer(is, ctype);
            }
            player.setLoopCount(1);
            //player.addPlayerListener(this);
        } catch (Exception ex) {
            if (player != null) {
                player.close();
                player = null;
            }
            Alert alert = new Alert("Warning", "Cannot create player", null, null);
            alert.setTimeout(1000);
        }
    }

    public static String guessContentType(String url)
            throws Exception {
        String ctype;

        if (url.endsWith("wav")) {
            ctype = "audio/x-wav";
        } else if (url.endsWith("jts")) {
            ctype = "audio/x-tone-seq";
        } else if (url.endsWith("mid")) {
            ctype = "audio/midi";
        } else if (url.endsWith("mp3")) {
            ctype = "audio/mpeg";
        } else if (url.endsWith("mpg")) {
            ctype = "video/mpeg";
        } else {
            throw new Exception("Cannot guess content type from URL: " + url);
        }
        return ctype;
    }

    public SplashScreen sendSplashScreen() {
        try {
            welcomeLogo = Image.createImage("/graphics/welcomeLogo.PNG");
        } catch (Exception splashExc) {
            System.out.println("Logo not found!!");
        }
        welcomeScreen = new SplashScreen(getDisplay());
        welcomeScreen.setTitle("iPlay");
        welcomeScreen.setCommandListener(this);
        welcomeScreen.setText("iPlay some of the stuffs :'(");
        welcomeScreen.setImage(welcomeLogo);
        return welcomeScreen;
    }

    public Form sendiPlayer() {
        iPlayerSkin = new Form("iPlayer");
        //creating images
        try {
            previous = Image.createImage("/graphics/previous.PNG");
        } catch (Exception prevImgkExcp) {
            System.out.println("Cannot display previous.PNG");
        }
        try {
            next = Image.createImage("/graphics/next.PNG");
        } catch (Exception nxtImgExcp) {
            System.out.println("Cannot display next.PNG");
        }
        try {
            down = Image.createImage("/graphics/down.PNG");
        } catch (Exception downImgExcp) {
            System.out.println("Cannot display down.PNG");
        }
        try {
            up = Image.createImage("/graphics/up.PNG");
        } catch (Exception upImgExcp) {
            System.out.println("Cannot display up.PNG");
        }
        try {
            play = Image.createImage("/graphics/play.PNG");
        } catch (Exception midImgExcp) {
            System.out.println("Cannot display play.PNG");
        }

        //creating ItemCommands and other commands
        previousCommand = new Command("", Command.ITEM, 10);
        nextCommand = new Command("", Command.ITEM, 10);
        downCommand = new Command("", Command.ITEM, 10);
        upCommand = new Command("", Command.ITEM, 10);
        playCommand = new Command("Play/Pause", Command.ITEM, 1);
        muteCommand = new Command("", Command.ITEM, 10);
        shuffleCommand = new Command("", Command.ITEM, 10);
        exitCommand = new Command("Exit", Command.OK, 5);
        playlistCommand = new Command("Playlist", Command.OK, 5);
        browserCommand = new Command("Add Media", Command.OK, 5);
        seekerCommand = new Command("", Command.ITEM, 10);
        volCommand = new Command("", Command.ITEM, 10);
        unmuteCommand = new Command("", Command.ITEM, 10);
        shuffleOffCommand = new Command("", Command.ITEM, 10);
        videoPlaylistCommand = new Command("Video List", Command.OK, 5);
        //Creating ImageItems
        upImgItem = new ImageItem(null, up, Item.LAYOUT_CENTER, null, Item.PLAIN);
        upImgItem.setDefaultCommand(upCommand);
        upImgItem.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER | Item.LAYOUT_NEWLINE_AFTER);
        upImgItem.setItemCommandListener(this);

        prevImgItem = new ImageItem(null, previous, Item.LAYOUT_CENTER, null, Item.PLAIN);
        prevImgItem.setDefaultCommand(previousCommand);
        prevImgItem.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER);
        prevImgItem.setItemCommandListener(this);

        playImgItem = new ImageItem(null, play, Item.LAYOUT_CENTER, null, Item.PLAIN);
        playImgItem.setDefaultCommand(playCommand);
        playImgItem.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER);
        playImgItem.setItemCommandListener(this);

        nextImgItem = new ImageItem(null, next, Item.LAYOUT_CENTER, null, Item.PLAIN);
        nextImgItem.setDefaultCommand(nextCommand);
        nextImgItem.setLayout(Item.LAYOUT_2 | Item.LAYOUT_NEWLINE_AFTER);
        nextImgItem.setItemCommandListener(this);

        downImgItem = new ImageItem(null, down, Item.LAYOUT_CENTER, null, Item.PLAIN);
        downImgItem.setDefaultCommand(downCommand);
        downImgItem.setLayout(Item.LAYOUT_2 | Item.LAYOUT_NEWLINE_AFTER | Item.LAYOUT_CENTER);
        downImgItem.setItemCommandListener(this);

        mute = new StringItem("Mute", null, Item.PLAIN);
        unmute = new StringItem("Unmute", null, Item.PLAIN);
        shuffle = new StringItem("Shuffle", null, Item.PLAIN);
        shuffleOff = new StringItem("Shuffle-Off", null, Item.PLAIN);

        space = new Spacer(1, 1);
        iPlayerSkin.append(space);

        iPlayerSkin.append(upImgItem);
        iPlayerSkin.append(prevImgItem);
        iPlayerSkin.append(playImgItem);
        iPlayerSkin.append(nextImgItem);
        iPlayerSkin.append(downImgItem);

        if (shuffleFlag == false) {
            iPlayerSkin.append(shuffle);
            shuffle.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER);
            shuffle.setDefaultCommand(shuffleCommand);
            shuffle.setItemCommandListener(this);
        } else if (shuffleFlag == true) {
            iPlayerSkin.append(shuffleOff);
            shuffleOff.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER);
            shuffleOff.setDefaultCommand(shuffleOffCommand);
            shuffleOff.setItemCommandListener(this);
        }
        if (muteFlag == false) {
            iPlayerSkin.append(mute);
            mute.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER);
            mute.setDefaultCommand(muteCommand);
            mute.setItemCommandListener(this);
        } else if (muteFlag == true) {
            iPlayerSkin.append(unmute);
            unmute.setLayout(Item.LAYOUT_2 | Item.LAYOUT_CENTER);
            unmute.setDefaultCommand(unmuteCommand);
            unmute.setItemCommandListener(this);
        }

        iPlayerSkin.addCommand(browserCommand);
        iPlayerSkin.addCommand(playlistCommand);
        iPlayerSkin.addCommand(videoPlaylistCommand);
        iPlayerSkin.addCommand(exitCommand);
        iPlayerSkin.setCommandListener(this);

        return iPlayerSkin;
    }

//    public Form videoPlayer() {
//        videoPlayerSkin = new Form("iVideo");
//        return videoPlayerSkin;
//    }

    public List playlist() {
        playlistVector = new Vector();
        playList = new List("iPlaylist", Choice.IMPLICIT);
        backFrmPlaylist = new Command("Back", Command.BACK, 1);
        playFromPlaylist = new Command("Play", Command.OK, 1);
        for (int n = 1; n < 32; n++) {
            nthUrl = "Url-" + n;
            url = getAppProperty(nthUrl);
            if ((url == null) || (url.length() == 0)) {
                break;
            }
            nthTitle = "Title-" + n;
            title = getAppProperty(nthTitle);

            if ((title == null) || (title.length() == 0)) {
                title = url;
            }
            playlistVector.addElement(url);
            playList.append(title, null);
        }
        playList.addCommand(backFrmPlaylist);
        playList.addCommand(playFromPlaylist);
        playList.setCommandListener(this);
        return playList;
    }

    public List videoList() {
        videoListVector = new Vector();
        videoPlayList = new List("Video List", Choice.IMPLICIT);
        bakFrmVidList = new Command("Back", Command.BACK, 1);
        plyFrmVidList = new Command("Play", Command.OK, 1);
        for (int n = 1; n < 32; n++) {
            nthUrl = "VidUrl-" + n;
            url = getAppProperty(nthUrl);
            if ((url == null) || (url.length() == 0)) {
                break;
            }
            nthTitle = "VidTitle-" + n;
            title = getAppProperty(nthTitle);

            if ((title == null) || (title.length() == 0)) {
                title = url;
            }
            videoListVector.addElement(url);
            videoPlayList.append(title, null);
        }
        videoPlayList.addCommand(bakFrmVidList);
        videoPlayList.addCommand(plyFrmVidList);
        videoPlayList.setCommandListener(this);
        return videoPlayList;
    }

    public FileBrowser returnBrowser() {
        browser = new FileBrowser(getDisplay());
        browser.setTitle("Add to playlist");
        backFromBrowser = new Command("Back", Command.BACK, 1);
        browser.addCommand(backFromBrowser);
        browser.setCommandListener(this);
        return browser;
    }

//    public void playerUpdate(Player player, String event, Object eventData) {
//        if (event.equals(PlayerListener.STARTED) && new Long(0L).equals((Long) eventData)) {
//            if ((vidctrl = (VideoControl) player.getControl("VideoControl")) != null) {
//                videoDisp = (Item) vidctrl.initDisplayMode(vidctrl.USE_GUI_PRIMITIVE, null);
//                videoPlayerSkin.append(videoDisp);
//            }
//            switchDisplay(videoPlayer());
//        } else if (event.equals(PlayerListener.CLOSED)) {
//            videoPlayerSkin.deleteAll();
//            switchDisplay(sendiPlayer());
//        }
//    }

    public void changeVolume(int diff) {
        vc = (VolumeControl) player.getControl("VolumeControl");

        if (vc != null) {
            int cv = vc.getLevel();
            cv += diff;
            cv = vc.setLevel(cv);
        }
    }

    public void yesMute() {
        vc = (VolumeControl) player.getControl("VolumeControl");
        vc.setMute(true);
        muteFlag = true;
    }

    public void noMute() {
        vc = (VolumeControl) player.getControl("VolumeControl");
        vc.setMute(false);
        muteFlag = false;
    }

    public void commandAction(Command command, Item item) {
        if (command == upCommand) {
            changeVolume(10);
        } else if (command == downCommand) {
            changeVolume(-10);
        } else if (command == playCommand) {
            if (paused == false) {
                pauseSound();
                paused = true;
            } else if (paused == true) {
                try {
                    playSound();
                    paused = false;
                } catch (Exception e) {
                }
            } else if (isPlaying() == false && paused == false) {
                if (shuffleFlag) {
                    i = random.nextInt(9);
                } else {
                    i = i + 1;
                }
                display.setCurrent(playlist());
                setParam((String) playlistVector.elementAt(i));
                playSound();
                display.setCurrent(sendiPlayer());
            }
        } else if (command == nextCommand) {
            if (isPlaying() || paused == true) {
                stopSound();
                prev = i;
                if (shuffleFlag) {
                    i = random.nextInt(9);
                } else {
                    i = i + 1;
                }
                display.setCurrent(playlist());
                setParam((String) playlistVector.elementAt(i));
                playSound();
                display.setCurrent(sendiPlayer());
            }
        } else if (command == previousCommand) {
            if (isPlaying() || paused == true) {
                stopSound();
                display.setCurrent(playlist());
                setParam((String) playlistVector.elementAt(prev));
                playSound();
                display.setCurrent(sendiPlayer());
            }
        } else if (command == muteCommand || command == unmuteCommand) {
            if (muteFlag == false) {
                yesMute();
                switchDisplay(sendiPlayer());
            } else {
                noMute();
                switchDisplay(sendiPlayer());
            }
        } else if (command == shuffleCommand || command == shuffleOffCommand) {
            if (shuffleFlag == false) {
                shuffleFlag = true;
                switchDisplay(sendiPlayer());
            } else {
                shuffleFlag = false;
                switchDisplay(sendiPlayer());
            }
        }
    }

    public void commandAction(Command command, Displayable displayable) {

        if (command == welcomeScreen.DISMISS_COMMAND) {
            switchDisplay(sendiPlayer());
        } else if (command == exitCommand) {
            exitMIDlet();
        } else if (command == playlistCommand) {
            switchDisplay(playlist());
        } else if (command == backFrmPlaylist) {
            switchDisplay(sendiPlayer());
        } else if (((displayable == playList) && (command == List.SELECT_COMMAND)) || (command == playFromPlaylist)) {
            stopSound();
            i = playList.getSelectedIndex();
            prev = i;
            setParam((String) playlistVector.elementAt(i));
            playSound();
            switchDisplay(sendiPlayer());
        } else if ((displayable == browser && command == browser.SELECT_FILE_COMMAND)) {
            switchDisplay(playlist());
        } else if (command == browserCommand) {
            switchDisplay(returnBrowser());
        } else if (command == backFromBrowser) {
            switchDisplay(sendiPlayer());
        } else if (command == videoPlaylistCommand) {
            switchDisplay(videoList());
        } else if (command == bakFrmVidList) {
            switchDisplay(sendiPlayer());
//        } else if (((displayable == videoPlayList) && (command == List.SELECT_COMMAND)) || (command == plyFrmVidList)) {
//            stopSound();
//            i = videoPlayList.getSelectedIndex();
//            prev = i;
//            setParam((String) videoListVector.elementAt(i));
//            playSound();
//        }
    }
}
}
