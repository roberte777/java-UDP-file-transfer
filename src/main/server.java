import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

class UDPServer {
    static final private int PORT = 9876;
    private String ADDRESS;
    static private final int PACKETLENGTH=1024;
    private static final int MAX_CHECKSUM = 127 * 1024;

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
    //determine checksum from byte array
    public static int checksum(byte[] packet) {
        int sum = 0;
        for (int i = 0; i < packet.length; i++) {
            sum += packet[i];
        }
        return sum;
    }
    //function to read in file and send it to client
    public static void sendFile(
            String fileName,
            DatagramSocket serverSocket,
            DatagramPacket receivePacket
            ) throws IOException, InterruptedException {
        //open file from res folder
        File file = new File("res/" + fileName);
        //throw error if file doesn't exist
        if (!file.exists()) {
            throw new FileNotFoundException();
        }
        long fileLength = file.length();
        //bytes sent so far
        int transferredBytes = 0;
        //bytes being sent now
        int sendingBytes = transferredBytes + PACKETLENGTH;
        //final byte container for data
        byte[] packetBytes = new byte[PACKETLENGTH];
        int packetCount = 0;

        //create file input stream
        FileInputStream fileInputStream = new FileInputStream(file);

            while(transferredBytes <= fileLength){
                // Build base header
                String responseHeader = "HTTP/1.0 200 Document Follows\\r\\n" +
                        "Content-Type: text/plain\\r\\n" +
                        "Content-Length: "+ fileLength + "\\r\\n" +
                        "Content-Range: bytes" + transferredBytes + "-" + sendingBytes + "/" + file.length() + "\\r\\n";

                // Read data from file into packet
                sendingBytes = transferredBytes + (PACKETLENGTH - responseHeader.length() - 24);

                fileInputStream.read(packetBytes, responseHeader.length() + 24, PACKETLENGTH - (responseHeader.length()+24));

                //cheksum of header
                int headerChecksum = UDPServer.checksum(responseHeader.getBytes());

                //checksum of file
                int fileChecksum = UDPServer.checksum(Arrays.copyOfRange(packetBytes, responseHeader.length() + 24, PACKETLENGTH));

                //add checksums
                int checksum = headerChecksum + fileChecksum;

                //insert checksum into response header
                responseHeader = responseHeader + "Checksum: " + String.format("%06d", checksum) + "\\r\\n\\r\\n";

                byte[] responseHeaderBytes = responseHeader.getBytes();
                // Write header into a byte array
                for (int j = 0; j <responseHeader.length(); j++){
                    packetBytes[j] = responseHeaderBytes[j];
                }

                DatagramPacket sendPacket =
                        new DatagramPacket(packetBytes, packetBytes.length, receivePacket.getAddress(), receivePacket.getPort());

                serverSocket.send(sendPacket);

//                System.out.println("Sending this to client: " + new String(packetBytes));
                System.out.println("Sending bytes from: " + transferredBytes + " to " + sendingBytes);

                packetBytes = new byte[PACKETLENGTH];
                transferredBytes += (PACKETLENGTH - responseHeader.length() - 24);
                packetCount += 1;
                //this delay is to prevent problems. The buffer was being overwritten.
                TimeUnit.MILLISECONDS.sleep(100);
            }
        fileInputStream.close();
        //create final packet with a "0" to indicate the end of the file
        byte[] finalPacket = new byte[1];
        finalPacket[0] = 0;
        DatagramPacket finalPacketSend = new DatagramPacket(finalPacket, finalPacket.length, receivePacket.getAddress(), receivePacket.getPort());
        //send final packet
        serverSocket.send(finalPacketSend);
        System.out.println("Finished sending file! Should have sent: " + file.length());

    }


    public static void main(String args[]) throws Exception {

        DatagramSocket serverSocket = new DatagramSocket(PORT);

        byte[] receiveData = new byte[1024];

        while(true) {
            DatagramPacket receivePacket =
              new DatagramPacket(receiveData, receiveData.length);
            serverSocket.receive(receivePacket);
            System.out.println("Received packet from client");
            String request  = new String(receivePacket.getData());
            System.out.println("Request from client: " + request);

            String fileName = parseMessage(request);
            System.out.println("File name: " + fileName);

            sendFile(fileName, serverSocket, receivePacket);

        }
    }

}
