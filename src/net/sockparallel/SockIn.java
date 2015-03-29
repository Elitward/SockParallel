package net.sockparallel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SockIn implements Sock{
	private ServerSocket listen = null;
	
	private Socket server = null;
	
	SockIn(int port){
		if(listen==null && port>0){
			try {
				System.out.println("SockIn: Listening at " + port);
				listen = new ServerSocket(port);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public Socket getLastSocket() {
		if(listen==null){
			return null;
		}else{
			if(server==null){
				try {
					System.out.println("SockIn: accepting a socket at " + listen.getLocalPort());
					server = listen.accept();
					System.out.println("SockIn: accepted a socket at " + server.getLocalPort() + "<-" + server.getPort());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return server;
		}
	}

	@Override
	public Socket getNewSocket() {
		Socket newSocket = null;
		try {
			newSocket = listen.accept();
			server = newSocket;
			System.out.println("SockIn: accepted a socket at " + server.getLocalPort() + "<-" + server.getPort());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return newSocket;
	}
}
