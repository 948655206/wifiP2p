package com.example.checkdatabase.interfaces;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

public interface ISendReceive {

    void sendPicture(String path, Socket socket) ;

    void getPicture(String path, Socket socket);
    void sendDb(String path,Socket socket);
    void getDB(String path,Socket socket);
}
