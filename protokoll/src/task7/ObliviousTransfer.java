package task7;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;

import chiffre.Grundlagen;
import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.krypto.protokoll.Communicator;
import de.tubs.cs.iti.krypto.protokoll.Protocol;

public final class ObliviousTransfer implements Protocol {
	private final boolean DEBUG = true;
	private final boolean TEST = true;

	private static final int RADIX_SEND_ = 16;
	private BigInteger zwei = new BigInteger("2", 10);

	static private int MinPlayer = 2;
	static private int MaxPlayer = 2;
	static private String NameOfTheGame = "ObliviousTransfer";

	private Communicator Com;
	private boolean betrug_ = false;

	public void setCommunicator(Communicator com) {
		Com = com;
	}

	public void sendFirst() {

		// (0)a Alice erzeugt sich einen ElGamal Key
		BigInteger myGamalP;
		BigInteger myGamalG;
		BigInteger myY;
		BigInteger myX;
		BigInteger help;
		if (TEST) {
			myGamalP = new BigInteger(
					"13261063939096985426999424781129436987736604484071841574839029035275097976621226106248381646461633027127647215070176806960882462844165647876651836347109303");
			myGamalG = new BigInteger(
					"11449415071830494793854044177711897602839781159400329949451774490076059017229975065899539762216842867220320484076072264155276684642243703364069496832384226");
			myY = new BigInteger(
					"12291108192856071170865558012429961903760322492409283286333655332690208506622303938336813071688446187130979374752507108428801107842293004214159912505057697");
			myX = new BigInteger(
					"338247438063093584360735553456651782895945714953753136968197534452413025437614400799748890371900646240882573007655796701481099145579155445557798688838152");
			help = myGamalP.subtract(BigIntegerUtil.TWO);
		} else {
			System.out.print("A: Generiere El-Gamal Key für mich... "
					+ "Augenblick...");
			int bitLength = 512;
			BigInteger[] prime = Grundlagen.generatePrimePQ(bitLength);
			myGamalP = prime[0];
			myGamalG = Grundlagen.calcPrimeRoot(myGamalP, prime[1]);
			System.out.println("\t [OK]");
			// A wählt x zufällig in {1,...,p-2}
			help = myGamalP.subtract(BigIntegerUtil.TWO);
			myX = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
			// A berechnet y = g^xA mod p
			myY = myGamalG.modPow(myX, myGamalP);
		}

		// (0)b Alice sendet ihren PublicKey an Bob
		Com.sendTo(1, myGamalP.toString(RADIX_SEND_)); // p
		Com.sendTo(1, myGamalG.toString(RADIX_SEND_)); // g
		Com.sendTo(1, myY.toString(RADIX_SEND_)); // yA
		if (DEBUG) {
			System.out.println("DDD| (0) A sendet an B:");
			System.out.println("DDD| \t p = " + myGamalP);
			System.out.println("DDD| \t g = " + myGamalG);
			System.out.println("DDD| (1) A sendet yA an B: " + myY);
		}

		// (0)c Alice gibt zwei Nachrichten M1 und M2 an, von denen Bob eine
		// erhalten soll
		System.out
				.println("Geben sie jetzt die beiden Nachrichten an, von denen Bob eine erhalten soll.");
		System.out.println("Nachricht 1: ");
		String M0 = askString();
		BigInteger messM0 = new BigInteger(M0, 36);
		System.out.println("Nachricht 2: ");
		String M1 = askString();
		BigInteger messM1 = new BigInteger(M1, 36);

		if (messM0.equals(messM1)) {
			betrug_ = true;
			System.out
					.println("DDD| Alice betrügt, indem sie zwei gleiche Nachrichten eingibt.");
		}

		if (DEBUG) {
			System.out.println("DDD| (0)c Alice Nachrichten");
			System.out.println("DDD| \t Nachricht 1: " + M0);
			System.out.println("DDD| \t Nachricht 1: " + messM0);
			System.out.println("DDD| \t Nachricht 1: " + messM0.toString(36));
			System.out.println("DDD| \t Nachricht 2: " + M1);
			System.out.println("DDD| \t Nachricht 2: " + messM1);
			System.out.println("DDD| \t Nachricht 2: " + messM1.toString(36));
		}

		// (1)a Alice wählt zufällig zwei weitere Nachrichten m1 und m2;
		BigInteger[] m = new BigInteger[2];
		m[0] = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
		m[1] = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
		// (1)b Alice sendet m1 und m2 an Bob
		Com.sendTo(1, m[0].toString(RADIX_SEND_)); // m1
		Com.sendTo(1, m[1].toString(RADIX_SEND_)); // m2
		if (DEBUG) {
			System.out.println("DDD| (1) A sendet an B:");
			System.out.println("DDD| \t m1 = " + m[0]);
			System.out.println("DDD| \t m2 = " + m[1]);
		}
		// (2) Alice empfängt q von Bob
		String getq = Com.receive();
		BigInteger q = new BigInteger(getq, RADIX_SEND_);
		if (DEBUG) {
			System.out.println("DDD| (2) A empfängt von B");
			System.out.println("DDD| \t q = " + q);
		}
		// (3)a Alice berechnet k0' und k1' und signiert sie
		// ki'=(D_A((q-m[i])mod p²))mod p
		BigInteger[] k = new BigInteger[2];
		k[0] = (q.subtract(m[0])).mod(myGamalP.multiply(myGamalP));
		k[0] = Grundlagen.elGamalDecipher(k[0], myX, myGamalP);
		k[0] = k[0].mod(myGamalP);
		k[1] = (q.subtract(m[1])).mod(myGamalP.multiply(myGamalP));
		k[1] = Grundlagen.elGamalDecipher(k[1], myX, myGamalP);
		k[1] = k[1].mod(myGamalP);
		// (3)b Alice signiert k0 und k1
		BigInteger[] Sk = new BigInteger[2];
		Sk[0] = Grundlagen.elGamalSign(k[0], myGamalP, myGamalG, myY, myX);
		Sk[1] = Grundlagen.elGamalSign(k[1], myGamalP, myGamalG, myY, myX);
		if (betrug_) {
			BigInteger i = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
			int ii = (i.mod(zwei)).intValue();
			System.out.println("DDD| Fälsche Signatur k" + ii);
			Sk[ii] = BigIntegerUtil.randomBetween(BigInteger.ONE,
					myGamalP.subtract(zwei));
		}
		if (DEBUG) {
			boolean t0 = Grundlagen.elGamalVerify(k[0], Sk[0], myGamalP,
					myGamalG, myY);
			boolean t1 = Grundlagen.elGamalVerify(k[1], Sk[1], myGamalP,
					myGamalG, myY);
			System.out.println("DDD| (3)b Prüfe Signaturen:");
			System.out.println("DDD| \t Sk[0] ist " + t0);
			System.out.println("DDD| \t Sk[1] ist " + t1);
		}
		// (3)c Alice sendet beide Signaturen an Bob
		Com.sendTo(1, Sk[0].toString(RADIX_SEND_));
		Com.sendTo(1, Sk[1].toString(RADIX_SEND_));
		if (DEBUG) {
			System.out.println("DDD| (3) A sendet an B:");
			System.out.println("DDD| \t S(k0) = " + Sk[0]);
			System.out.println("DDD| \t S(k1) = " + Sk[1]);
		}
		// (3)d Alice wählt zufällig s aus {0,1}
		BigInteger sbig = BigIntegerUtil.randomBetween(BigInteger.ONE, help);
		sbig = sbig.mod(zwei);
		int s;
		// (3)e Alice berechnet (M_0+ks')mod n, (M_1+ks+1')mod n
		s = sbig.intValue(); // s
		BigInteger send0 = k[s].add(messM0).mod(myGamalP); // (M_0 + k[s]') mod
															// n
		s = sbig.xor(BigInteger.ONE).intValue(); // s xor 1
		BigInteger send1 = k[s].add(messM1).mod(myGamalP); // (M_1 + k[s+1]')
															// mod n
		// (3)f send0 und send1 beides und s an Bob senden
		Com.sendTo(1, send0.toString(RADIX_SEND_)); // send0
		Com.sendTo(1, send1.toString(RADIX_SEND_)); // send1
		Com.sendTo(1, sbig.toString(RADIX_SEND_)); // s
		if (DEBUG) {
			System.out.println("DDD| (3)d-f Berechnete Werte");
			System.out.println("DDD| \t s = " + sbig);
			System.out.println("DDD| \t send0 = " + send0.toString(16));
			System.out.println("DDD| \t send1 = " + send1.toString(16));
		}

		// (4) nichts tun
	}

