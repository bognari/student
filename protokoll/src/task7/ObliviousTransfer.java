package task7;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

import chiffre.Grundlagen;
import task6.StationToStation;
import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.*;
public final class ObliviousTransfer implements Protocol {
	private final boolean DEBUG = true;

	private static final int RADIX_SEND_ = 16;
	private BigInteger zwei = new BigInteger("2",10);
	
	static private int MinPlayer        = 2;
	static private int MaxPlayer        = 2;
	static private String NameOfTheGame = "ObliviousTransfer";

    private Communicator Com;
	
	public void setCommunicator(Communicator com)
	{
	  Com = com;
	}
	
	
	public void sendFirst ()
	{
		//TODO User fragen, ob und wo Alice betrügen soll
		
		//(0)a Alice erzeugt sich einen ElGamal Key
		System.out.print("A: Generiere El-Gamal Key für mich... "
				+ "Augenblick...");
		int bitLength = 512;
		BigInteger[] prime = Grundlagen.generatePrimePQ(bitLength);
		BigInteger myGamalP = prime[0];
		BigInteger myGamalG = Grundlagen.calcPrimeRoot(myGamalP, prime[1]);
		System.out.println("\t [OK]");
		//A wählt x zufällig in {1,...,p-2}
		BigInteger help = myGamalP.subtract(BigIntegerUtil.TWO);
		BigInteger myX = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
		//A berechnet y = g^xA mod p
		BigInteger myY = myGamalG.modPow(myX, myGamalP);
		
		//(0)b Alice sendet ihren PublicKey an Bob
		Com.sendTo(1, myGamalP.toString(RADIX_SEND_)); // p
		Com.sendTo(1, myGamalG.toString(RADIX_SEND_)); // g
		Com.sendTo(1, myY.toString(RADIX_SEND_)); // yA
		if (DEBUG) {
			System.out.println("DDD| (0) A sendet an B:");
			System.out.println("DDD| \t p = " + myGamalP);
			System.out.println("DDD| \t g = " + myGamalG);
			System.out.println("DDD| (1) A sendet yA an B: " + myY);
		}
		
		//(0)c Alice gibt zwei Nachrichten M1 und M2 an, von denen Bob eine erhalten soll
		System.out.println("Geben sie jetzt die beiden Nachrichten an, von denen Bob eine erhalten soll.");
		System.out.println("Nachricht 1: ");
		String M1 = askString();
		System.out.println("Nachricht 2: ");
		String M2 = askString();
		//(1)a Alice wählt zufällig zwei weitere Nachrichten m1 und m2;
		BigInteger mess1 = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
		String m1 = mess1.toString(36);//radix 36 damit auch viele Buchstaben raus kommen
		BigInteger mess2 = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
		String m2 = mess2.toString(36);
		BigInteger[] m = new BigInteger[2];
		m[0]= mess1;
		m[1]= mess2;
		//(1)b Alice sendet m1 und m2 an Bob
		Com.sendTo(1, m1); // m1
		Com.sendTo(1, m2); // m2
		if (DEBUG) {
			System.out.println("DDD| (1) A sendet an B:");
			System.out.println("DDD| \t m1 = " + m1);
			System.out.println("DDD| \t m2 = " + m2);
		}
		//(2) Alice empfängt q von Bob
		String getq = Com.receive();
		BigInteger q = new BigInteger(getq,RADIX_SEND_);
		if(DEBUG){
			System.out.println("DDD| (2) A empfängt von B");
			System.out.println("DDD| \t q = " + q);
		}
		//(3)a Alice berechnet k0' und k1' und signiert sie
		//ki'=(D_A((q-m[i])mod p²))mod p
		BigInteger[] k = new BigInteger[2];
		k[0] = (q.subtract(m[0])).mod(myGamalP.multiply(myGamalP));
		k[0] = Grundlagen.elGamalDecipher(k[0], myX, myGamalP);
		k[0] = k[0].mod(myGamalP);
		k[1] = (q.subtract(m[1])).mod(myGamalP.multiply(myGamalP));
		k[1] = Grundlagen.elGamalDecipher(k[1], myX, myGamalP);
		k[1] = k[1].mod(myGamalP);
		//(3)b Alice signiert k0 und k1
		BigInteger[] Sk = new BigInteger[2];
		Sk[0] = Grundlagen.elGamalSign(k[0], myGamalP, myGamalG, myY, myX);
		Sk[1] = Grundlagen.elGamalSign(k[1], myGamalP, myGamalG, myY, myX);
		//(3)c Alice sendet beide Signaturen an Bob
		Com.sendTo(1, Sk[0].toString(RADIX_SEND_));
		Com.sendTo(1, Sk[1].toString(RADIX_SEND_));
		if (DEBUG) {
			System.out.println("DDD| (3) A sendet an B:");
			System.out.println("DDD| \t S(k0) = " + Sk[0]);
			System.out.println("DDD| \t S(k1) = " + Sk[1]);
		}
		//(3)d Alice wählt zufällig s aus {0,1}
		BigInteger sbig = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
		sbig = sbig.mod(zwei);
		int s = sbig.intValue();
		//(3)e Alice berechnet (M_0+ks')mod n, (M_1+ks+1')mod n und sendet beides und s an Bob
		
		//(4) nichts tun
	}
	
