import java.io.*;
import java.net.*;
import java.sql.Array;
import java.util.ArrayList;

class UDPClient {
    static public double PROB;
    public static byte[] corruption(byte[] correctPacket, int packetsToCorrupt) {

        //alter pakcetsToCorrupt number of random bytes in the packet
        for (int i = 0; i < packetsToCorrupt; i++) {
            int randomIndex = (int) (Math.random() * correctPacket.length);
            correctPacket[randomIndex] = (byte) (Math.random() * 127);
        }
        return correctPacket;
    }
    public static byte[] gremlin(byte[] correctPacket) {
        //random number generator from 0 to 1
        double randomNumber = Math.random();
        System.out.println(PROB);
        if (randomNumber < PROB) {
            double randomNumber2 = Math.random();
            //50 percent change to corrupt 1
            if (randomNumber2 < 0.5) {
                return corruption(correctPacket, 1);
            }
            // 30 percent chance to corrupt 2
            if (randomNumber2 < 0.8){
                return corruption(correctPacket, 2);

            }
            // 20 percent chance to corrupt 3
            else {
                return corruption(correctPacket, 3);
            }
        } else {
            System.out.println("Not gremlined packet");
            return correctPacket;
        }
    }
    public static void main(String args[]) throws Exception
    {
        PROB = Double.parseDouble(args[0]);
        DatagramSocket clientSocket = new DatagramSocket();

//        InetAddress IPAddress = InetAddress.getByName("172.16.238.6");
        InetAddress IPAddress = InetAddress.getByName("localhost");


        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        String request = "GET test.html HTTP/1.0";
        sendData = request.getBytes();
        DatagramPacket sendPacket =
            new DatagramPacket(sendData, sendData.length, IPAddress, 9876);

        clientSocket.send(sendPacket);
        System.out.println("Request sent to server: " + request);

        DatagramPacket receivePacket =
            new DatagramPacket(receiveData, receiveData.length);

        //create a file output stream
        FileOutputStream fileOutputStream = new FileOutputStream("final/test.html");

        //get first packet
        clientSocket.receive(receivePacket);
        //create a byte array from the packet
        Packet firstPacket = new Packet(
                    gremlin(receivePacket.getData()
                )
        );

        System.out.println(firstPacket);
        Packet[] packetStorage = new Packet[firstPacket.totalPackets()];
        packetStorage[firstPacket.sequenceNumber] = firstPacket;
        if (firstPacket.validPacket) {
            byte[] ack = ("ACK "+ firstPacket.sequenceNumber).getBytes();
            DatagramPacket dataAck = new DatagramPacket(ack, 0, ack.length);
            clientSocket.send(dataAck);

        } else {
            byte[] nack = ("NACK " + firstPacket.sequenceNumber).getBytes();
            DatagramPacket dataNack = new DatagramPacket(nack, 0, nack.length);
            clientSocket.send(dataNack);
        }


        //recieve all packets from socket
        parseFile:
        while (true) {
            clientSocket.receive(receivePacket);
            //create a Packet from the received data
            Packet packet = new Packet(
                    gremlin(receivePacket.getData()
                    )
            );
            if (packet.validPacket) {
                byte[] ack = ("ACK "+ packet.sequenceNumber).getBytes();
                DatagramPacket dataAck = new DatagramPacket(ack, 0, ack.length);
                clientSocket.send(dataAck);

            } else {
                byte[] nack = ("NACK " + packet.sequenceNumber).getBytes();
                DatagramPacket dataNack = new DatagramPacket(nack, 0, nack.length);
                clientSocket.send(dataNack);
            }

            //parse header
            //separate header and file content
            byte[] fileData = packet.fileData;
            //write file
            for (byte fileByte : fileData) {
                if (fileByte == 0) break parseFile;
                //write the byte array to the file output stream as string
                fileOutputStream.write(fileByte);
            }
            //check if the packet is the final packet
            if (packet.fileData[0] == 0) {
                break;
            }
            //java thing the docs recommended me to do.
            fileOutputStream.flush();
        }

        fileOutputStream.close();

        clientSocket.close();
        System.out.println("Closing connection!");
    }
}
