package net.sockparallel;

import java.net.Socket;

public interface Sock {
	Socket getLastSocket();
	Socket getNewSocket();
}
