import java.io.*;
import java.net.*;

class UDPClient {
    static public double PROB;
    public static byte[] parseHeader(byte[] packet) throws NumberFormatException {
        System.out.println("Parsing packet");
        String stringPacket = new String(packet);
        String[] packetArray = stringPacket.split("\\\\r\\\\n\\\\r\\\\n");
        String checksum = packetArray[0].substring(packetArray[0].length() - 6);
        String header = packetArray[0].substring(0, packetArray[0].length() - 16);

        int headerChecksum = UDPServer.checksum(header.getBytes());
        String fileData = packetArray[1];
        int dataChecksum = UDPServer.checksum(fileData.getBytes());
        int finalChecksum = headerChecksum + dataChecksum;
        try {
            int parsedChecksum = Integer.parseInt(checksum);
            if (finalChecksum != parsedChecksum) {
                System.out.println("Checksum mismatch detected!");
            } else {
                System.out.println("Valid file!");
            }
        } catch (NumberFormatException e) {
            System.out.println("Corrupted checksum detected.");
        }
        System.out.println("Writing " + fileData.length() + " bytes to file.");
        return fileData.getBytes();

    }
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

        InetAddress IPAddress = InetAddress.getByName("172.16.238.6");

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
        FileOutputStream fileOutputStream = new FileOutputStream("src/final/test.html");

        //recieve all packets from socket
        parseFile:
        while (true) {
            clientSocket.receive(receivePacket);
            //create a byte array from the packet
            byte[] packet = receivePacket.getData();
//            System.out.println("Receiving from server: " + new String(packet));

            packet = gremlin(packet);

            //parse header
            //separate header and file content
            byte[] fileData = parseHeader(packet);
            //write file
            for (byte fileByte : fileData) {
                if (fileByte == 0) break parseFile;
                //write the byte array to the file output stream as string
                fileOutputStream.write(fileByte);
            }
            //check if the packet is the final packet
//            if (packet[0] == 0) {
//                break;
//            }
            fileOutputStream.flush();

        }

        fileOutputStream.close();

        clientSocket.close();
    }
}
