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
    private String[] packetArray;

    public Packet(byte[] packet){
        String stringPacket = new String(packet);
        this.packetArray = stringPacket.split("\\\\r\\\\n\\\\r\\\\n");
        this.fileData = packetArray[1].getBytes();
        this.header = packetArray[0].substring(0, packetArray[0].length() - 16);
        this.checksum();
        this.sequenceNumber = Integer.parseInt(packetArray[0].split("\\\\r\\\\n")[4].split(" ")[1]);
        this.totalFileLength = Integer.parseInt(packetArray[0].split("\\\\r\\\\n")[3].split("/")[1]);
    }

    @Override
    public String toString() {
        return "Packet #" + this.sequenceNumber + " - Data Received: " + this.fileData.length + " - Damaged?: " + (this.validPacket ? "Nah": "Yuh");
    }

    public int totalPackets(){
        return (int) Math.ceil((double) this.totalFileLength / this.fileData.length);
    }
    private void checksum() {
        int headerChecksum = UDPServer.checksum(this.header.getBytes());
        int dataChecksum = UDPServer.checksum(this.fileData);
        int finalChecksum = headerChecksum + dataChecksum;
        try {
            int checksum = Integer.parseInt(this.packetArray[0].substring(this.packetArray[0].length() - 6));
            this.validPacket = finalChecksum == checksum;
        } catch (Exception e) {
            this.validPacket = false;
        }

    }
    public byte[] corruption(byte[] correctPacket, int packetsToCorrupt) {

        //alter pakcetsToCorrupt number of random bytes in the packet
        for (int i = 0; i < packetsToCorrupt; i++) {
            int randomIndex = (int) (Math.random() * correctPacket.length);
            correctPacket[randomIndex] = (byte) (Math.random() * 127);
        }
        return correctPacket;
    }
    public void gremlin(double PROB) {
        //random number generator from 0 to 1
        double randomNumber = Math.random();
        if (randomNumber < PROB) {
            double randomNumber2 = Math.random();
            //50 percent change to corrupt 1
            if (randomNumber2 < 0.5) {
                this.fileData = corruption(this.fileData, 1);
            }
            // 30 percent chance to corrupt 2
            if (randomNumber2 < 0.8){
                this.fileData = corruption(this.fileData, 2);

            }
            // 20 percent chance to corrupt 3
            else {
                this.fileData = corruption(this.fileData, 3);
            }
        } else {
            System.out.println("Not gremlined packet");
        }
        this.checksum();
    }

}
