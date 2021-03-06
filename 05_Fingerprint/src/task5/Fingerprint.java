/*
 * jCrypt - Programmierumgebung für das Kryptologie-Praktikum
 * Studienarbeit am Institut für Theoretische Informatik der
 * Technischen Universität Braunschweig
 * 
 * Datei:        Fingerprint.java
 * Beschreibung: Dummy-Implementierung der Hash-Funktion von Chaum, van Heijst
 *               und Pfitzmann
 * Erstellt:     30. März 2010
 * Autor:        Martin Klußmann
 */

package task5;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Random;

import de.tubs.cs.iti.jcrypt.chiffre.BigIntegerUtil;
import de.tubs.cs.iti.jcrypt.chiffre.HashFunction;

/**
 * Dummy-Klasse für die Hash-Funktion von Chaum, van Heijst und Pfitzmann.
 *
 * @author Martin Klußmann
 * @version 1.1 - Sat Apr 03 22:20:18 CEST 2010
 */
public final class Fingerprint extends HashFunction {

private boolean DEBUG = false;
	
	private BigInteger myP_;
	private BigInteger myG1_;
	private BigInteger myG2_;
	
  /**
   * Berechnet den Hash-Wert des durch den FileInputStream
   * <code>cleartext</code> gegebenen Klartextes und schreibt das Ergebnis in
   * den FileOutputStream <code>ciphertext</code>.
   * 
   * @param cleartext
   * Der FileInputStream, der den Klartext liefert.
   * @param ciphertext
   * Der FileOutputStream, in den der Hash-Wert geschrieben werden soll.
   */
  public void hash(FileInputStream cleartext, FileOutputStream ciphertext) {
	  //testwerte
	  /*myP_=new BigInteger("2999",10);
	  myG1_=new BigInteger("17",10);
	  myG2_=new BigInteger("1235",10);*/
	  
	  ArrayList<Byte> clear = getClear(cleartext);
	  BigInteger hashvalue = hashIt(clear);
	  System.out.println(">>>hashvalue "+hashvalue.toString(16));
	  char[] outputChar = hashvalue.toString(16).toCharArray();
	  try{
		  for (char c : outputChar) {
			  //hier was besseres ausdenken - erledigt!
			  ciphertext.write(c);
		  }
	  } catch (IOException e){
		  System.err.println(e);
	  }
  }

private ArrayList<Byte> getClear(FileInputStream cleartext) {
	ArrayList<Byte> back = new ArrayList<Byte>();
	byte b[]=new byte[1];
	try{
		while(cleartext.read(b) != -1){
			back.add(b[0]);
		}
	} catch (IOException e){
		System.err.println(e);
	}
	return back;
}

public BigInteger hashIt(ArrayList<Byte> clear) {
	//Länge von P herausfinden, damit passende Stückchen eingelesen werden können
	int Lp=myP_.bitLength();
	int clearlen = clear.size();
	int m = 2*(Lp-2);
	int xlen = m - Lp -1;
	if (DEBUG) System.out.println(">>>xlen is "+xlen);
	int numrounds = (clearlen*8)/xlen;
	BigInteger gi, giplus1;
	giplus1=null;
	//schreibe die bits aus den bytes in die BigInts undzwar jeweils ceil(m/2) und floor(m/2)
	BigInteger x[]= new BigInteger[numrounds+1];
	BigInteger zwei = new BigInteger("2",10);
	int position = 0;
	for(int i=0; i<numrounds;i++){
		//x[i] auf Länge m mit 1en füllen
		x[i]= (zwei.pow(xlen)).subtract(BigInteger.ONE);
		//if (DEBUG) System.out.println(">>>initial x["+i+"] " +x[i]);
		for(int j=xlen; j>0;j--){
			//falls das passende bit aber 0 ist, wieder abziehen
			int seewhatitis = (clear.get(position/8)/((int)Math.pow(2,position%8)))%2;
			//if (DEBUG) System.out.println(">>>seewhatitis " +seewhatitis);
			if(seewhatitis==0){
				x[i]=x[i].subtract(zwei.pow(j-1));
			}
			//if (DEBUG) System.out.println(">>>x["+i+"] " +x[i]);
			position++;
		}
	}
	//rest von x_k mit 0en füllen, x_k+1 mit d=#0en
	int d=(numrounds+1)*xlen-clearlen*8;
	for(int j=d;j>0;j--){
		x[numrounds-1]=x[numrounds-1].subtract(zwei.pow(j-1));
	}
	if (d==0) {
		x[numrounds]=new BigInteger("0",10);
	} else {
		x[numrounds]=BigInteger.valueOf(d);
	}
	
	gi=myG2_.modPow(x[0], myP_);
	BigInteger appendone;
	for(int i=1;i<numrounds;i++){
		appendone=gi.multiply(zwei).add(BigInteger.ONE);
		giplus1=myG1_.modPow(appendone, myP_);
		gi=giplus1.multiply(myG2_.modPow(x[i], myP_));
	}
	return gi;
}

