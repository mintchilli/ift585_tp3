package ift585_tp3;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class main {

	public static void main(String[] args) throws IOException {
		System.out.println("entrez votre adresse web (xxxxxxx.zz)");
		Scanner scanner = new Scanner(System.in);
		String url = scanner.nextLine();

		byte[] header = new byte[12];
		// set all the bits of the header to 0;
		for (int i = 0; i < header.length; i++) {
			header[i] = 0;
		}

		// set the id -> here it will be FEFE just b/c
		header[0] = (byte) 254;
		header[1] = (byte) 254;
		
		// set the query bit
		header[2] = 1;
		// set the recursive bit
		header[5] = 1;

		
		//compute our dns question
		String[] labels = url.split("\\.");
		
		int totalLenght = 0;
		for (String label : labels) {
			totalLenght += label.length();
		}
		totalLenght += labels.length;
		//for the null byte, the QTYPE and the QCLASS
		totalLenght+=5;
		
		//build the question
		byte[] question = new byte[totalLenght];
		int index = 0;
		for (String label : labels) {
			question[index] = (byte) label.length();
			index++;
			for (char c : label.toCharArray()) {
				question[index] = (byte) c;
				index++;
			}
		}
		question[index] = 0;
		index++;
		question[index] = 0;
		index++;
		question[index] = 1;
		index++;
		question[index] = 0;
		index++;
		question[index] = 1;
		
		byte[] finalQuestion = new byte[question.length + header.length];
		//concatenate arrays
		for (int i = 0; i < header.length; i++) {
			finalQuestion[i] = header[i];
		}
		for (int i = 0; i < question.length; i++) {
			finalQuestion[i + 12] = question[i];
		}
		
		
		//prepare the net stuff
		DatagramSocket ds = new DatagramSocket();
		
		//use google's dns service
		byte[] ipAddr = new byte[]{8, 8, 8, 8};
		InetAddress addr = InetAddress.getByAddress(ipAddr);
		
        DatagramPacket packet = new DatagramPacket(finalQuestion, finalQuestion.length, addr, 53);
        
        //send packet
        ds.send(packet);
        
        byte[] buf = new byte[finalQuestion.length + 16];
        packet = new DatagramPacket(buf, buf.length);
        
        //recieve packet
        ds.receive(packet);
        
        //parse the last bits of the response to get ip address
        ipAddr[0] = buf[buf.length - 4];
        ipAddr[1] = buf[buf.length - 3];
        ipAddr[2] = buf[buf.length - 2];
        ipAddr[3] = buf[buf.length - 1];
        
        String ipAdress = "";
        for (int i = 0; i < ipAddr.length; i++) {
			ipAdress += ipAddr[i] & 0xFF;
			ipAdress += ".";
		}
        
        //build the get request for the main page
        ipAdress = ipAdress.substring(0, ipAdress.length() - 1);
        addr = InetAddress.getByAddress(ipAddr);
        System.out.println(ipAdress);
        String Query = "GET / HTTP/1.1\r\nHost: " + url + "\r\nConnection: keep-alive\r\nAccept: text/html"+ "\r\n\r\n";
        
        //create the tcp socket
        Socket clientSocket = new Socket(ipAdress, 80);
        
        DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        //send the data
        outToServer.writeBytes(Query);
        
        String line = "";
        String html = "";
        System.out.println("---------------HEADER---------------");
        boolean body = false;
        //read the data
        while ((line = in.readLine()) != null) {
        	if(line.isEmpty() && !body) {
        		System.out.println("\r\n---------------BODY---------------");
        		body = true;
        	}
        	if(!line.isEmpty() && body) {
        		html += line;
        	}
            System.out.println(line);
        }
        clientSocket.close();
        
        //parse the html file for img tags
        Document doc = Jsoup.parse(html);
        Elements img = doc.getElementsByTag("img");
        
        //prepare the net stuff for the image
        Socket imageSocket = new Socket(ipAdress, 80);
        DataOutputStream imgOut = new DataOutputStream(imageSocket.getOutputStream());
        BufferedReader imgIn = new BufferedReader(new InputStreamReader(imageSocket.getInputStream()));
        
        if (img.size() > 0) {
        	System.out.println("\r\n---------------IMAGE HEADER---------------");
			for (Element element : img) {
				boolean imgBody = false;
				//send the query
				Query = "GET /" +element.attr("src") +" HTTP/1.1\r\nHost: " + url + "\r\nConnection: keep-alive\r\nAccept: text/html, image/tiff, image/gif, image/jpeg"+ "\r\n\r\n";
				imgOut.writeBytes(Query);
				
				//read the response
		        while ((line = imgIn.readLine()) != null) {
		        	if(line.isEmpty() && !imgBody) {
		        		System.out.println("\r\n---------------IMAGE DATA---------------");
		        		System.out.println("\r\n a shietload of data");
		        		break;
		        	}
		            System.out.println(line);
		        }
			}
		}
        imageSocket.close();

        
	}

}
