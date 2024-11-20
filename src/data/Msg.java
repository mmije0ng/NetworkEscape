package data;

import javax.swing.*;
import java.io.File;
import java.io.Serializable;

public class Msg implements Serializable{
    public final static int MODE_LOGIN =0x1;
    public final static int MODE_LOGOUT =0x2;
    public final static int MODE_TX_STRING =0x3;
    public final static int MODE_TX_FILE =0x4;
    public final static int MODE_TX_IMAGE =0x5;
    public final static int MODE_CREATE_LOBBY =0x6;
    public final static int MODE_SEARCH_LOBBY =0x7;
    public final static int MODE_ENTER_LOBBY =0x8;
    public final static int MODE_EXIT_LOBBY =0x9;

    public String userName;
    public int mode;
    String message;
    ImageIcon image;
    long size;
    File file;
    public String lobbyName;
    public String lobbyPassword;
    public int playMode;

    public Msg(String userName, int code, String message, ImageIcon image, long size, File file, String lobbyName, String lobbyPassword, int playMode) {
        this.userName = userName;
        this.mode = code;
        this.message = message;
        this.image = image;
        this.size = size;
        this.file = file;
        this.lobbyName=lobbyName;
        this.lobbyPassword=lobbyPassword;
        this.playMode=playMode;
    }

    public Msg(String userName, int code, String message, ImageIcon image) {
        this(userName,code,message,image,0,null,null,null,0);
    }
    public Msg(String userName, int code) {
        this(userName, code, null, null,0, null,null,null,0);
    }

    public Msg(String userName, int code, String message) {
        this(userName, code, message, null,0,null,null,null,0);
    }

    public Msg(String userName, int code, ImageIcon image) {
        this(userName, code, null, image,0,null,null,null,0);
    }

    public Msg(String userName, int code, String filename, long size, File file) {
        this(userName, code, filename, null, size, file,null,null,0);
    }
    public Msg(String userName, int code, String lobbyName, String lobbyPassword, int playMode){
        this(userName,code,null,null,0,null,lobbyName,lobbyPassword,playMode);
    }

    public Msg(String userName, int code, String lobbyName, String lobbyPassword){
        this(userName,code,null,null,0,null,lobbyName,lobbyPassword,0);
    }

}
