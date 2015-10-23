package javatcpsniffer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Локальный прокси сервер (для прослушиваемого приложения)
 */
class LocalProxy
{
    private final InetAddress listenIp;
    private final int listenPort;
    private final InetAddress serverIp;
    private final int serverPort;
    
    public LocalProxy(InetAddress listenIp, int listenPort, InetAddress serverIp, int serverPort)
    {
        this.listenIp = listenIp;
        this.listenPort = listenPort;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }
    
    public void logTCP()
    {
        try
        {
            ServerSocket serverSocket = new ServerSocket(this.listenPort, -1, this.listenIp);
            while(true)
            {
                Socket client = serverSocket.accept();
                PacketHandler handler = new PacketHandler(client, this.serverIp, this.serverPort);
                handler.start();
            }
        }
        catch(IOException ie)
        {
            System.err.println("Не удалось создать сокет для прослушивания");
            System.err.println("IOException : " + ie.getMessage());
        }
    }
}