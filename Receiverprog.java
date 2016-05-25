package receiverside;

	import java.net.DatagramPacket;
	import java.net.DatagramSocket;
	import java.net.InetAddress;

	public class Receiverprog{

		static final int MAX_PACKET_SIZE=30;
		static byte[] secretKey;
		static byte[] sequenceNumber;
		
		public static void main(String[] args) throws Exception {
			secretKey=new byte[]{3,45,34,24,57,89,65,12,3,111,23,59,54,78,12,98};;
//			for(int i=0;i<secretKey.length;i++){
//				secretKey[i]=(byte) i;
//			}
			sequenceNumber=new byte[4];
			for(int i=0;i<sequenceNumber.length;i++){
				sequenceNumber[i]=(byte) i;
		    }
			
			int RECEIVER_PORT_NUMBER=9999;
			int TRANSMITTER_PORT_NUMBER=9998;
			
			byte[] receivedMessage = new byte[MAX_PACKET_SIZE+10]; 
			DatagramPacket receivePacket = new DatagramPacket(receivedMessage,receivedMessage.length);
			DatagramSocket serverSocket = new DatagramSocket( RECEIVER_PORT_NUMBER); 
			
			System.out.println("Received Message:");
			while(true){
				// receiving client's request
				serverSocket.receive(receivePacket); 
				byte[] stream=new byte[MAX_PACKET_SIZE+6];
				for(int i=0;i<MAX_PACKET_SIZE+6;i++){
					stream[i]=receivePacket.getData()[i];
				}
				byte[] decryptedData =Decrypt(stream);
				if(checkPak(receivePacket.getData(),decryptedData)){
					incrementSequenceNumber();
				}
			 
				byte[] ack=new byte[5];
				ack[0]=(byte) 0xff;
				for(int i=1;i<=4;i++){
					ack[i]=sequenceNumber[i-1];
				}
				
				byte[] iCheck=IntegrityCheckACK(ack);
				byte[] ackPak=new byte[9];
			 
				for(int i=0;i<=4;i++){
					ackPak[i]=ack[i];
				}
				for(int i=5;i<9;i++){
					ackPak[i]=iCheck[i-5];
				}
			 byte[] recip={(byte)10,(byte)0,(byte)0,(byte) (244-12)};
				InetAddress rece=InetAddress.getLocalHost();
				DatagramPacket sentPacket=new DatagramPacket(ackPak,ackPak.length,rece,TRANSMITTER_PORT_NUMBER);
				DatagramSocket receiverSocket=new DatagramSocket();
				receiverSocket.send(sentPacket);
			 
				for(int i=6; i<(int)decryptedData[5]+6; i++){
					System.out.print(decryptedData[i]+" ");
				}
				System.out.println();
			}
		//Main ends
	}

		public static byte[]  IntegrityCheck(byte[] encryptedData){
			byte[] integrityCheck=new byte[4];
			int j=0;
			byte k;
			for(j=0;j<4;j++){
				k=encryptedData[j];
				for(int i=j+4;i<encryptedData.length;i++){
					k=(byte) (k^encryptedData[i]);
				}
				integrityCheck[j]=k;
			}
			return integrityCheck;
		}
			 
		public static byte[]  IntegrityCheckACK(byte[] encryptedData){
			byte[] data=new byte[8];
			for(int i=0;i<5;i++){
				data[i]=encryptedData[i];
			}
			byte[] integrityCheck=new byte[4];
			int j=0;
			byte k;
			for(j=0;j<4;j++){
				k=data[j];
				for(int i=j+4;i<data.length;i++){
					k=(byte) (k^data[i]);
				}
				integrityCheck[j]=k;
			}
			return integrityCheck;
		}
			 
		public static boolean checkPak(byte[] pak,byte[] decryptPak){
			byte[] checkPak1=new byte[pak.length-4];
			for(int i=0;i<pak.length-4;i++){
				checkPak1[i]=pak[i];
			}
			byte[] iCheck=new byte[4];
			for(int i=0;i<4;i++){
				iCheck[i]=pak[i+pak.length-4];
			}
			byte[] cICheck=IntegrityCheck(checkPak1);
				 
			byte[] seqNum=new byte[4];
			for(int i=0;i<4;i++){
				seqNum[i]=decryptPak[i+1];
			}
			byte[] payLoad=new byte[decryptPak.length-6];
			for(int i=0;i<decryptPak.length-6;i++){
				payLoad[i]=decryptPak[i+6];
			}
			int n=0,m=0;
			for(int i=0;i<4;i++){
				if(cICheck[i]==iCheck[i])
					n=n+1;
			}
			for(int i=0;i<4;i++){
				if(seqNum[i]==sequenceNumber[i])
					m=m+1;
			}
			if((decryptPak[0]==0x55||decryptPak[0]==0xaa) && n==4 && m==4 && payLoad.length<=MAX_PACKET_SIZE)
				return true;
			else
				return false;
		}
		
		public static void incrementSequenceNumber(){
			for(int i=3;i>=0;i--){
				if(sequenceNumber[i]<(byte)127){
					sequenceNumber[i]=(byte) (sequenceNumber[i]+1);
					return;
				}
				else
					i=i-1;
			}
		}
			 
		public byte[] Encrypt(byte[] payload,boolean last){
			RC4 rc4=new RC4(secretKey);
			int n=payload.length+6,i;
			byte[] data=new byte[n+(n%4)];
			if(last)
				data[0]=(byte) 0xaa;
			else
				data[0]=(byte) 0x55;
			for(i=1;i<=4;i++){
				data[i]=sequenceNumber[i-1];
			}
			data[5]=(byte) payload.length;
			for(i=6;i<n;i++){
				data[i]=payload[i-6];
			}
			return rc4.encrypt(data);
		}
			 
		public static byte[] Decrypt(byte[] payload){
			RC4 rc4=new RC4(secretKey);
			return rc4.decrypt(payload);
		}
	}