	public void receiveFirst() {
		// (0) Bob empfängt den Public-Key von Alice
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
		// (1)b Bob empfängt m1 und m2
		String m1 = Com.receive();
		String m2 = Com.receive();
		BigInteger[] m = new BigInteger[2];
		m[0] = new BigInteger(m1, RADIX_SEND_);
		m[1] = new BigInteger(m2, RADIX_SEND_);
		if (DEBUG) {
			System.out.println("DDD| (0) B empfängt von A:");
			System.out.println("DDD| \t m1 = " + m1);
			System.out.println("DDD| \t m2 = " + m2);
			System.out.println("DDD| \t m[0] = " + m[0]);
			System.out.println("DDD| \t m[1] = " + m[1]);
		}
		// (2)a Bob wählt zufällig r aus {0,1} und k aus Z_p
		BigInteger k = BigIntegerUtil.randomBetween(BigInteger.ONE,
				partnerGamalP);
		BigInteger r_z = BigIntegerUtil.randomBetween(BigInteger.ONE,
				zwei.multiply(zwei));
		int r = (r_z.mod(zwei)).intValue();
		// (2)b Bob berechnet q=(E_A(k)+m_r)mod p²
		BigInteger eak = Grundlagen.elGamalEncipher(k, partnerGamalP,
				partnerGamalG, partnerY);
		BigInteger q = (eak.add(m[r])).mod(partnerGamalP
				.multiply(partnerGamalP));
		// (2)c Bob sendet q an Alice
		Com.sendTo(0, q.toString(RADIX_SEND_));
		if (DEBUG) {
			System.out.println("DDD| (2) B sendet an A:");
			System.out.println("DDD| \t r = " + r);
			System.out.println("DDD| \t q = " + q);
		}
		// (3)b Bob empfängt die Signaturen von k0' und k1' von Alice
		sReceive = Com.receive();
		BigInteger Sk0 = new BigInteger(sReceive, RADIX_SEND_);
		sReceive = Com.receive();
		BigInteger Sk1 = new BigInteger(sReceive, RADIX_SEND_);
		if (DEBUG) {
			System.out.println("DDD| (3) B empfängt von A:");
			System.out.println("DDD| \t Sk0 = " + Sk0);
			System.out.println("DDD| \t Sk1 = " + Sk1);
		}
		// (3)d Bob empfängt (M_0+ks')mod n, (M_1+ks+1')mod n und s von Alice
		sReceive = Com.receive();
		BigInteger rec0 = new BigInteger(sReceive, RADIX_SEND_);
		sReceive = Com.receive();
		BigInteger rec1 = new BigInteger(sReceive, RADIX_SEND_);
		sReceive = Com.receive();
		BigInteger s = new BigInteger(sReceive, RADIX_SEND_);
		if (DEBUG) {
			System.out.println("DDD| (3)d Bob hat empfangen");
			System.out.println("DDD| \t rec0 (send0) = " + rec0.toString(16));
			System.out.println("DDD| \t rec1 (send1) = " + rec1.toString(16));
			System.out.println("DDD| \t s = " + s.toString(16));
		}

		// (4)a Bob berechnet M_(s xor r)
		BigInteger biR = new BigInteger("" + r, 10);
		BigInteger calc;
		BigInteger calcQuer; // Das ungenutzte, andere received
		BigInteger t = biR.xor(s); // t = r xor s
		if (s.intValue() == 0 && r == 0) { // t=0
			calc = rec0; // M0 = M0 + k0
			calcQuer = rec1; // M1 = M1 + k1
		} else if (s.intValue() == 0 && r == 1) { // t=1
			calc = rec1; // M1 = M1 + k1
			calcQuer = rec0; // M0 = M0 + k0
		} else if (s.intValue() == 1 && r == 1) { // t=0
			calc = rec0; // M0 = M0 + k1
			calcQuer = rec1; // M1 = M1 + k0
		} else { // t=1
			calc = rec1; // M1 = M1+k0
			calcQuer = rec0; // M0 = M0 + k1
		}
		// M_(s xor r)
		calc = calc.mod(partnerGamalP);
		calc = calc.subtract(k); // sendT - k
		calc = calc.mod(partnerGamalP); // sendT - k mod p

		// kQuer_(r xor 1) = (calcQuer mod p - calc) mod p
		BigInteger kQuer;
		kQuer = calcQuer.mod(partnerGamalP);
		kQuer = kQuer.subtract(calc);
		kQuer = kQuer.mod(partnerGamalP);

		BigInteger test = calcQuer.mod(partnerGamalP);
		test = test.subtract(k);
		test = test.mod(partnerGamalP);

		if (DEBUG) {
			System.out.println("DDD| (4)a Bob berechnet M_(s xor r)");
			System.out
					.println("DDD| \t s xor r = " + s + " xor " + r + "=" + t);
			System.out.println("DDD| \t M_(s xor r) = " + calc);
			System.out.println("DDD| \t M_(s xor r) = " + calc.toString(36));
			System.out.println("DDD| \t Test = " + test);
			System.out.println("DDD| \t Test = " + test.toString(36));
		}

		// (4)b Bob prüft, ob Alice betrogen hat
		boolean checkCheat = false;
		// (4)b Prüfe, ob Signaturen für k und/oder kQuer gelten
		boolean s0OK = Grundlagen.elGamalVerify(k, Sk0, partnerGamalP,
				partnerGamalG, partnerY);
		boolean s1OK = Grundlagen.elGamalVerify(k, Sk1, partnerGamalP,
				partnerGamalG, partnerY);
		boolean sQ0OK = Grundlagen.elGamalVerify(kQuer, Sk0, partnerGamalP,
				partnerGamalG, partnerY);
		boolean sQ1OK = Grundlagen.elGamalVerify(kQuer, Sk1, partnerGamalP,
				partnerGamalG, partnerY);
		if (DEBUG) {
			System.out.println("DDD| (4)b Prüfe gültige Siganturen");
			System.out.println("DDD| \t k  : " + s0OK + " \t " + s1OK);
			System.out.println("DDD| \t kQ : " + sQ0OK + " \t " + sQ1OK);
		}

		if (DEBUG) {
			System.out.println("DDD| (4)b BETRUGSVERSUCHE:");
		}
		// (4)b Signatur auf k ist falsch
		boolean cheat1;
		cheat1 = !(s0OK ^ s1OK);
		// (4)b Signatur gilt für beide k
		boolean cheat2;
		cheat2 = ((s0OK && sQ0OK) || (s1OK && sQ1OK));
		// (4)b Sk0=Sk1, identische Signaturen
		boolean cheat3;
		cheat3 = Sk0.equals(Sk1);
		// (4)b EA(kQuer(rxor1)) = (q-m(rxor1)) mod p
		boolean cheat4;
		BigInteger eQuer;
		BigInteger qQuer;
		eQuer = Grundlagen.elGamalEncipher(kQuer, partnerGamalP, partnerGamalG,
				partnerY);
		qQuer = q.subtract(m[biR.xor(BigInteger.ONE).intValue()]).mod(
				partnerGamalP);
		cheat4 = eQuer.equals(qQuer);
		if (DEBUG) {
			System.out.println("DDD| \t s(k) gilt nicht für k     : " + cheat1);
			System.out.println("DDD| \t s(k) gilt für k und kQ    : " + cheat2);
			System.out.println("DDD| \t s(k0)==s(k1)              : " + cheat3);
			System.out.println("DDD| \t EA(kQ)==(q-m)mod n        : " + cheat4);
		}

		checkCheat = cheat1 || cheat2 || cheat3 || cheat4;
		if (checkCheat) {
			System.out.println("Betrüger!");
		} else {
			System.out.println("Kein Betrug von Alice festgestellt.");
			System.out.println("Nachricht: " + calc.toString(36));
		}

	}

	public String nameOfTheGame() {
		return NameOfTheGame;
	}

	public int minPlayer() {
		return MinPlayer;
	}

	public int maxPlayer() {
		return MaxPlayer;
	}

	private String askString() {
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s = "Tschüss bis zum nächsten Mal"; // dummymessage
		try {
			if ((s = in.readLine()) != null && s.length() != 0) {
				// nix tun, in s steht jetzt der String
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return s;
	}
}
