/*
 * jCrypt - Programmierumgebung für das Kryptologie-Praktikum
 * Studienarbeit am Institut für Theoretische Informatik der
 * Technischen Universität Braunschweig
 * 
 * Datei:        RunningKey.java
 * Beschreibung: Dummy-Implementierung der Chiffre mit laufendem Schlüssel
 * Erstellt:     30. März 2010
 * Autor:        Martin Klußmann
 */

package task2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import de.tubs.cs.iti.jcrypt.chiffre.Cipher;

/**
 * Dummy-Klasse für die Chiffre mit laufendem Schlüssel.
 *
 * @author Martin Klußmann
 * @version 1.0 - Tue Mar 30 16:23:47 CEST 2010
 */
public class RunningKey extends Cipher {
	
	final boolean DEBUG = false;
	
	//int keyAlphLenght; //modulus
	String keyFilePath;
	ArrayList<ArrayList<Integer[]>> lookup;

  /**
   * Analysiert den durch den Reader <code>ciphertext</code> gegebenen
   * Chiffretext, bricht die Chiffre bzw. unterstützt das Brechen der Chiffre
   * (ggf. interaktiv) und schreibt den Klartext mit dem Writer
   * <code>cleartext</code>.
   *
   * @param ciphertext
   * Der Reader, der den Chiffretext liefert.
   * @param cleartext
   * Der Writer, der den Klartext schreiben soll.
   */
  public void breakCipher(BufferedReader ciphertext, BufferedWriter cleartext) {
	//Bereite Schlüsseltext-Datei vor
	keyFilePath = "key_text.txt"; //Datei mit Schlüsseltext
	writeToFile(keyFilePath, ""); //Legt die Datei für Schlüsseltext an
		
	//Erfrage vermutete Alphabetgröße/Modulus
	BufferedReader standardInput = launcher.openStandardInput();
	boolean accepted = false;

	String msg = "Bitte geben Sie die Größe des vermuteten Alphabetes ein:";
	System.out.println(msg);
	do {
	  msg = "Bitte geben Sie die Größe des vermuteten Alphabetes ein:";
	  try {
		modulus = Integer.parseInt(standardInput.readLine());
		if (modulus < 1) {
		  System.out.println(
				  "Eine Größe des Alphabetes unter 1 wird nicht akzeptiert. " +
				  "Bitte korrigieren Sie Ihre Eingabe.");
		} else {
			msg = "Die Größe des Alphabetes wurde aktzeptiert. Das verwendete Alphabet umfasst " +
				modulus + " Zeichen.";
			System.out.println(msg);
			accepted = true;  
		}
	  } catch (NumberFormatException e) {
		  System.out.println("Fehler beim Parsen der Alphabetsgröße. Bitte korrigieren"
				 + " Sie Ihre Eingabe.");
	  } catch (IOException e) {
		  System.err.println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
		  e.printStackTrace();
		  System.exit(1);
	  }
	} while (!accepted);
	
	createLookupTable(modulus);
	
	//Chiffre start
	msg = "Beginne mit dem Verfahren zum Brechen der Chiffre.";
	System.out.println(msg);
	double[] weights = enterWeighting();
	//Lese die Buchstaben des Ciphertextes ein
	  ArrayList<Integer> cipherChars;
	  cipherChars = readBufferedReaderToList(ciphertext);
	
	  //Mache Platz für Klartext und Schlüsseltext
	  int[] klartext=new int[cipherChars.size()];
	  int[] schluesseltext=new int[cipherChars.size()];
	  
	//Fülle Klar-und Schlüsseltext mit -1 damit festgestellt werden kann, ob eine Textstelle schon behandelt wurde
	  for(int i=0;i<klartext.length;i++) klartext[i]=-1;
	  for(int i=0;i<schluesseltext.length;i++) schluesseltext[i]=-1;
	   
	//Solange der User noch Lust hat, Textteile zu entziffern: 
	boolean fertig=false;
	do {
		//Erfrage die Position und Länge des Ciphertextabschnittes, den der User betrachten möchte
		int start=0; int laenge=4;
		start = enterIndexes();
		if(DEBUG) System.out.println("Abschnitt ab " + start + "  (Länge: " + laenge + ")");
		ArrayList<Integer> abschnitt = getAbschnitt(start,laenge,cipherChars);
		//falls der abschnitt nicht richtig gefüllt wurde ->Neustart
		if(abschnitt ==null)continue;
		//Zeige bereits entschlüsselte Abschnitte, falls sie angrenzen/überlappen
		showClearAndKeyText(start,laenge,klartext,schluesseltext);
		//Analysiere den Abschnitt auf wahrscheinliche Klar & Schlüsseltexte
		ArrayList<String[]> possible4grams=getPossible4grams(abschnitt);
		if(possible4grams.size()==0){
			msg="Die Analyse war nicht erfolgreich, da der Chiffretext nicht aus den häufigsten Zeichen zusammengesetzt ist.\n Bitte wählen sie einen anderen Abschnitt.";
			System.out.println(msg);
			continue;
		}
		
		//Sortiere nach Bewertung
		ArrayList<Double> allWeights = new ArrayList<Double>();
		for(int i = 0; i < possible4grams.size(); i++) {
			String[] ausgabetmp = possible4grams.get(i);
			allWeights.add(evaluatePart(ausgabetmp[0],ausgabetmp[1],weights));
		}
		Collections.sort(allWeights);
		Collections.reverse(allWeights);
		
		if(DEBUG) System.out.println(">>>> Liste wird sortiert.");
		long timeStart = System.nanoTime();

		ArrayList<String[]> ausgabeNew = new ArrayList<String[]>();
		for(int i = 0; i < possible4grams.size();i++) {
			String[] s = {"----","----","0"};
			ausgabeNew.add(s);
		}
				
		Iterator<String[]> it = possible4grams.iterator();
		while(it.hasNext()) {
			String[] ausgabetmp = it.next();
			double w = evaluatePart(ausgabetmp[0],ausgabetmp[1],weights);
			int index = allWeights.indexOf(w);
			String[] sOut = {ausgabetmp[0],ausgabetmp[1],""+w};
			if(index >=0) {
				ausgabeNew.set(index,sOut);
				allWeights.set(index, -1.0);
			}
		}
		if(DEBUG) System.out.println(">>>> Dauer der Sortierung: " + ((System.nanoTime() - timeStart)/(1000000000)) + "s");
		
		//Gib dem User die bewerteten 4gram Paare aus
		System.out.println("Die am Höchsten bewerteten 4-Gramme (Ausschnitt aus den " + ausgabeNew.size() + " 4-Grammen):");
		Iterator<String[]> itOut = ausgabeNew.iterator();
		int counter = 0;
		while(itOut.hasNext() && counter < 20){
			String[] ausgabetmp = itOut.next();
			System.out.println("\t" + ausgabetmp[0] + "\t" + ausgabetmp[1]+"\t"+ausgabetmp[2]);
			counter++;
		}
		//Schreibe alle 4Gramme und ihre Gewichtung in eine Datei
		String filename = "out" + File.separator + "4gram-sorted.txt";
		try {
			FileWriter writer = new FileWriter(filename);
			BufferedWriter out = new BufferedWriter(writer);
			itOut = ausgabeNew.iterator();
			while(itOut.hasNext()){
				String[] ausgabetmp = itOut.next();
				out.write(ausgabetmp[0] + "\t" + ausgabetmp[1]+"\t"+ausgabetmp[2] + "\n");
			}
			out.close();
		} catch (IOException e){
			e.printStackTrace();
		}		
		System.out.println("Die komplette Auflistung der sortierten 4-Gramme findet sich unter '" + filename +"'.");
	
		//Bitte den User um eine Auswahl und speichere sein Ergebnis ab
		setClearAndKeyText(start,klartext,schluesseltext,abschnitt);
		//Abfrage ob der Text vollständig bearbeitet wurde oder der User schon zufrieden ist, dann
		System.out.println("Möchten Sie die Entschlüsselung beenden? [y/n]");
		try{
			String yesorno = standardInput.readLine();
			if (yesorno.equalsIgnoreCase("y")||yesorno.equalsIgnoreCase("")) fertig=true;
		} catch(IOException e) {
			System.err.println(e);
		}
	} while (!fertig);
	
	//Schreibe den Schlüssel voller 'a' überall da wo bisher nix drin steht 
	for(int i = 0; i < schluesseltext.length; i++) {
		if(schluesseltext[i] == -1) {
			schluesseltext[i] = 97;
		}
	}
	
	//Speicher Schlüsseltext
	ArrayList<Character> brokenKey = new ArrayList<Character>();
	for (int i = 0; i < schluesseltext.length; i++) {
		brokenKey.add((char) schluesseltext[i]);
	}
	writeToFile(keyFilePath, brokenKey);
  }

private void setClearAndKeyText(int start,int[] klartext, int[] schluesseltext, ArrayList<Integer> abschnitt) {
	String msg;
	String input, yesorno;
	int tempo;
	char[] tmp = new char[4];
	BufferedReader standardInput = launcher.openStandardInput();
	boolean entscheidung=false;
	do{
		msg = "Bitte geben Sie nun die 4 Zeichen des Klartextes ein, die Sie abspeichern möchten";
		System.out.println(msg);
		try{ 
			input=standardInput.readLine();
			if(input.length()!=4){
				System.out.println("Bitte geben Sie GENAU 4 Zeichen ein.");
				continue;
			}
			for(int i=0;i<4;i++){
				tempo = (charMap.mapChar(abschnitt.get(i))-charMap.mapChar(input.charAt(i)) + modulus)%modulus;				
				if(DEBUG) {
					System.out.print(">>>> Werte prüfen: ");
					System.out.print(charMap.mapChar(abschnitt.get(i)) + "\t");
					System.out.print(charMap.mapChar(input.charAt(i)) + "\t");
					System.out.print((charMap.mapChar(abschnitt.get(i))-charMap.mapChar(input.charAt(i)) + modulus) + "\t");
					System.out.print((charMap.mapChar(abschnitt.get(i))-charMap.mapChar(input.charAt(i)) + modulus)%modulus + "\t");
					System.out.print(tempo  + "\t");
					System.out.print(charMap.remapChar(tempo) + "\t");
					System.out.print((char)charMap.remapChar(tempo));
					System.out.println();
				}
				tmp[i]=(char) charMap.remapChar(tempo);
			}
			msg = "Der passende Schlüsseltext ist "+ String.valueOf(tmp) +"\nEinverstanden? [y/n]";
			System.out.println(msg);
			yesorno=standardInput.readLine();
			if(yesorno.equalsIgnoreCase("y")||yesorno.equalsIgnoreCase("")) {
				entscheidung=true;
				for(int i=0;i<input.length();i++){
					klartext[i+start]=input.charAt(i);
					schluesseltext[i+start]=tmp[i];
				}
				msg="Klar und Schlüsseltext wurden abgespeichert.";
				System.out.println(msg);
				if(klartext.length>start+4 && klartext[start+4]!=-1){
					msg="Der auf den Klartext folgende Text lautet :";
					int i=start+4;
					while(klartext.length>i && klartext[i]!=-1){
						msg+= (char) klartext[i];
						i++;
					}
					msg+="\nMöchten Sie Klar- und Schlüsseltext ab hier tauschen?[y/n]";
					System.out.println(msg);
					yesorno=standardInput.readLine();
					if(yesorno.equalsIgnoreCase("y")||yesorno.equalsIgnoreCase("")) {
						int temp;
						for(int j=start+4;j<klartext.length;j++){
							temp=klartext[j];
							klartext[j]=schluesseltext[j];
							schluesseltext[j]=temp;
						}
						System.out.println("Klar- und Schlüsseltext wurden ab Position "+(start+4)+" getauscht.");
					}
				} 
			}
		}catch(IOException e) {
			System.err.println(e);
		}
	}while(!entscheidung);
}

private ArrayList<String[]> getPossible4grams(ArrayList<Integer> abschnitt) {
	ArrayList<String[]> possible4grams = new ArrayList<String[]>();
	String clear,key;
	char tmp;
	//gehe für jeden der 4 Ciffre-Buchstaben alle möglichen Paare häufigster Buchstaben durch und bilde alle Kombinationen
	for(int i=0; i<lookup.get(charMap.mapChar(abschnitt.get(0))).size();i++){
		for(int j=0; j<lookup.get(charMap.mapChar(abschnitt.get(1))).size();j++){
			for(int k=0; k<lookup.get(charMap.mapChar(abschnitt.get(2))).size();k++){
				for(int l=0; l<lookup.get(charMap.mapChar(abschnitt.get(3))).size();l++){
					//Schreibe aktuelle Kombinationen in Strings und von da aus in possible4grams
					clear="";
					key="";
					tmp=(char) charMap.remapChar(lookup.get(charMap.mapChar(abschnitt.get(0))).get(i)[0]);
					clear += tmp;
					tmp=(char) charMap.remapChar(lookup.get(charMap.mapChar(abschnitt.get(0))).get(i)[1]);
					key += tmp;
					tmp=(char) charMap.remapChar(lookup.get(charMap.mapChar(abschnitt.get(1))).get(j)[0]);
					clear += tmp;
					tmp=(char) charMap.remapChar(lookup.get(charMap.mapChar(abschnitt.get(1))).get(j)[1]);
					key += tmp;
					tmp=(char) charMap.remapChar(lookup.get(charMap.mapChar(abschnitt.get(2))).get(k)[0]);
					clear += tmp;
					tmp=(char) charMap.remapChar(lookup.get(charMap.mapChar(abschnitt.get(2))).get(k)[1]);
					key += tmp;
					tmp=(char) charMap.remapChar(lookup.get(charMap.mapChar(abschnitt.get(3))).get(l)[0]);
					clear += tmp;
					tmp=(char) charMap.remapChar(lookup.get(charMap.mapChar(abschnitt.get(3))).get(l)[1]);
					key += tmp;
					String[] tmp2={clear,key};
					possible4grams.add(tmp2);
				}
			}
		}
	}
	return possible4grams;
}

private void createLookupTable(int modulus) {
	int[] mostfreq = new int[10];
	mostfreq[0]=charMap.mapChar('e');
	mostfreq[1]=charMap.mapChar('n');
	mostfreq[2]=charMap.mapChar('r');
	mostfreq[3]=charMap.mapChar('i');
	mostfreq[4]=charMap.mapChar('s');
	mostfreq[5]=charMap.mapChar('a');
	mostfreq[6]=charMap.mapChar('d');
	mostfreq[7]=charMap.mapChar('t');
	mostfreq[8]=charMap.mapChar('h');
	mostfreq[9]=charMap.mapChar('u');
	if (modulus == 27 || modulus == 31 || modulus == 33){
		mostfreq[9]=charMap.mapChar('*');
	}
	if (modulus == 90 || modulus == 91){
		mostfreq[9]=charMap.mapChar(' ');
	}
	Integer[] tmp={0,0};
	lookup = new ArrayList<ArrayList<Integer[]>>();
	for (int i=0;i<modulus;i++){
		lookup.add(new ArrayList<Integer[]>());
		for(int j=0;j<10;j++){
			for(int k=0;k<10;k++){
				if((mostfreq[j]+mostfreq[k])%modulus == i){
					tmp = new Integer[2];
					tmp[0]=mostfreq[j];
					tmp[1]=mostfreq[k];
					lookup.get(i).add(tmp);
				}
			}
		}
	}
}

private void showClearAndKeyText(int start, int laenge, int[] klartext, int[] schluesseltext) {
	if(start < 0) start = 0;
	if(laenge < 0) laenge = 0;
	//Prüfe ob es angrenzend oder überlappend zum Textabschnitt ab start 
	//bis start+laenge bereits entschlüsselte Textstellen gibt.
	boolean notext=true;
	for(int i=Math.max(start-1,0);i<Math.min(start+laenge+1,klartext.length);i++){
		if(klartext[i]!=-1 && schluesseltext[i]!=-1) notext=false;
	}
	if(notext) {
		System.out.println("Kein bereits entschlüsselter Abschnitt grenzt an den gewählten Abschnitt");
	} else {
		//finde bereits entschlüsselte Abschnitte und gib sie aus
		int j=Math.max(start-1,0);
		while (j>0 && klartext[j]!=-1 && schluesseltext[j]!=-1){
			j--;
		}
		int i=j;
		while(i<klartext.length){
			ArrayList<Character> klar=new ArrayList<Character>();
			ArrayList<Character> schl=new ArrayList<Character>();
			while(klartext[i]!=-1 && schluesseltext[i]!=-1){
				klar.add((char)klartext[i]);
				schl.add((char)schluesseltext[i]);
				i++;
				if(i>=klartext.length) break;
			}
			if(!klar.isEmpty()){
				System.out.println("Bereits entzifferter Klartext\n von Position "+j+" bis Position "+(i-1)+":\n"+String.valueOf(klar));
				System.out.println("Bereits entzifferter Schlüsseltext\n von Position "+j+" bis Position "+(i-1)+":\n"+String.valueOf(schl));
			}
			j=++i;
			if(j>Math.min(start+5,klartext.length)) break;
		}
	}
}

private ArrayList<Integer> getAbschnitt(int start, int laenge, ArrayList<Integer> cipherChars) {
	//Checken ob Start und Länge zulässig sind
	if (start<0 || laenge<=0 || start+laenge>cipherChars.size()){
		System.out.println("Ungültige Eingabe. Start muss zwischen 0 und "+(cipherChars.size()-4)+" sein.");
		return null;
	}
	//abschnitt aus dem ciphertext herauskopieren
	ArrayList<Integer> abschnitt = new ArrayList<Integer>();
	for(int i=start;i<(start+laenge);i++){
		abschnitt.add(cipherChars.get(i));
	}
	return abschnitt;
}

/**
   * Entschlüsselt den durch den Reader <code>ciphertext</code> gegebenen
   * Chiffretext und schreibt den Klartext mit dem Writer
   * <code>cleartext</code>.
   *
   * @param ciphertext
   * Der Reader, der den Chiffretext liefert.
   * @param cleartext
   * Der Writer, der den Klartext schreiben soll.
   */
  public void decipher(BufferedReader ciphertext, BufferedWriter cleartext) {
	  if(DEBUG) System.out.println(">>>decipher called");
	  String msg = "";
	  
	  //Lese die Buchstaben des Keys ein
	  ArrayList<Integer> keyChars,cipherChars;
	  if(keyFilePath == null) {
		  keyFilePath = "out/out.txt"; //Workaround
	  }
	  if (DEBUG) System.out.println(">>>> keyFilePath=" + keyFilePath);
	  keyChars = readFileToList(keyFilePath);
	  cipherChars = readBufferedReaderToList(ciphertext);
	  
	  if(keyChars.size() >= cipherChars.size()) {
		  doDencipher(keyChars,cipherChars,cleartext);
	  } else {
		  msg = "Schlüsseldatei ist zu klein! Verschlüsseln wird abgebrochen. " +
		  		"Empfohlene Mindestlänge des Schlüssels ist " + cipherChars.size();
		  System.out.println(msg);
	  }  

  }

