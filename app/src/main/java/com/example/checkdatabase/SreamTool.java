package com.example.checkdatabase;


import android.util.Log;

import com.example.checkdatabase.Contants.Contants;
import com.example.checkdatabase.interfaces.ISendReceive;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SreamTool implements ISendReceive {
    public static final String TAG="SreamTool";
    public int isFinished= Contants.IS_NOT_START;
    public void receive(Socket socket)  {
        //接收图片的位置
        String receivePicturePath="/sdcard/Success-Import/";
        //接收数据库的位置
        String receiveDbPath="/data/data/com.example.checkdatabase/cache/zxy456.db";

        //接收信息
        //获取开始时间
        long startTime=System.currentTimeMillis();
        //接收图片
        getPicture(receivePicturePath,socket);
        //接收数据库
//        getDB(receiveDbPath,socket);
        long endTime=System.currentTimeMillis();
        Log.d(TAG, "接收用时: "+(endTime-startTime)/1000+"s");
        Log.d(TAG, "全部文件接收完毕！！！: ");

    }

    /**
     *
     * @param buf 读取大小
     * @param writeLen 已经写入到文件中的
     * @param leftLen 内存中剩余的字节长度
     */
    public static void move( byte buf[] ,int writeLen, int leftLen){
        for (int i = 0; i < leftLen; i++) {
            buf[i] = buf[writeLen+i];
        }
    }

    public void send(Socket socket)  {//发送信息到组长

            //获取开始时间
            long startTime=System.currentTimeMillis();
            //图片 需要发送的位置
            String picturePath="/sdcard/Success-Import/";
            Log.d(TAG, "picturePath启动 ");
            sendPicture(picturePath,socket);
            Log.d(TAG, "picturePath关闭 ");
            //数据库 需要发送的位置
            String dbPath="/data/data/com.gs.syniotshesvr/databases/syniotshesvr";
            Log.d(TAG, "数据库发送启动: ");
//            sendDb(dbPath,socket);
            Log.d(TAG, "数据库发送关闭: ");
            //不再写入
            long endTime=System.currentTimeMillis();
            Log.d(TAG, "发送用时: "+(endTime-startTime)/1000+"s");


    }




    @Override
    public void sendPicture(String path,Socket socket) {
        int totalSzie=0;//用来记录已传输文件的总大小
        byte buf[]=new byte[8192];
        int len;
        //照片
        File file=new File (path);
        //将文件夹中的文件添加到 列表中
        File[] files=file.listFiles ();
        if (file.exists ()){
            try {
                //socket中收到输出流
                DataOutputStream dout= null;
                dout = new DataOutputStream(socket.getOutputStream ());
                //传入文件的个数
                dout.writeInt (files.length);
                //传入名字和大小
                for (int i=0;i<files.length;i++){
                    //在输出流中写入名字
                    dout.writeUTF (files[i].getName ());
                    dout.flush ();
                    //在输出流中写入文件大小
                    dout.writeLong (files[i].length ());
                    dout.flush ();
                    totalSzie+=files[i].length ();
                }
                dout.writeLong (totalSzie);
                Log.d (TAG, "传输文件总大小: "+totalSzie);
                for (int i=0;i<files.length;i++){
                    //缓存输入流
                    BufferedInputStream din=new BufferedInputStream (new FileInputStream (files[i]));
                    while((len=din.read (buf))!=-1){//-1表示为读取为空，din.read()返回读取的字节数,存储到一个字节数组buf中
                        dout.write (buf,0,len);//将一个字节数组中，从off开始,到len个字节写入到输出流
                    }
                }
                isFinished=isFinished=Contants.IS_TRANSPORT_SUCCESS;
                Log.d (TAG, "图片发送完成。。。。: "+isFinished);
                //关闭流

            } catch (IOException e) {
                e.printStackTrace();
                isFinished=Contants.IS_TRANSPOT_FAIL;
                Log.d(TAG, "发送图片出错: "+isFinished);
            }

        }
    }

    /**
     * 通过定义定义缓存中的字段
     * 已经读取的字节
     * 对多余部分字节 写到 下一个程序
     * 完成对多个文件的提取
     */
    @Override
    public void getPicture(String picture,Socket socket) {
        try {

            File dirs1=new File (picture);
            int  fileNum=0;
            long totalSize=0;
            if (!dirs1.exists ()){
                dirs1.mkdir();
            }
            DataInputStream din= null;
            din = new DataInputStream(new BufferedInputStream(socket.getInputStream ()));
            //获取文件的个数
            fileNum=din.readInt ();
            Log.d (TAG, "startSocket: "+fileNum);

            //创建数组将文件名和文件大小保存正在其中
            String[] mFileName=new String[fileNum];
            long mFileSize[]=new long[fileNum];
            Log.d(TAG, "发送文件名和大小: ");
            //读取对应的文件名和文件大小
            for (int i=0;i<fileNum;i++){
                mFileName[i]=din.readUTF ();
                mFileSize[i]=din.readLong ();
            }
            Log.d(TAG, "发送文件名和大小成功: ");
            totalSize=din.readLong ();
            Log.d (TAG, "totalSize: "+totalSize);
            //每次存储8字节数据
            byte[] buf =new byte[8192];
            //当前缓存中的字节长度
            int bufferedLen =0;
            //单个文件已经写入的总字节
            long writeLens=0;
            //上一次循环没用完的字节，这一次需要输入的字节
            int leftLen=0;
            //总共存入的字节
            long totalWritenLens=0;
            //这次需要存入的字节数
            int writeLen=0;
            for (int i=0;i<fileNum;i++){
                //当下一个循环开始，下一个文件起始为0
                writeLens=0;
                //创建文件
                FileOutputStream fout=new FileOutputStream (picture+mFileName[i]);
                while (true){
                    if (leftLen>0){//当剩下的字节 大于0 下次输入的字节剩下的字节
                        bufferedLen =leftLen;
                    }else {//如果没有 剩下 则 这次输入为 一个8byte读取字节
                        bufferedLen =din.read (buf);
                    }
                    if (bufferedLen ==-1) return;
                    //开始输出
                    if (writeLens+ bufferedLen >=mFileSize[i]){//当已经写入文件的字节+缓存区中要写入的字节 大于等于 第i个的大小的时候
                        //只写入thisPufSizec的部分
                        //需要剩下的字节
                        leftLen=(int) (writeLens+ bufferedLen -mFileSize[i]);
                        //这次需要写入的字节长度=这次缓存中的长度-多余的字节长度
                        writeLen=bufferedLen-leftLen;
//                        Log.d (TAG, "writeLen2: "+writeLen);
                        //开始写入到文件中
                        fout.write (buf,0,writeLen);
                        totalWritenLens+=writeLen;
                        //记录下次输入的起始位置
                        move (buf,writeLen,leftLen);
                        break;
                    }else {//如果存入缓存中的字节还是 小于 当前需要存入的文件大小时
                        // 如果是上次存留的buf 则全部写入到这个文件中
                        //如果是这次的buf 也全部写入
                        fout.write (buf,0,bufferedLen);
                        writeLens+=bufferedLen;
                        totalWritenLens+=bufferedLen;
                        if (totalWritenLens>=totalSize){
                            Log.d (TAG, "有错误: ");
                            return;
                        }
                        //如果else 执行完 代表 剩余的字节也写完了
                        leftLen=0;
                    }
                }
                fout.close();
            }
            isFinished=Contants.IS_TRANSPORT_SUCCESS;
            Log.d (TAG, "全部字节存储完毕: "+isFinished);

        } catch (IOException e) {
            e.printStackTrace();
            isFinished=Contants.IS_TRANSPOT_FAIL;
            Log.d(TAG, "图片存储失败: "+isFinished);
        }

    }

    @Override
    public void sendDb(String dbPath,Socket socket) {
        try {
            Log.d (TAG, "成功连接到端口: ");
            File file1=new File (dbPath);
            if (!file1.isFile()){
                Log.d(TAG, "不是个文件！ ");
                file1.createNewFile();
            }
            DataOutputStream dout=new DataOutputStream (socket.getOutputStream ());
            FileInputStream fin=new FileInputStream(file1);
            int len1=0;
            byte[] bytes=new byte[1024];
            Log.d(TAG, "开始传输数据库: ");
            while ((len1=fin.read (bytes))!=-1){
                dout.write (bytes,0,len1);
                dout.flush();
            }
            dout.close();
            isFinished=Contants.IS_TRANSPORT_SUCCESS;
            Log.d(TAG, "数据库发送完成: "+isFinished);



        } catch (IOException e) {
            e.printStackTrace ();
            isFinished=Contants.IS_TRANSPOT_FAIL;
            Log.d(TAG, "数据库发送失败: "+isFinished);
        }
    }

    @Override
    public void getDB(String receiveDbPath,Socket socket) {
        try {
            DataInputStream din2=new DataInputStream (socket.getInputStream());
            FileOutputStream fout=new FileOutputStream (receiveDbPath);
            int len=0;
            byte[] bytes=new byte[1024];
            while ((len=din2.read (bytes))!=-1){
                fout.write (bytes,0,len);
                Log.d(TAG, "数据库接收中.....: ");
            }
            fout.close();
            Log.d(TAG, "数据库接收完成: ");
        } catch (FileNotFoundException e) {
            isFinished=Contants.IS_TRANSPOT_FAIL;
            Log.d(TAG, "数据库接收失败: ");
            e.printStackTrace();
        } catch (IOException e) {
            isFinished=Contants.IS_TRANSPOT_FAIL;
            Log.d(TAG, "数据库接收失败: "+isFinished);
            e.printStackTrace();
        }

    }
}
