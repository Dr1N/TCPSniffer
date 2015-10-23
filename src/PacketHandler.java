/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package javatcpsniffer;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Calendar;
import java.util.Formatter;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Обработчик пакетов
 */
class PacketHandler extends Thread
{
    //<editor-fold defaultstate="collapsed" desc="Connection handlers threads">
    class clientToServer extends Thread
    {
        @Override
        public void run()
        {
            int cnt;
            byte[] buffer = new byte[524288];
            while(true)
            {
                try
                {
                    cnt = PacketHandler.this.client.getInputStream().read(buffer, 0, buffer.length);
                    if (cnt == -1) throw new IOException("Соединение разорвано Клиентом. Получено -1 байт");
                    PacketHandler.this.server.getOutputStream().write(buffer, 0, cnt);
                    PacketHandler.this.logData(buffer, cnt, true);
                }
                catch(IOException ioe)
                {
                    //System.err.print("Не удалось совершить обмен данными (C-S) - ");
                    //System.err.println("IOException: " + ioe.getMessage());
                    break;
                }
            }
        }
    }
    
    class serverToClient extends Thread
    {
        @Override
        public void run()
        {
            int cnt;
            byte[] buffer = new byte[524288];
            while(true)
            {
                try
                {
                    cnt = PacketHandler.this.server.getInputStream().read(buffer, 0, buffer.length);
                    if (cnt == -1) throw new IOException("Соединение разорвано Сервером. Получено -1 байт");
                    PacketHandler.this.client.getOutputStream().write(buffer, 0, cnt);
                    PacketHandler.this.logData(buffer, cnt, false);
                }
                catch(IOException ioe)
                {
                    //System.err.print("Не удалось совершить обмен данными (S-C) - ");
                    //System.err.println("IOException: " + ioe.getMessage());
                    break;
                }
            }
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="TcpLogger thread">
    
    //Вывод дампа
    
    class TcpLogger extends Thread
    {
        private final byte[] data;
        public int cnt; 
        private final boolean isClient;
        
        public TcpLogger(byte[] data, int cnt, boolean isClient)
        {
            this.data = data;
            this.cnt = cnt;
            this.isClient = isClient;
        }
               
        @Override
        public void run()
        {
            
            String dir = isClient ? "Клиент -> Сервер" : "Сервер -> Клиент";
            String len = isClient ? "Получено от клиента: " + cnt : "Получено от сервера: " + cnt;
            String time =  this.getTime(); 
                                    
            StringBuilder sb = new StringBuilder();
            Formatter frm = new Formatter(sb);
                        
            byte[] buf = new byte[16];
            int bufIndex = 0;
            String strBuf = null;
            String offset = null;
            
            //Сформировать массив кратный 16ти
            
            int nSize = this.cnt;
            while(nSize % 16 != 0) nSize++;
            
            byte[] fullDada = new byte[nSize];
            
            for (int i = 0; i < nSize; i++)
            {
                fullDada[i] = (i < this.cnt) ? data[i] : 0;
            }
 
            try
            {
                PacketHandler.lock.lock();
                frm.format("\n--------------\n%s%s%sb\n--------------\n", time, dir, len);
                for (int i = 0; i < nSize; i++)
                {
                    //Строка (16 байт)
                    
                    buf[bufIndex] = fullDada[i];
                    bufIndex++;
                    if(bufIndex == 16) 
                    { 
                        bufIndex = 0;
                        try
                        {
                            strBuf = new String(buf, "CP1251");
                        } catch (UnsupportedEncodingException ex)
                        {
                            
                        }
                        strBuf = strBuf.replaceAll("\n", " ")
                                       .replaceAll("\t", " ")
                                       .replaceAll("\r", " ");
                    }
                    
                    //Формирование строки (весь пакет)
                    
                    if(i == 0)
                    {
                        frm.format("000000 |\t");
                    }
                    
                    if(i % 16 == 0 && i != 0)
                    {
                        offset = this.getOffset(i);
                        frm.format("\t%s\n%s\t", strBuf, offset);
                        frm.format("%02X ", fullDada[i]);
                    }
                    else
                    {
                        frm.format("%02X ", fullDada[i]);
                    }
                }
                frm.format("\t%s", strBuf);
                
                //Консоль
                
                System.out.print(sb.toString());
           
                //Запись в файд
                
                try (FileWriter fw = new FileWriter("log.txt", true))
                {
                    fw.write(sb.toString());
                }
                catch(IOException ioe)
                {
                    System.err.println("Write file. IOException : " + ioe.getMessage());
                }
            }
            finally
            {
                PacketHandler.lock.unlock();
                frm.close();
            }
        }
        
        private String getTime()
        {
            Calendar c = Calendar.getInstance();
            StringWriter sw = new StringWriter();
            sw.write(c.get(Calendar.YEAR) + ".");
            sw.write(c.get(Calendar.MONTH) + ".");
            sw.write(c.get(Calendar.DAY_OF_MONTH) + " ");
            sw.write(c.get(Calendar.HOUR) + ":");
            sw.write(c.get(Calendar.MINUTE) + ":");
            sw.write(c.get(Calendar.SECOND)+ " ");
             
            return sw.toString();
        }
        
        private String getOffset(int index)
        {
            StringBuilder sb = new StringBuilder(String.valueOf(index));
            while(sb.length() != 6) sb.insert(0, "0");
            return sb.toString() + " |";
        }
    }
    //</editor-fold>
    
    public static Lock lock = new ReentrantLock();
    private Socket client;
    private Socket server;
    private boolean isValid;
    
    public PacketHandler(){}
    
    public PacketHandler(Socket client, InetAddress serverIp, int serverPort)
    {
        this.setDaemon(true);
        this.client = client;
        try
        {
            this.server = new Socket(serverIp, serverPort);
            this.isValid = true;
        } 
        catch (IOException ex)
        {
            this.isValid = false;
            System.err.print("Не удалось установить соединение с сервером - ");
            System.err.println("IOException : " + ex.getMessage());
        }
    }
    
    public TcpLogger getLogger(byte[] arr, int cnt, boolean isClient)
    {
        return new TcpLogger(arr, cnt, isClient);
    }
    
    @Override
    public void run()
    {
        //Если соединение с сервером не установлено - закрываемся
        
        if(this.isValid == false) 
        {
            System.err.println("СИСТЕМНЫЙ СБОЙ");
            System.exit(1);
        }
           
        //Запуск потоков
        
        Thread clientToServer = new PacketHandler.clientToServer();
        Thread serverToClient = new PacketHandler.serverToClient();
        
        clientToServer.start();
        serverToClient.start();
    }
         
    /**
     * 
     * @param data - данные
     * @param direction - направление, true - клиент-сервер
     */
    private void logData(byte[] data, int cnt, boolean isClient)
    {
        TcpLogger loger = new TcpLogger(data, cnt, isClient);
        loger.start();
    }
}