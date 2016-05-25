package finalProject;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Random;



public class Transmitter {
	
	//maximum size of the payload 
	static final int MAX_PACKET_SIZE=30;
	//the size of the message or the data which has to be transmitted
	static final int MESSAGE_SIZE=500;
	//the timeout value for the socket is set to 1 seconds
	final static int TIMEOUT_VAL_MS=1000;
	//creating two new variables of type byte[]
	static byte[] secretKey;
	static byte[] sequenceNumber;

	public static void main(String[] args) throws Exception {
		//assigning the same  random byte array to the transmitter and the receiver
		secretKey=new byte[]{3,45,34,24,57,89,65,12,3,111,23,59,54,78,12,98};
		//assigning a random byte array to the sequence number 
        sequenceNumber=new byte[4];
		for(int i=0;i<sequenceNumber.length;i++){
			sequenceNumber[i]=(byte) i;
	    }//end of for loop
		
		//the port number to which the transmitter sends the packets
		int RECEIVER_PORT_NUMBER=9999;
		//the port number through which the transmitter listens
		int TRANSMITTER_PORT_NUMBER=9998;
		//the received  acknowledgments will be stored in this array 
		byte[] acknowledgment=new byte[9];
		
		//The received acknowledgement UDP packet
		DatagramPacket ackPak = new DatagramPacket(acknowledgment,acknowledgment.length);
		//Transmitter socket.Receiving on this port number
		DatagramSocket receiverSocket = new DatagramSocket(TRANSMITTER_PORT_NUMBER);
		
		//Creating  500 bytes of random data
		byte[] message=new byte[MESSAGE_SIZE];
		new Random().nextBytes(message);
		//Printing out the message packet wise in each line
		System.out.println("Message:");
		for(int k=0;k<MESSAGE_SIZE-(MESSAGE_SIZE%MAX_PACKET_SIZE);k=k+MAX_PACKET_SIZE){
			for(int i=k;i<k+MAX_PACKET_SIZE;i++)
				System.out.print(message[i]+" ");
			System.out.println();
		}//end of for loop
		//printing out the last payload of the packet 
		for(int k=MESSAGE_SIZE-(MESSAGE_SIZE%MAX_PACKET_SIZE);k<MESSAGE_SIZE;k++)
			System.out.print(message[k]+" ");
		System.out.println();
		
		//Initializing the payload with a size of 30 bytes
		byte[] payload=new byte[MAX_PACKET_SIZE];

		//Initializing a variable of type int
		int packetNum=0;
		//dividing the message of length 500 bytes into 30 bytes block 
		for(int k=0;k<MESSAGE_SIZE-(MESSAGE_SIZE%MAX_PACKET_SIZE);k=k+MAX_PACKET_SIZE){
			packetNum++;
			//creating the payload from the message
			for(int i=k;i<k+MAX_PACKET_SIZE;i++)
				payload[i-k]=message[i];
			
			//encrypting the payload along with the header by calling the method 
			//Encrypt.The method adds the header and encrypts the payload.
			byte[] encryptedData=Encrypt(payload,false);
			//Calculating the integrity check by calling the method the IntegrityCheck
			byte[] integrityCheck=IntegrityCheck(encryptedData);
		    
			
			//creating the packet which has to be sent to the receiver
			byte[] packet=new byte[encryptedData.length+4];
			//forming a packet by attaching the encryptedData and the integrity
			//check fields.
			for(int i=0;i<encryptedData.length;i++)
				packet[i]=encryptedData[i];
			for(int i=encryptedData.length;i<packet.length;i++)
				packet[i]=integrityCheck[i-encryptedData.length];
		    
			
			// creating the IP address object for the server machine
			//  loop back the request to the same machine
			InetAddress receiver=InetAddress.getLocalHost();
		    
			//Creating the UDP packet to send
			DatagramPacket sentPacket=new DatagramPacket(packet,packet.length,receiver,RECEIVER_PORT_NUMBER);
			// creating the UDP transmitter socket (randomly chosen client port number)
			DatagramSocket receSocket=new DatagramSocket();
		
			//creating new variables of types boolean and integer
			boolean check; int trial=1,max_trials=3;
			//Using a do while loop for executing the stop and wait protocol
			do{
				//systems sends three packets and waits for the acknowledgments 
				//if there are no ackn0wledgments then the transmitter stop with the message
				if(trial>max_trials){
					System.out.println("Communication error");
					System.exit(0);
				}//end of if
				//calls the method communicate which receives the acknowledgments 
				//and processes these acknowledgments 
				Communicate(1,packetNum,max_trials,receSocket,sentPacket,receiverSocket,ackPak);
				//After receiving the acknowledgments calls the method checkAck which checks
				//the packet type and all other fields
				check=checkAck(ackPak.getData());
				trial=trial+1;
				//executes this until a correct ack is received or 3 packets are sent
			}while(!check);//do while ends
			
		}//for loop ends
		
		//increment the packet number
		packetNum++;
		//creating a byte array for sending the last payload with 20 bytes message
		byte[] lastPayload=new byte[MESSAGE_SIZE%MAX_PACKET_SIZE];
		//forms the last payload from 480 to 500 bytes of the original message
		int k=MESSAGE_SIZE-(MESSAGE_SIZE%MAX_PACKET_SIZE);
		for(int i=k;i<MESSAGE_SIZE;i++){
			lastPayload[i-k]=message[i];
		}//for loop ends
		
		//encrypting the payload along with the header by calling the method 
		//Encrypt.The method adds the header and encrypts the payload.
		byte[] encryptedData=Encrypt(lastPayload,true);
		//Calculating the integrity check by calling the method the IntegrityCheck
		byte[] integrityCheck=IntegrityCheck(encryptedData);
		
		//forming a packet by attaching the encryptedData and the integrity
		//check fields.
		byte[] packet=new byte[encryptedData.length+4];
		for(int i=0;i<encryptedData.length;i++)
			packet[i]=encryptedData[i];
		for(int i=encryptedData.length;i<packet.length;i++)
			packet[i]=integrityCheck[i-encryptedData.length];
	    
		
		
		// creating the IP address object for the server machine
		//  loop back the request to the same machine
		InetAddress receiver=InetAddress.getLocalHost();
		
		//Creating the UDP packet to send
		DatagramPacket sentPacket=new DatagramPacket(packet,packet.length,receiver,RECEIVER_PORT_NUMBER);
		// creating the UDP client socket (randomly chosen client port number)
		DatagramSocket receSocket=new DatagramSocket();
		
		//creating new variables of types boolean and integer
		boolean check; int trial=1,max_trials=3;
		//Using a do while loop for executing the stop and wait protocol
		do{
			//systems sends three packets and waits for the acknowledgments 
			//if there are no ackn0wledgments then the transmitter stop with the message
			if(trial>max_trials){
				System.out.println("Communication error");
				System.exit(0);
			}//end of if
			//calls the method communicate which receives the acknowledgments 
			//and processes these acknowledgments 
			Communicate(1,packetNum,max_trials,receSocket,sentPacket,receiverSocket,ackPak);
			//After receiving the acknowledgments calls the method checkAck which checks
			//the packet type and all other fields
			check=checkAck(ackPak.getData());
			trial=trial+1;
			//executes this until a correct ack is received or 3 packets are sent
		}while(!check);//Do while ends
		
	}//end of main
	
	
	//Definition of the method IntefrityCheck of which takes an input byte stream and 
	//returns the encrypted byte sequence
	public static byte[] IntegrityCheck(byte[] encryptedData){
		byte[] integrityCheck=new byte[4];
		int j=0;
		byte k;
		//the input byte sequence is divided into blocks of bytes and the 4 bytes of
		//the blocks are XORed.And are stored in integrity Check
		for(j=0;j<4;j++){
			k=encryptedData[j];
			for(int i=j+4;i<encryptedData.length;i++){
				k=(byte) (k^encryptedData[i]);
			}//end of for loop
			integrityCheck[j]=k;
		}//end of for loop
		return integrityCheck;
	}
	
	
	//This method checks the integrity check of the received acknowledgments
	public static byte[] IntegrityCheckACK(byte[] encryptedData){
		byte[] data=new byte[8];
		for(int i=0;i<5;i++){
			data[i]=encryptedData[i];
		}//end of for loop
		byte[] integrityCheck=new byte[4];
		int j=0;
		byte k;
		//the input byte sequence is divided into blocks of bytes and the 4 bytes of
		//the blocks are XORed.And are stored in integrity Check
		for(j=0;j<4;j++){
			k=data[j];
			for(int i=j+4;i<data.length;i++){
				k=(byte) (k^data[i]);
			}//end of for loop
			integrityCheck[j]=k;
		}//end of for loop
		return integrityCheck;
	 }//end of the method
	
	
	//method to implement stop and wait protocol i.e if a timeout occurs it sends
	//the sends the packets again with doubling the timeout value and waits for the 
	//acknowledgments
	public static void Communicate(int trial,int packetNum,int maxTrials,DatagramSocket receSocket,DatagramPacket sentPacket,DatagramSocket receiverSocket,DatagramPacket ackPak) throws IOException{
		if(trial>maxTrials){
			System.out.println("Communication Error");
			System.exit(0);
		}//end of if
		else{
			System.out.println("trial "+(trial)+" packet "+packetNum);
			
			//send the packet to the receiver
			receSocket.send(sentPacket);
			//double the value of timeout for each timeout
			receiverSocket.setSoTimeout((int) (Math.pow(2,trial-1)*TIMEOUT_VAL_MS));
			try{
				receiverSocket.receive(ackPak);
				// the receive() method blocks here (program execution stops)
				// only two ways to continue: a) packet is received 
				//(normal execution // after the catch block); 
				//b) timeout (exception is thrown)
			}catch (InterruptedIOException e3){
				// timeout - timer expired before receiving the response from the server
				//Uses recursion to call it self until the trials exceed the max trials
				Communicate(trial+1,packetNum,maxTrials,receSocket,sentPacket,receiverSocket,ackPak);
			}//end of catch statement
		}//end of else statement
	}//end of the method Communicate
	
	
	//Defining a method to check whether the received acknowledgment is correct or not
	//by checking the packet type and integrity check and it sets the sequence number 
	//if correct ack is received
	public static boolean checkAck(byte[] ack){
		byte[] ack1=new byte[5];
		for(int i=0;i<5;i++){
			ack1[i]=ack[i];
		}//end of for
		
		//extracting intigrity check from the ack
		byte[] iCheck=new byte[4];
		for(int i=0;i<4;i++){
			iCheck[i]=ack[i+5];
		}//end of for
		
		//extracting the sequence number from the ack
		byte[] seqNum=new byte[4];
		for(int i=0;i<4;i++){
			seqNum[i]=ack[i+1];
		}// end of for
		
		//calculating the integrity check locally
		byte[] cICheck=IntegrityCheckACK(ack);

		//checking both the integrity checks
		int n=0;
		for(int i=0;i<4;i++){
			if(cICheck[i]==iCheck[i])
				n++;
		}
		//if the packet type and integrity check are coorect the the sequence number is set
		if(ack[0]==(byte)0xff && n==4){
			setSequenceNumber(seqNum);
			return true;
		}
		else
			return false;
	}
	
	
	//defining the method Encrypt to encrypt the data and to add the header information
	public static byte[] Encrypt(byte[] payload,boolean last){
		//creating a new rc4 object
		RC4 rc4=new RC4(secretKey);
		int n=payload.length+6,i;
		//if its the last packet then set the packet type to 0xaa or set it to 0x55
		byte[] data=new byte[n+(n%4)];
		if(last)
			data[0]=(byte) 0xaa;
		else
			data[0]=(byte) 0x55;
		//add the sequence number
		for(i=1;i<=4;i++){
			data[i]=sequenceNumber[i-1];
		}
		//forming the packet
		data[5]=(byte) payload.length;
		for(i=6;i<n;i++){
			data[i]=payload[i-6];
		}
		//returning the encrypted data
		return rc4.encrypt(data);
	}
	//method to decrypt the input byte sequence
//	public byte[] Derypt(byte[] payload){
//		RC4 rc4=new RC4(secretKey);
//		return rc4.decrypt(payload);
//	}
    
	//method to set the sequence number to the given sequence number
	public static void setSequenceNumber(byte[] snum){
		for(int i=0;i<4;i++)
			sequenceNumber[i]=snum[i];
	}
}
