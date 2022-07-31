//cole spencer and ethan wilkes
import java.io.*;
import java.net.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

class UDPClient {
    static public double PROB;
    static public double LOSS_PROB;
    public static void main(String args[]) throws Exception
    {
        PROB = Double.parseDouble(args[0]);
        LOSS_PROB = Double.parseDouble(args[1]);
        final int PORT = 9876;
        DatagramSocket clientSocket = new DatagramSocket();

       InetAddress IPAddress = InetAddress.getByName("172.16.238.6");
       //test on localhost before running with docker on above IP for final
       //output
//         InetAddress IPAddress = InetAddress.getByName("localhost");


        byte[] sendData = new byte[1024];
        byte[] receiveData = new byte[1024];
        int receivedCorrectly = 0;
        String request = "GET test.html HTTP/1.0";
        sendData = request.getBytes();
        DatagramPacket sendPacket =
            new DatagramPacket(sendData, sendData.length, IPAddress, PORT);

        clientSocket.send(sendPacket);
        System.out.println("Request sent to server: " + request);

        DatagramPacket receivePacket =
            new DatagramPacket(receiveData, receiveData.length);

        //get first packet
        clientSocket.receive(receivePacket);
        //create a byte array from the packet

        Packet firstPacket = new Packet(
                receivePacket.getData()
        );
        firstPacket.gremlin(PROB, LOSS_PROB);

        System.out.println(firstPacket);
        Packet[] packetStorage = new Packet[firstPacket.totalPackets()];
        if (firstPacket.packetLost) {
        }
        else if (firstPacket.validPacket) {
            byte[] ack = ("ACK "+ firstPacket.sequenceNumber).getBytes();
            DatagramPacket dataAck = new DatagramPacket(ack, ack.length, IPAddress, PORT);
            clientSocket.send(dataAck);
            packetStorage[firstPacket.sequenceNumber] = firstPacket;
            receivedCorrectly++;


        } else {
            byte[] nack = ("NACK " + firstPacket.sequenceNumber).getBytes();
            DatagramPacket dataNack = new DatagramPacket(nack,nack.length, IPAddress, PORT);
            clientSocket.send(dataNack);
        }
        System.out.println("First Packet Sequence Number: "+ firstPacket.sequenceNumber);

        //recieve all packets from socket
        while (true) {
            clientSocket.receive(receivePacket);
            //create a Packet from the received data
            Packet packet = new Packet(
                    receivePacket.getData()
            );
            packet.gremlin(PROB, LOSS_PROB);
            System.out.println("Packet Sequence Number: " + packet.sequenceNumber);
            if (packet.packetLost || packetStorage[packet.sequenceNumber] != null) {

            }
            else if (packet.validPacket) {
                byte[] ack = ("ACK "+ packet.sequenceNumber).getBytes();
                DatagramPacket dataAck = new DatagramPacket(ack, ack.length, IPAddress, PORT);
                clientSocket.send(dataAck);
                packetStorage[packet.sequenceNumber] = packet;
                receivedCorrectly++;
                System.out.println("Sending ACK for packet " + packet.sequenceNumber);


            } else {
                byte[] nack = ("NACK " + packet.sequenceNumber).getBytes();
                DatagramPacket dataNack = new DatagramPacket(nack,nack.length, IPAddress, PORT);
                clientSocket.send(dataNack);
                System.out.println("Sending NACK for " + packet.sequenceNumber);

            }

            if (receivedCorrectly == packet.totalPackets() ) {
                byte[] finalPacketArray = ("ACK -1").getBytes();
                DatagramPacket finalAck = new DatagramPacket(finalPacketArray, finalPacketArray.length, IPAddress, PORT);
                clientSocket.send(finalAck);
                clientSocket.close();
                break;
            }

        }
        //create a file output stream
        FileOutputStream fileOutputStream = new FileOutputStream("final/test.html");
        parseFile:
        for (Packet packet : packetStorage){
            for (byte fileByte : packet.fileData){

                if (fileByte == 0) break parseFile;

                fileOutputStream.write(fileByte);
            }

        }

        fileOutputStream.close();
        System.out.println("Closing connection!");
    }
}