	public void receiveFirst ()
	{
		//(0) Bob empfängt den Public-Key von Alice
		String sReceive = Com.receive();
		BigInteger partnerGamalP = new BigInteger(sReceive, RADIX_SEND_);
		sReceive = Com.receive();
		BigInteger partnerGamalG = new BigInteger(sReceive, RADIX_SEND_);
		sReceive = Com.receive();
		BigInteger partnerY = new BigInteger(sReceive, RADIX_SEND_);
		if (DEBUG) {
			System.out.println("DDD| (0) B empfängt von A:");
			System.out.println("DDD| \t p = " + partnerGamalP);
			System.out.println("DDD| \t g = " + partnerGamalG);
			System.out.println("DDD| \t y = " + partnerY);
		}
		//(1)b Bob empfängt m1 und m2
		String m1 = Com.receive();
		String m2 = Com.receive();
		if (DEBUG) {
			System.out.println("DDD| (0) B empfängt von A:");
			System.out.println("DDD| \t m1 = " + m1);
			System.out.println("DDD| \t m2 = " + m2);
		}
		BigInteger[] m = new BigInteger[2];
		byte[] t1 = m1.getBytes();
		m[0]= new BigInteger(t1);
		byte[] t2 = m2.getBytes();
		m[1]= new BigInteger(t2);
		//(2)a Bob wählt zufällig r aus {0,1} und k aus Z_p
		BigInteger k = BigIntegerUtil.randomBetween(BigInteger.ONE, partnerGamalP);
		BigInteger r_z = BigIntegerUtil.randomBetween(BigInteger.ONE, zwei.multiply(zwei));
		int r = (r_z.mod(zwei)).intValue();
		//(2)b Bob berechnet q=(E_A(k)+m_r)mod p²
		BigInteger eak = Grundlagen.elGamalEncipher(k, partnerGamalP, partnerGamalG, partnerY);
		BigInteger q = (eak.add(m[r])).mod(partnerGamalP.multiply(partnerGamalP)) ;
		//(2)c Bob sendet q an Alice
		Com.sendTo(0, q.toString(RADIX_SEND_));
		if (DEBUG) {
			System.out.println("DDD| (2) B sendet an A:");
			System.out.println("DDD| \t q = " + q);
		}
		//(3)b Bob empfängt die Signaturen von k0' und k1' von Alice
		sReceive = Com.receive();
		BigInteger Sk0 = new BigInteger(sReceive, RADIX_SEND_);
		sReceive = Com.receive();
		BigInteger Sk1 = new BigInteger(sReceive, RADIX_SEND_);
		if (DEBUG) {
			System.out.println("DDD| (3) B empfängt von A:");
			System.out.println("DDD| \t Sk0 = " + Sk0);
			System.out.println("DDD| \t Sk1 = " + Sk1);
		}
		//(3)d Bob empfängt (M_0+ks')mod n, (M_1+ks+1')mod n und s von Alice
		//(4)a Bob berechnet M_s+r
		//(4)b Bob prüft, ob Alice betrogen hat

	}
	
	public String nameOfTheGame ()
	{
		return NameOfTheGame;
	}
	
	public int minPlayer ()
	{
		return MinPlayer;
	}
	
	public int maxPlayer ()
	{
		return MaxPlayer;
	}
	
	private String askString() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    String s="Tschüss bis zum nächsten Mal"; //dummymessage
	    try {
			if ((s = in.readLine()) != null && s.length() != 0){
				//nix tun, in s steht jetzt der String
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s;
	}
}