  /**
   * Verschlüsselt den durch den Reader <code>cleartext</code> gegebenen
   * Klartext und schreibt den Chiffretext mit dem Writer
   * <code>ciphertext</code>.
   * 
   * @param cleartext
   * Der Reader, der den Klartext liefert.
   * @param ciphertext
   * Der Writer, der den Chiffretext schreiben soll.
   */
  public void encipher(BufferedReader cleartext, BufferedWriter ciphertext) {
	  if(DEBUG) System.out.println(">>>encipher called");
	  String msg = "";
	  
	  //Lese die Buchstaben des Keys ein
	  ArrayList<Integer> keyChars,clearChars;
	  keyChars = readFileToList(keyFilePath);
	  clearChars = readBufferedReaderToList(cleartext);
	  
	  if(keyChars.size() >= clearChars.size()) {
		doEncipher(keyChars,clearChars,ciphertext);
		try {
			ciphertext.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	  } else {
		  msg = "Schlüsseldatei ist zu klein! Verschlüsseln wird abgebrochen. " +
		  		"Empfohlene Mindestlänge des Schlüssels ist " + clearChars.size();
		  System.out.println(msg);
	  }
  }

  /**
   * Erzeugt einen neuen Schlüssel.
   * 
   * @see #readKey readKey
   * @see #writeKey writeKey
   */
  public void makeKey() {
	if(DEBUG) System.out.println(">>>makeKey called");
	
    int alphabetLength = 0; //Laenge des verwendeten Alphabets
    String keypath = null; //Datei mit Schluesseltext
    
    BufferedReader standardInput = launcher.openStandardInput();
    
    //Einlesen der Größe des Alphabets
    boolean accepted = false;
    String msg = "Bitte geben Sie die Größe des verwendeten Alphabetes ein:";
    System.out.println(msg);
    do {
      System.out.print("Geben Sie die Größe des Alphabetes ein: ");
      try {
        alphabetLength = Integer.parseInt(standardInput.readLine());
        if (alphabetLength < 1) {
          System.out.println(
        		  "Eine Größe des Alphabetes unter 1 wird nicht akzeptiert. " +
        		  "Bitte korrigieren Sie Ihre Eingabe.");
        } else {
	msg = "Die Größe des Alphabetes wurde aktzeptiert. Das Alphabet umfasst " +
		alphabetLength + " Zeichen.";
	System.out.println(msg);
	accepted = true;
          
        }
      } catch (NumberFormatException e) {
        System.out.println("Fehler beim Parsen der Alphabetsgröße. Bitte korrigieren"
            + " Sie Ihre Eingabe.");
      } catch (IOException e) {
        System.err
            .println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
        e.printStackTrace();
        System.exit(1);
      }
    } while (!accepted);
    
    //Einlesen des Dateinamens für Schlüsseltext
    accepted = false;
    do {
      try {
        System.out.print("Geben Sie den Dateinamen des Schlüsseltextes ein: ");
        keypath = standardInput.readLine();
        if (keypath.length() > 0) {
          msg = "Der Pfad zur Schlüsseldatei lautet: " + keypath;
          System.out.println(msg);
          accepted = true;
        } else {
          System.out.println("Der Dateiname ist zu kurz. " +
          		"Es ist mindestens ein Zeichen erforderlich.");
        }
      } catch (IOException e) {
        System.err
            .println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
        e.printStackTrace();
        System.exit(1);
      }
    } while (!accepted);
    
    //Erzeuge key
    if(accepted) {
		//Setze globale Variablen des Schlüssels
		modulus = alphabetLength;
		keyFilePath = keypath;
		
		//Speicherung erfolgt automatisch durch Job
    } 
  }

  /**
   * Liest den Schlüssel mit dem Reader <code>key</code>.
   * 
   * @param key
   * Der Reader, der aus der Schlüsseldatei liest.
   * @see #makeKey makeKey
   * @see #writeKey writeKey
   */
  public void readKey(BufferedReader key) {
	  if(DEBUG) System.out.println(">>>readKey called");
	  String s;
	  String[] sKey;
	  
	  try {
			s = key.readLine();
			sKey = s.split(" ", 2);
			if(DEBUG) { 
				System.out.print(">>>> Eingelesen: " + s);
				System.out.print("\t Array: " + Arrays.toString(sKey));
				System.out.println();
			}
			if(sKey.length == 2) {
				modulus = Integer.parseInt(sKey[0]);
				keyFilePath = sKey[1];
			} else {
				System.out.println("!ACHTUNG! Key wurde falsch eingelesen.");
			}
	  } catch (IOException e) {
			e.printStackTrace();
		}
  }

  /**
   * Schreibt den Schlüssel mit dem Writer <code>key</code>.
   * 
   * @param key
   * Der Writer, der in die Schlüsseldatei schreibt.
   * @see #makeKey makeKey
   * @see #readKey readKey
   */
  public void writeKey(BufferedWriter key) {
	  if(DEBUG) System.out.println(">>>writeKey called");
	  
	  try {
		  key.write("" + modulus);
		  key.write(" ");
		  key.write(keyFilePath);
		  key.close();
	  } catch (IOException e) {
		  System.out.println("Abbruch: Fehler beim Schreiben oder Schließen der "
	  + "Schlüsseldatei.");
	  e.printStackTrace();
	  System.exit(1);
	  }
  }
  
  /**
   * Liest aus einer Datei die einzelnen Zeichen aus und gibt die Zeichen als Liste zurück.
   * @param filePath	Pfad zur auszulesenden Datei
   * @return Liste der einzelnen Zeichen, Zahlenwerte der Zeichen
   */
  private ArrayList<Integer> readFileToList(String filePath) {
	  ArrayList<Integer> back = new ArrayList<Integer>();
  
	  try {
		  BufferedReader br = new BufferedReader(new FileReader(filePath));
		  back = readBufferedReaderToList(br);
	  } catch (FileNotFoundException e) {
		  e.printStackTrace();
	  } 
	  
	  return back;
  }
  
  /**
   * Liest aus einer Datei die einzelnen Zeichen aus und gibt die Zeichen als Liste zurück.
   * @param reader	BufferedReader mit der auszulesenden Datei
   * @return Liste der einzelnen Zeichen, Zahlenwerte der Zeichen
   */
  private ArrayList<Integer> readBufferedReaderToList(BufferedReader reader) {
	  ArrayList<Integer> back = new ArrayList<Integer>();
	  
	  try {
		  int character;
		  while(reader.ready()) {
			  character = reader.read();
			  back.add(character);
		  }
		  reader.close();
	  } catch (IOException e) {
		  e.printStackTrace();
	  }
	  
	  return back;
  }
  	
  	/** 
  	 * Methode zur eigentlichen Verschlüsselung
  	 * @param keyChars	Liste der einzelnen Zeichen der Schlüsseldatei
  	 * @param clearChars	Liste der einzelnen Zeichen der Klartextdatei
  	 */
	private void doEncipher(ArrayList<Integer> keyChars, 
			ArrayList<Integer> clearChars,BufferedWriter ciphertext) {

		// charMap.setConvertToLowerCase();
		// charMap.setConvertToUpperCase();

		try {
	  int character;
	  boolean characterSkipped = false;
	  boolean useNextKey = true;
	  	  
	  Iterator<Integer> keyIterator = keyChars.iterator();
	  Iterator<Integer> clearIterator = clearChars.iterator();
	  int shift = 0;
	  int keyChar = 0;
	  while(clearIterator.hasNext() && keyIterator.hasNext()) {
		  if(useNextKey) {
			  keyChar = keyIterator.next();	  
		  }		  
		  useNextKey = true;
		  
		  shift = keyChar;
		  character = clearIterator.next();
		  
		  if (charMap.mapChar(character) !=-1) {
			  character = charMap.mapChar(character);
			  shift = charMap.mapChar(shift);	
			  
		  character = (character + shift + modulus) % modulus;
		  character = charMap.remapChar(character);
		  System.out.print((char)character);
		  ciphertext.write(character);
		  } else {
			  characterSkipped = true;
			  useNextKey = false;
		  }
	  }
	  System.out.println();
	  if (characterSkipped) {
		System.out.println("Warnung: Mindestens ein Zeichen aus der "
		+ "Klartextdatei ist im Alphabet nicht\nenthalten und wurde "
		+ "überlesen.");
	  }
	} catch (IOException e) {
	  System.err.println("Abbruch: Fehler beim Zugriff auf Klar- oder "
	  + "Chiffretextdatei.");
	  e.printStackTrace();
	  System.exit(1);
	}
	
	}

	private void doDencipher(ArrayList<Integer> keyChars, 
			ArrayList<Integer> cipherChars, BufferedWriter cleartext) {

		  try {
		  int character;
		  int shift = 0;
		  boolean characterSkipped = false;

		  Iterator<Integer> keyIterator = keyChars.iterator();
		  Iterator<Integer> cipherIterator = cipherChars.iterator();
		  
		  ArrayList<String> newCleartext = new ArrayList<String>();
		  while(cipherIterator.hasNext() && keyIterator.hasNext()) {
			  shift     = keyIterator.next();
			  character = cipherIterator.next();
			  
			  if (charMap.mapChar(character) !=-1) {
				  character = charMap.mapChar(character);
				  shift = charMap.mapChar(shift);
	
			  character = (character - shift + modulus) % modulus;
			  character = charMap.remapChar(character);
			  newCleartext.add(Character.toString((char) character));
			  cleartext.write((char) character);
			  } else {
				  characterSkipped = true;
			  }
		  }

		  //Zeige deciphered Klartext
		  System.out.println("Ausschnitt aus dem Klartext: ");
		  Iterator<String> newClearIterator = newCleartext.iterator();
		  int counter = 0;
		  while(newClearIterator.hasNext() && counter < 100) {
			System.out.print(newClearIterator.next());
			counter++;
		  }
		  System.out.println();
		  
		  if (characterSkipped) {
		System.out.println("Warnung: Mindestens ein Zeichen aus der "
		+ "Klartextdatei ist im Alphabet nicht\nenthalten und wurde "
		+ "überlesen.");
		  }
		
		  //erst schließen, wenn kein weiterer Zugriff erforderlich ist!
		  cleartext.close();
		  } catch (IOException e) {
		  System.err.println("Abbruch: Fehler beim Zugriff auf Klar- oder "
		  + "Chiffretextdatei.");
		  e.printStackTrace();
		  System.exit(1);
		  }
				
	}
	
	private void writeToFile(String filename,ArrayList<Character> text) {
		try {
			  FileWriter writer = new FileWriter(filename);
			  BufferedWriter out = new BufferedWriter(writer);
			  for(int i=0;i<text.size();i++){
				  out.append(text.get(i));
		  		}
			  out.close();
		  } catch (IOException e){
			  e.printStackTrace();
		  }
	  }
	  
	  private void writeToFile(String filename,String text) {
		  try {
			  FileWriter writer = new FileWriter(filename);
			  BufferedWriter out = new BufferedWriter(writer);
			  out.write(text);
			  out.close();
		  } catch (IOException e){
			  e.printStackTrace();
		  }
	  }
	
	  
	private int enterIndexes() {
		int back = 0;
	    BufferedReader standardInput = launcher.openStandardInput();
		
		//Einlesen der Größe des Alphabets
	    boolean accepted = false;
	    String msg = "Bitte geben Sie Startindex von dem Textabschnitt an," +
	    		" den Sie betrachten möchten.";
	    System.out.println(msg);
	    do {
	      System.out.print("Geben Sie den Startindex an:");
	      try {
			back= Integer.parseInt(standardInput.readLine());
	        if (back < 0) {
	          System.out.println(
	        		  "Die Eingabe wurde abgelehnt. Der Startindex muss größer als 0 sein." +
	        		  "Der Startindex muss eine natürliche Zahl sein.");
	        } else {
				msg = "Es wird der Text ab Index " + back + " betrachtet.";
				System.out.println(msg);
				accepted = true;	          
	        }
	      } catch (IOException e) {
	    	  System.err.println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
	        e.printStackTrace();
	        System.exit(1);
	      } catch (NumberFormatException e){
	    	  System.out.println("Fehler beim Parsen des Startindexes.");
	    	  accepted = false;
	      }
	    } while (!accepted);
	    
		
		return back;
	}
	private double evalutateFormula(double[] g, double[][] k, double[][] s) {
		double back = 0;
		
		double value;
		double sum;
		
		//Klartextausschnitt
		value = 0;
		sum = 0;
		for(int j = 0;j < 3; j++) {
			sum = 0;
			for(int i = 0 ; i < 5-j - 1 ; i++) {
				sum = sum + k[j][i];
			}
			value = value + g[j] * sum;
		}
		back = back + value;
		
		//Schlüsseltextausschnit
		value = 0;
		sum = 0;
		for(int j = 0;j < 3; j++) {
			sum = 0;
			for(int i = 0 ; i < 5-j-1 ; i++) {
				sum = sum + s[j][i];
			}
			value = value + g[j] * sum;
		}
		back = back + value;
		
		return back;
	}
	
	private double evaluatePart(String klar, String key, double[] g) {
		double back = 0.0;
						
		//Bereite Buchstabensuppe vor
		String[] cMUni = new String[4]; String[] cKUni = new String[4];
		String[] cMDi = new String[3]; String[] cKDi = new String[3];
		String[] cMTri = new String[2]; String[] cKTri = new String[2];
		for(int i = 0; i < 4; i++) {
			cMUni[i] = "" + klar.charAt(i);
			cKUni[i] = "" + key.charAt(i);
		}
		for(int i = 0; i < 3; i++) {
			cMDi[i] = "" + klar.charAt(i) + klar.charAt(i+1);
			cKDi[i] = "" + key.charAt(i) + key.charAt(i+1);
		}
		for(int i = 0; i < 2; i++) {
			cMTri[i] = "" + klar.charAt(i) + klar.charAt(i+1) + klar.charAt(i+2);
			cKTri[i] = "" + key.charAt(i) + key.charAt(i+1) + key.charAt(i+2);
		}
		
		//Bereite Häufigkeitsvariablen vor
		String[][] freqUni  = readFrequencyTable("../table/1-grams_programmierer.alph.tab");
		String[][] freqDi   = readFrequencyTable("../table/2-grams_programmierer.alph.tab");
		String[][] freqTri  = readFrequencyTable("../table/3-grams_programmierer.alph.tab");
		
		double[][] k = new double[3][4];
		double[][] s = new double[3][4];
		for(int i = 0; i<k.length;i++) {
			k[0][i] = 0;s[0][i]=0;
			k[1][i] = 0;s[0][i]=0;
			k[2][i] = 0;s[0][i]=0;
		}
		
		//1gram abcd x (a b c d) y
		//2gram abcd xx xa (ab bc cd) dy yy
		//3gram abcd xxx xxa xab (abc bcd) cdy dyy yyy
		//1gram relative frequencies
		for(int i = 0; i < cMUni.length;i++) {
			k[0][i] = findFrequency(cMUni[i], freqUni);
			s[0][i] = findFrequency(cKUni[i], freqUni);
		}
		//2gram relative frequencies 
		for(int i = 0; i < cMDi.length;i++) {
			k[1][i] = findFrequency(cMDi[i], freqDi);
			s[1][i] = findFrequency(cKDi[i], freqDi);
		}
		//3gram relative frequencies
		for(int i = 0; i < cMTri.length;i++) {
			k[2][i] = findFrequency(cMTri[i], freqTri);
			s[2][i] = findFrequency(cKTri[i], freqTri);
		}
		
		back = evalutateFormula(g, k, s);
		
		return back;
	}
	
	private double findFrequency(String s, String[][] freq) {
		double back = 0.0;
		
		for(int i = 0; i < freq.length;i++) {
			if(freq[i][0].equals(s)) {
				back = Double.parseDouble(freq[i][1]);
			}
		}
		
		return back;
	}

  private String[][] readFrequencyTable(String filename){
	  String[][] table;
	  String[][] help;
	  StringBuffer help1 = new StringBuffer("");
	  String helper;
	  try{
		  BufferedReader file = new BufferedReader(new FileReader(filename));
		  String line;
		  int linecount=0;
			while ((line = file.readLine()) != null) {
					help1.append(line);
					help1.append("\n");
					linecount++;
				}
			helper=help1.toString();
			help = new String[linecount][3];
			int eol,e0,e1;
			int i,j;
			i=0;
			while(i<linecount){
				if (helper.length() <=0) break;
				eol = helper.indexOf("\n");
				j=eol-1;
				while(Character.isDigit(helper.charAt(j)) || helper.charAt(j)=='_'){
				j--;}
				e1=j;
				j--;
				while(Character.isDigit(helper.charAt(j)) || helper.charAt(j)=='.'){j--;}
				e0=j;
				//System.out.println("eol="+eol+" e1="+e1+" e0="+e0);
				if(e0<0 || e1<=e0+1 || eol<=e1+1){
					if (eol>=0) {
						helper=helper.substring(eol+1); 
						continue;
					}
				}
				help[i][0]=helper.substring(0,e0);
				help[i][1]=helper.substring(e0+1,e1);
				help[i][2]=helper.substring(e1+1,eol);
				helper=helper.substring(eol+1);
				i++;
			}
			linecount=i;
			if(linecount<1) return null;
			table = new String[linecount][3];
			for(i=0;i<linecount;i++){
				table[i][0]=help[i][0];
				table[i][1]=help[i][1];
				table[i][2]=help[i][2];
			}
		file.close();
		return table;
	  } catch (IOException e2) {
			e2.printStackTrace();
	  }
	  return null;
  }
  
  private double[] enterWeighting() {
	  double[] g = new double[3];
		//Erfrage Gewichtungen
		BufferedReader standardInput = launcher.openStandardInput();
		boolean accepted = false;
		String msg = "Bitte geben sie die Gewichtungen für die NGramme ein:";
		System.out.println(msg);
		for(int i = 0; i<g.length;i++) {
			accepted = false;
			do {
				System.out.print("Geben Sie die Gewichtung für " + (i+1) + "-Gramme ein: ");
				try {
					g[i] = (double)Integer.parseInt(standardInput.readLine());
					if (g[i] < 0 || g[i] >= 1000) {
						System.out.println(
								"Die Gewichtung muss eine natürliche Zahl zwischen 0 bis 1000 sein.");
					} else {
						msg = "Die Gewichtungen für "+(i+1)+"-Gramme lautet " + g[i] + ".";
						System.out.println(msg);
						accepted = true;
					}
				} catch (NumberFormatException e) {
					System.out.println("Fehler beim Parsen der Zahl. Bitte korrigieren"
							+ " Sie Ihre Eingabe.");
				} catch (IOException e) {
					System.err.println("Abbruch: Fehler beim Lesen von der Standardeingabe.");
					e.printStackTrace();
					System.exit(1);
				}
			} while (!accepted);
		}
	return g;
  }
}
