package javatcpsniffer;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main
{
    /**
     * @param args the command line arguments
     * @throws java.lang.InterruptedException
     */
    public static void main(String[] args) throws InterruptedException
    {
        try
        {
            //IP локального сервера и IP прокси
            
            LocalProxy dumper = new LocalProxy(
                InetAddress.getByAddress(new byte[]{ (byte)192, (byte)168, 1, 100} ), 8000,
                InetAddress.getByAddress(new byte[]{ (byte)192, (byte)168, 1, 100} ), 3128
            );

            dumper.logTCP();
        }
        catch (UnknownHostException ex)
        {
            System.out.println("UnknownHostException : " + ex.getMessage());
        }
    }
}