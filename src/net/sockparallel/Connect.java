package net.sockparallel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Connect {
	private final int BUFFER_SIZE = 128*1024;
	private Socket one;
	private ArrayList<Socket> many = new ArrayList<Socket>();
	
	//private final long TOMBSTONE = 0; //Long.MAX_VALUE;

	Connect(Sock single, ArrayList<Sock> group){
		if( single.getClass().getName().compareTo(new SockIn(0).getClass().getName())==0 ){		//single sock in in coming socket (listen)
			//first accept the single socket
			one = single.getNewSocket();
			for(int i=0; i<group.size(); i++){
				Socket s = group.get(i).getNewSocket();
				if(s!=null){
					many.add(s);
				}
			}
			System.out.println("1 -> " + many.size());
		}else{
			//first accept all sub socket
			for(int i=0; i<group.size(); i++){
				Socket s = group.get(i).getNewSocket();
				if(s!=null){
					many.add(s);
				}
			}
			one = single.getNewSocket();
			System.out.println(many.size() + " -> 1");
		}
		
		if(many.size() == group.size() && one!=null){
			ThreadSplit	single2group = new ThreadSplit();
			ThreadMix	group2single = new ThreadMix();
			
			single2group.setTwin(group2single);
			group2single.setTwin(single2group);
			
			single2group.start();
			group2single.start();
			
			try {
				single2group.join();
				group2single.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		if(one!=null){
			try {
				one.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for(Socket temp : many){
			try {
				temp.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	class Buffer {
		private int len;
		private int off;
		private int capacity;
		private byte[] buf;
		
		final private int BUFFER_UNIT = 64*1024;
		
		public Buffer(){
			len = 0;
			off = 0;
			capacity = 0;
			buf = null;
		}
		
		public int append(byte[] newBuf, int newLen){
			if(newLen>(capacity-off-len)){
				//not enough space to append
				int newCap = ( ((newLen+len)/BUFFER_UNIT) + (((newLen+len)%BUFFER_UNIT==0)?0:1) )*BUFFER_UNIT;
				byte[] newCache = new byte[newCap];
				if(len>0){
					System.arraycopy(buf, off, newCache, 0, len);
				}
				off = 0;
				System.arraycopy(newBuf, 0, newCache, len, newLen);
				len += newLen;
				capacity = newCap;
				buf = newCache;
				return len;
			}else{
				//just append the current buff tail
				System.arraycopy(newBuf, 0, buf, off+len, newLen);
				len += newLen;
				return len;
			}
		}
		
		public int append(byte b){
			if(capacity-off-len>0){
				//just append the current buff tail
				buf[off+len] = b;
				len++;
				return len;				
			}else{
				byte[] temp = {b};
				return append(temp, 1);
			}
		}
		
		public byte fetch(){
			if(len>0){
				len--;
				return buf[off++];
			}else{
				return -1;
			}
		}
		
		public int getLength(){
			return len;
		}
		
		public int getOffset(){
			return off;
		}
		
		public byte[] getBuffer(){
			return buf;
		}

		public void markConsume(int length){
			if(len>=length){
				len-=length;
				off+=length;
			}
		}
		
	}
	
	class ThreadMix extends Thread{
		private ThreadMix that;
		private boolean ioRunning;
		private ThreadSplit twin;
		
		class ThreadRead extends Thread{
			private int mId;
			private InputStream mRead;
			private Buffer mWrite;
			private long isCnt;
			
			ThreadRead(int id, InputStream readFrom, Buffer writeTo){
				mId = id;
				mRead = readFrom;
				mWrite = writeTo;
				isCnt = 0;
			}

			@Override
			public void run() {
				ioRunning = true;
				while(ioRunning){
					byte[] buffer = new byte[BUFFER_SIZE];
					int len = -1;
					try {
						len = mRead.read(buffer, 0, BUFFER_SIZE);
						System.out.println(" MIX >: read@" + mId + "[c=" + isCnt + "|l=" + len + "] : " + getHexMain(buffer, 0, len) );
						isCnt += len;
					} catch (IOException e) {
						e.printStackTrace();
					}
					if(len<0){
						ioRunning = false;
						break;
					}

					if(len>0){
						synchronized (mWrite){
							mWrite.append(buffer, len);
						}
						synchronized(that){
							that.notify();
						}
					}
				}
				System.out.println(" MIX >: read@" + mId + " IO broken");
			}
			
		}

		@Override
		public void run() {
			that = this;
			InputStream[] is = new InputStream[many.size()];
			OutputStream os = null;
			
			Buffer   oBuf = new Buffer();
			Buffer[] iBuf = new Buffer[many.size()];
			for(int i=0; i<iBuf.length; i++){
				iBuf[i] = new Buffer();
			}
			
			long	osCnt = 0;
			/*long[]	isCnt = new long[many.size()];
			for(int i=0; i<isCnt.length; i++){
				isCnt[i] = 0;
			}*/
			
			ThreadRead[] tRead = new ThreadRead[many.size()];
			
			try {
				for(int i=0; i<many.size(); i++){
					is[i] = many.get(i).getInputStream();
				}
				os = one.getOutputStream();

				for(int i=0; i<many.size(); i++){
					tRead[i] = new ThreadRead(i, is[i], iBuf[i]);
					tRead[i].start();
					//System.out.println("MIX:ThreadRead[" + i + "] started");
				}
				
				ioRunning = true;

				int cur = 0;
				
				while(ioRunning && !Thread.currentThread().isInterrupted()){
					while(true){
						synchronized(iBuf[cur]){
							if(iBuf[cur].getLength()>0){
								oBuf.append(iBuf[cur].fetch());
								cur++;
								if(cur>=many.size())
									cur = 0;
							}else{
								break;
							}							
						}
					}
						
					if( oBuf.getLength()>0 ){
						byte[] buff = oBuf.getBuffer();
						int offs = oBuf.getOffset();
						int leng = oBuf.getLength();
						System.out.println(" MIX >: write[c=" + osCnt + "|l=" + leng + "|o=" + offs + "] : " + getHexMain(buff, offs, leng) );
						osCnt+=leng;
						os.write(buff, offs, leng);
						oBuf.markConsume(leng);
						os.flush();
					}else{
						synchronized(this){
							try {
								wait();
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
				
				for(int i=0; i<many.size(); i++){
					try {
						tRead[i].join();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
				/*if(TOMBSTONE!=0){
					System.err.println("========== TOMBSTONE ==========" + System.currentTimeMillis());
					try {
						Thread.sleep(TOMBSTONE);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}*/
			} finally {
				System.out.println("Final in ThreadMix");
				for(int i=0; i<is.length; i++){
					try {
						is[i].close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if(os!=null){
					try {
						os.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if(twin!=null){
					twin.interrupt();
					synchronized(twin){
						twin.notify();
					}
				}
			}
		}
		
		void setTwin(ThreadSplit thread){
			twin = thread;
		}

	}

	class ThreadSplit extends Thread{
		private ThreadMix twin;

		@Override
		public void run() {
			InputStream is = null;
			OutputStream[] os = new OutputStream[many.size()];
			
			Buffer[] oBuf = new Buffer[many.size()];
			for(int i=0; i<oBuf.length; i++){
				oBuf[i] = new Buffer();
			}

			long	isCnt = 0;
			long[]	osCnt = new long[many.size()];
			for(int i=0; i<osCnt.length; i++){
				osCnt[i] = 0;
			}

			try {
				is = one.getInputStream();
				
				for(int i=0; i<many.size(); i++){
					OutputStream tmp_os= many.get(i).getOutputStream();
					os[i] = tmp_os;
				}

				int cur = 0;
				boolean connectionOK = true;
				while(connectionOK && !Thread.currentThread().isInterrupted()){
					byte[] buffer = new byte[BUFFER_SIZE];
					int len = is.read(buffer, 0, BUFFER_SIZE);
					System.out.println("Split<: read" + "[c=" + isCnt + "|l=" + len + "] : " + getHexMain(buffer, 0, len) );
					isCnt+=len;
					
					if(len>0){
						for(int i=0;i<len; i++){
							oBuf[cur].append(buffer[i]);
							cur++;
							if(cur>=many.size())
								cur = 0;
						}
						for(int i=0; i<many.size(); i++){
							if(oBuf[i].getLength()>0){
								byte[] buff = oBuf[i].getBuffer();
								int offs = oBuf[i].getOffset();
								int leng = oBuf[i].getLength();
								System.out.println("Split<: write@" + i + "[c=" + osCnt[i] + "|l=" + leng + "|o=" + offs + "] : " + getHexMain(buff, offs, leng) );
								osCnt[i]+=leng;
								os[i].write(buff, offs, leng);
								oBuf[i].markConsume(oBuf[i].getLength());
								os[i].flush();
							}
						}
					}else{
						connectionOK = false;
						break;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				/*if(TOMBSTONE!=0){
					System.err.println("========== TOMBSTONE ==========" + System.currentTimeMillis());
					try {
						Thread.sleep(TOMBSTONE);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}*/
			} finally {
				System.out.println("Final in ThreadSplit");
				if(is!=null){
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				for(int i=0; i<os.length; i++){
					try {
						os[i].close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				
				if(twin!=null){
					twin.interrupt();
					synchronized(twin){
						twin.notify();
					}
				}
			}
		}

		void setTwin(ThreadMix thread){
			twin = thread;
		}
	}
	
	String getHexMain(byte[] buf, int off, int len){
		if(len>3){
			return "[" + buf[off] + " " + buf[off+1]  + " " + buf[off+2] + "..." + buf[off+len-1] + "]";
		}else{
			String txt = "[";
			for(int i=0; i<len; i++){
				txt += buf[i] + " ";
			}
			txt += "]";
			return txt;
		}
	}
}