  /**
   * Erzeugt neue Parameter.
   * 
   * @see #readParam readParam
   * @see #writeParam writeParam
   */
  public void makeParam() {
	  // Erzeuge Parameter p,g1,g2
	  int bitLength = enterBitLength();
	  
	  // erzeuge sichere Primzahl p, min 512 bits
	  Random random = new Random();
	  boolean isPrime = false;
	  BigInteger p = BigIntegerUtil.TWO;
	  BigInteger q = BigIntegerUtil.TWO;
	  do {
		  q = BigInteger.probablePrime(bitLength-1, random);
		  p = q.multiply(BigIntegerUtil.TWO);
		  p = p.add(BigInteger.ONE);
		  
		  isPrime = p.isProbablePrime(99);
	  } while (!isPrime);
	  
	  // erzeuge g1,g2 primitive Wurzeln mod p
	  BigInteger g1, g2;
	  boolean checkEqRoot = true;
	  do {
		  g1 = calcPrimeRoot(p, q);
		  g2 = calcPrimeRoot(p, q);
		  
		  checkEqRoot = g1.equals(g2);
	  } while (checkEqRoot);
	  
	  // Setze globale Variablen
	  myP_ = p;
	  myG1_ = g1;
	  myG2_ = g2;
  }

  /**
   * Liest die Parameter mit dem Reader <code>param</code>.
   * 
   * @param param
   * Der Reader, der aus der Parameterdatei liest.
   * @see #makeParam makeParam
   * @see #writeParam writeParam
   */
  public void readParam(BufferedReader param) {
	  
	  try {
		  // Hole Pfade der Paramdatei
		  myP_ = new BigInteger(param.readLine());
		  myG1_ = new BigInteger(param.readLine());
		  myG2_ = new BigInteger(param.readLine());
		  
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
	  
  }

  /**
   * Berechnet den Hash-Wert des durch den FileInputStream
   * <code>cleartext</code> gegebenen Klartextes und vergleicht das
   * Ergebnis mit dem durch den FileInputStream <code>ciphertext</code>
   * gelieferten Wert.
   *
   * @param ciphertext
   * Der FileInputStream, der den zu prüfenden Hash-Wert liefert.
   * @param cleartext
   * Der FileInputStream, der den Klartext liefert, dessen Hash-Wert berechnet
   * werden soll.
   */
  public void verify(FileInputStream ciphertext, FileInputStream cleartext) {
	  ArrayList<Byte> text = getClear(cleartext);
	  BigInteger textvalue = hashIt(text);
	  byte hashed[]= new byte[textvalue.bitLength()/8];
	  try {
		  String readCipher = "";
		  int read;
		  do {
			  read = ciphertext.read();
			  if(read != -1) {
				  readCipher = readCipher + (char)read;
				  //if(DEBUG) {System.out.println(read + "\t" + readCipher);}
			  }
		  } while (read != -1);
		  //if(DEBUG) {System.out.println(">>> gelesener Cipher: " + readCipher);}
		  
		  
//		  BigInteger hashtext = new BigInteger(hashed);
		  BigInteger hashtext = new BigInteger(readCipher,16);
		  if (DEBUG) System.out.println(">>>hashtext \t"+hashtext.toString(16));
		  if (DEBUG) System.out.println(">>>textvalue \t"+textvalue.toString(16));
		  if (hashtext.equals(textvalue)) {
			  System.out.println("hash verifiziert");
		  } else {
			  System.out.println("hash abgelehnt");
		  }
	  } catch (IOException e) {
		  System.err.println(e);
	  }
  }
  
  
  
  
  /**
   * Schreibt die Parameter mit dem Writer <code>param</code>.
   * 
   * @param param
   * Der Writer, der in die Parameterdatei schreibt.
   * @see #makeParam makeParam
   * @see #readParam readParam
   */
  public void writeParam(BufferedWriter param) {
	  
	  // Schreibe Pfade
	  try {
		  param.write("" + myP_);
		  param.newLine();
		  param.write("" + myG1_);
		  param.newLine();
		  param.write("" + myG2_);
		  param.close();
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
	  System.out.println("    * Schlüsseldatei gespeichert");
	  
  }
  
  public void setDebug(boolean b) {
	  DEBUG = b;
  }
  
  public boolean getDebug() {
	  return DEBUG;
  }
  
  

  /**
   * Algo 7.3
   * @param bitLength
   * @return
   */
  private BigInteger calcPrimeRoot(BigInteger p, BigInteger q) {
	  
	  // Generiere primitive Wurzel g in Z_p^*
	  boolean checkRoot = false;
	  BigInteger g = BigIntegerUtil.TWO;
	  BigInteger biNeg1 = new BigInteger("-1");
	  biNeg1 = biNeg1.mod(p);
	  do {
		  g = generateReducedRest(p);
		  boolean isNotOne = !g.equals(BigInteger.ONE);
		  boolean isNotP1 = !g.equals(p.subtract(BigInteger.ONE));
		  if(isNotOne || isNotP1) {
			  BigInteger h;
			  h = g.modPow(q, p);
			  checkRoot = (h.equals(biNeg1));
		  } else {
			  checkRoot = false;
		  }
	  } while (checkRoot);
	  
	  // Setze Rückgabevariabel
	  return g;
  }
  

  /**
   * Definition 3.2
   * @param modulus
   * @return
   */
  private BigInteger generateReducedRest(BigInteger modulus) {
	  	  
	  BigInteger reducedRest = BigInteger.ZERO; // Rückgabe
	  Random randomGenerator = new Random();
	  boolean check = false;
	  while (!check) {
		  reducedRest = BigIntegerUtil.randomSmallerThan(modulus,randomGenerator);
		  
		  check = reducedRest.gcd(modulus).equals(BigInteger.ONE);
	  }
	  
	  return reducedRest;
	  
  }
  

  private int enterBitLength() {
	  
	  BufferedReader standardInput = launcher.openStandardInput();
	  boolean accepted = false;

	  String msg = "    ! Bitte geben sie die gewünschte Bitlänge für die Primzahl P an (minimum 512):";
	  System.out.println(msg);
	  int bitLength = 0; // Rückgabe
	  do {
		  msg = "    ! Bitte geben sie die Bitlänge an.";
		  System.out.print("      ");
		  try {
			  String sIn = standardInput.readLine();
			  if(sIn.length() == 0 || sIn == null) {
				  // Standardwert bei "keiner" Eingabe
				  bitLength = 512;
				  accepted = true;
			  } else {
				  bitLength = Integer.parseInt(sIn);
				  if(bitLength >= 512) {
					  accepted = true;
				  } else {
					  accepted = false;
					  System.out.println(msg);
				  }
			  }
		  } catch (IOException e) {
			  System.err.println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
			  e.printStackTrace();
			  System.exit(1);
		  } catch (NumberFormatException e) {
			  System.err.println("      Keine gültige Zahl.");
			  System.out.println(msg);
		  }
	  } while (!accepted);
	  
	  return bitLength;
  }
}
