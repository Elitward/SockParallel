package net.sockparallel;

import java.io.IOException;
import java.net.Socket;

public class SockOut implements Sock{
	private String host = null;
	private int port = 0;
	private Socket client = null;
	
	SockOut(String host, int port){
		this.host = host;
		this.port = port;
	}
	
	@Override
	public Socket getSocket() {
		try {
			System.out.println("SockOut: connecting to " + host + ":" + port);
			client = new Socket(host, port);
			System.out.println("SockOut: connected a socket at " + client.getLocalPort() + "->" + client.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return client;
	}
}
