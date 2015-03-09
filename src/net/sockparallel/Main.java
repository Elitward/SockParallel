package net.sockparallel;

import java.util.ArrayList;

public class Main {
	
	final static String MODE_MIX   = "MIX";
	final static String MODE_SPLIT = "SPLIT";
	
	final static String DEFAULT_HOST = null;	//the loopback interface. 

	public static void main(String[] args) {
		if(args.length<3){
			printHelp();
			return;
		}
		
		Sock single = null;
		ArrayList<Sock> group = new ArrayList<Sock>();
		
		if(args[0].compareToIgnoreCase(MODE_MIX)==0){
			//mix data from 101 102 103 to 3128
			System.out.print("Mix data from ");
			for(int i=1; i<args.length-1; i++){
				int num = Integer.parseInt(args[i]);
				System.out.print("" + num + ",");
			}
			System.out.print(" to " + args[args.length-1]);
			System.out.println("...");

			for(int i=1; i<args.length-1; i++){
				int num = Integer.parseInt(args[i]);
				SockIn sin = new SockIn(num);
				group.add(sin);
			}

			single = new SockOut(getHostFromPara(args[args.length-1]), getPortFromPara(args[args.length-1]));
			
			/*while(true)*/{
				new Connect(single, group);
			}
		}else if(args[0].compareToIgnoreCase(MODE_SPLIT)==0){
			//split data from 8000 to 101 102 103
			System.out.print("Split data from " + args[1] + " to ");
			for(int i=2; i<args.length; i++){
				System.out.print(getHostFromPara(args[i]) + ":" + getPortFromPara(args[i]) + ", ");
			}
			System.out.println("...");

			for(int i=2; i<args.length; i++){
				SockOut sout = new SockOut(getHostFromPara(args[i]), getPortFromPara(args[i]));
				group.add(sout);
			}
			single = new SockIn(Integer.parseInt(args[1]));

			/*while(true)*/{
				new Connect(single, group);
			}
		}else{
			System.out.println("Mode ERROR:" + args[0]);
			printHelp();
		}
	}
	
	private static void printHelp(){
		System.out.println("Usage:");
		System.out.println("SockParallel mix port1 port2 port3 outgoingport");
		System.out.println("\te.g.\t SockParallel mix 101 102 103 3128");
		System.out.println("SockParallel split incomingport port1 port2 port3");
		System.out.println("\te.g.\t SockParallel split 8000 101 102 103");
	}
	
	private static String getHostFromPara(String para){
		int colon = para.indexOf(":");
		if(colon==-1){
			return null;
		}else{
			return para.substring(0, colon);
		}
	}

	private static int getPortFromPara(String para){
		int colon = para.indexOf(":");
		if(colon==-1){
			return Integer.parseInt(para);
		}else{
			return Integer.parseInt(para.substring(colon+1));
		}
	}
}
