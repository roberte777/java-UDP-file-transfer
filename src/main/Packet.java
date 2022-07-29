import java.net.DatagramPacket;

/**
 * Class to help me do shit to the packets, like extract sequence number
 */
public class Packet {
    public int sequenceNumber;
    public String header;
    public byte[] fileData;
    public int totalFileLength;
    public boolean validPacket;
    public int fileReceived;

    public Packet(byte[] packet){
        String stringPacket = new String(packet);
        String[] packetArray = stringPacket.split("\\\\r\\\\n\\\\r\\\\n");
        this.fileData = packetArray[1].getBytes();
        this.header = packetArray[0].substring(0, packetArray[0].length() - 16);
        int headerChecksum = UDPServer.checksum(this.header.getBytes());
        int dataChecksum = UDPServer.checksum(this.fileData);
        int finalChecksum = headerChecksum + dataChecksum;
        try {
            int checksum = Integer.parseInt(packetArray[0].substring(packetArray[0].length() - 6));
            this.validPacket = finalChecksum == checksum;
        } catch (Exception e) {
            this.validPacket = false;
        }
        this.sequenceNumber = Integer.parseInt(packetArray[0].split("\\\\r\\\\n")[4].split(" ")[1]);
        this.totalFileLength = Integer.parseInt(packetArray[0].split("\\\\r\\\\n")[3].split("/")[1]);
    }

    @Override
    public String toString() {
        return "Packet #" + this.sequenceNumber + " - Data Received: " + this.fileData.length + " - Damaged?: " + (this.validPacket ? "Yuh": "Nah");
    }

    public int totalPackets(){
        return this.totalFileLength / this.fileData.length;
    }

}
