//cole spencer and ethan wilkes
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

class UDPServer {
    static final private int PORT = 9876;
    static private final int PACKETLENGTH=1024;

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
            DatagramPacket receivePacket,
            Queue<Integer> resendQueue,
            List<byte[]> sentNotVerifiedPackets
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
        //Array to keep hold of the pakcets we have sent

        //create file input stream
        FileInputStream fileInputStream = new FileInputStream(file);

            //send the file
            while(transferredBytes <= fileLength){
                // Build base header
                String responseHeader = "HTTP/1.0 200 Document Follows\\r\\n" +
                        "Content-Type: text/plain\\r\\n" +
                        "Content-Length: "+ String.format("%06d", fileLength) + "\\r\\n" +
                        "Content-Range: bytes" + String.format("%06d", transferredBytes) + "-" +
                        String.format("%06d", sendingBytes) + "/" + file.length() + "\\r\\n" +
                        "Sequence-Number: " + String.format("%06d", transferredBytes / 842 ) + "\\r\\n";

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
                //send packet to the client
                serverSocket.send(sendPacket);
                //store the sent packet in the array in case of a nack
                sentNotVerifiedPackets.add(packetBytes);
                //System.out.println("Sending this to client: " + new String(packetBytes));
                System.out.println("Sending bytes from: " + transferredBytes + " to " + sendingBytes);

                packetBytes = new byte[PACKETLENGTH];
                transferredBytes += (PACKETLENGTH - responseHeader.length());
                //this delay is to prevent problems. The buffer was being overwritten.
                TimeUnit.MILLISECONDS.sleep(10);
            }

            //resend any messed up packets
            while(true){
                Integer sequenceNumber = resendQueue.poll();
                //client will send back a -1 when it has received all pakcets successfully
                if(sequenceNumber == null){

                }
                else if (sequenceNumber == -1) {
                    break;
                } else {
                    System.out.println("Resending packet " + sequenceNumber);
                    byte[] dataToResend = sentNotVerifiedPackets.get(sequenceNumber);

                    DatagramPacket sendPacket =
                            new DatagramPacket(dataToResend, dataToResend.length, receivePacket.getAddress(), receivePacket.getPort());

                    //send packet to the client
                    serverSocket.send(sendPacket);
                    TimeUnit.MILLISECONDS.sleep(10);
                }
            }

        fileInputStream.close();

        System.out.println("Finished sending file! Should have sent: " + file.length());

    }

    public static void handleAcks(DatagramSocket serverSocket, Queue<Integer> queue, List<byte[]> sentNotVerifiedPackets) throws InterruptedException, IOException {
        //create a set of packets that have been received
        Set<Integer> receivedPackets = new HashSet<Integer>();
        //only let receive block for 50 ms
        serverSocket.setSoTimeout(50);
        while(true){
            //receive ack from client
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket =
                    new DatagramPacket(receiveData, receiveData.length);
            try{
                serverSocket.receive(receivePacket);
                String response = new String(receivePacket.getData());
                String packetStatus = response.split(" ")[0];
                int sequenceNumber = Integer.parseInt(response.split(" ")[1].replace("\0",""));
                //ending packet
                if (sequenceNumber < 0) {
                    break;
                }
                //handle nacks
                if (packetStatus.equals("NACK")) {
                    queue.add(sequenceNumber);
                }
                else {
                    //add sequence number to set
                    receivedPackets.add(sequenceNumber);
                }
             } catch (java.net.SocketTimeoutException error){
                // resend any sequence numbers not in the set
                 for(int i = 0; i < sentNotVerifiedPackets.size(); i++){
                     if(!receivedPackets.contains(i)){
                         queue.add(i);
                     }
                 }

             }

        }
    }

    public static void selectiveRepeat(
            String fileName,
            DatagramSocket serverSocket,
            DatagramPacket receivePacket,
            Queue<Integer> resendQueue,
            List<byte[]> sentNotVerifiedPackets
        ) throws IOException, InterruptedException {
        sendFile(fileName, serverSocket, receivePacket, resendQueue, sentNotVerifiedPackets);
    }

    public static void main(String args[]) throws Exception {
        Queue<Integer> resendQueue = new LinkedList<>();
        DatagramSocket serverSocket = new DatagramSocket(PORT);
        List<byte[]> sentNotVerifiedPackets = new ArrayList<byte[]>();
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

            Thread receiveThread = new Thread(()-> {
                try {
                    handleAcks(serverSocket, resendQueue, sentNotVerifiedPackets);
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            });
            receiveThread.start();

            selectiveRepeat(fileName, serverSocket, receivePacket, resendQueue, sentNotVerifiedPackets);

            receiveThread.join();

        }
    }

}
