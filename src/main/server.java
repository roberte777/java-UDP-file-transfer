import java.io.*;
import java.net.*;

class UDPServer {
    //function to parse message for http request
    public static String parseMessage(String message) {
        String[] messageArray = message.split(" ");
        //if first word is GET, return the second word
        //else throw error
        if (messageArray[0].equals("GET")) {
            return messageArray[1];
        } else {
            throw new IllegalArgumentException("Invalid request");
        }
    }
    //function to read in file and send it to client
    public static void sendFile(
            String fileName,
            DatagramSocket serverSocket,
            DatagramPacket receivePacket
            ) throws IOException {
        //open file from res folder
        File file = new File("res/" + fileName);
        //throw error if file doesn't exist
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        //create file input stream
        FileInputStream fileInputStream = new FileInputStream(file);
        //read the fileinput stream into a byte array in a for loop
        byte[] fileBytes = new byte[1024];
        while (fileInputStream.read(fileBytes) != -1) {
            //create a datagram packet with the byte array
            DatagramPacket sendPacket =
                new DatagramPacket(fileBytes, fileBytes.length, receivePacket.getAddress(), receivePacket.getPort());
            //send the packet
            serverSocket.send(sendPacket);
            //reset the byte array
            fileBytes = new byte[1024];
        }
        fileInputStream.close();
        //create final packet with a "0" to indicate the end of the file
        byte[] finalPacket = new byte[1];
        finalPacket[0] = 0;
        DatagramPacket finalPacketSend = new DatagramPacket(finalPacket, finalPacket.length, receivePacket.getAddress(), receivePacket.getPort());
        //send final packet
        serverSocket.send(finalPacketSend);
    }

    public static void main(String args[]) throws Exception {

        DatagramSocket serverSocket = new DatagramSocket(9876);

        byte[] receiveData = new byte[1024];

        while(true) {
            DatagramPacket receivePacket =
              new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            System.out.println("Received packet from client");
            String request  = new String(receivePacket.getData());
            System.out.println("Request: " + request);

            String fileName = parseMessage(request);
            System.out.println("File name: " + fileName);

            sendFile(fileName, serverSocket, receivePacket);

            // InetAddress IPAddress = receivePacket.getAddress();

            // int port = receivePacket.getPort();

            // String capitalizedSentence = sentence;
            // sendData = capitalizedSentence.getBytes();

            // DatagramPacket sendPacket =
            //     new DatagramPacket(sendData, sendData.length, IPAddress,port);

            // serverSocket.send(sendPacket);
        }
    }

}
