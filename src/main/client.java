import java.io.*;
import java.net.*;

class UDPClient {
    public static void main(String args[]) throws Exception
    {
        DatagramSocket clientSocket = new DatagramSocket();

        InetAddress IPAddress = InetAddress.getByName("172.16.238.6");
        System.out.println(IPAddress);

        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        String request = "GET test.txt HTTP/1.0";
        sendData = request.getBytes();
        DatagramPacket sendPacket =
            new DatagramPacket(sendData, sendData.length, IPAddress, 9876);

        clientSocket.send(sendPacket);
        System.out.println("Request sent to server");

        DatagramPacket receivePacket =
            new DatagramPacket(receiveData, receiveData.length);

        //create a file output stream
        FileOutputStream fileOutputStream = new FileOutputStream("final/test.txt");
        //recieve all packets from socket
        while (true) {
            clientSocket.receive(receivePacket);
            //create a byte array from the packet
            byte[] fileBytes = receivePacket.getData();
            //write the byte array to the file output stream as string
            fileOutputStream.write(fileBytes);
            //check if the packet is the final packet
            if (fileBytes[0] == 0) {
                break;
            }
            fileOutputStream.flush();
        }

        fileOutputStream.close();

        clientSocket.close();
    }
}
