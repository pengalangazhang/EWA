import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class ContinuousInput {
	public static final int PORT = 8000;
	static Socket client;
	
	public static void main(String[] args) {
		try {
			Runtime.getRuntime().exec(("C:\\Users\\pzhang\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe forward tcp:8000 tcp:9000"));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			client = new Socket("localhost", PORT);
			System.out.println("Socket Created");
			PrintWriter output = new PrintWriter(client.getOutputStream());
			Scanner scan = new Scanner(System.in);
			String message = "";
			while (!message.equals("STOP")) {
				output.println(message);
				output.flush();
				message = scan.next();
			}
			scan.close();
			
			new Thread(readFromServer).start();
			Thread closeSocketOnShutdown = new Thread() {
				public void run() {
					try {
						client.close();
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			Runtime.getRuntime().addShutdownHook(closeSocketOnShutdown);
		}
		catch (UnknownHostException e) {
			System.out.println("Socket connection problem (Unknown host)" + e.getStackTrace());
		}
		catch (IOException e) {
			System.out.println("Could not initialize I/O on socket" + e.getStackTrace());
		}
	}
	
	private static Runnable readFromServer = new Runnable() {

		@Override
		public void run() {
			BufferedReader in = null;
			try {
				System.out.println("Reading from Server");
				in = new BufferedReader(new InputStreamReader(client.getInputStream()));
				String buffer;
				while ((buffer = in.readLine()) != null) {
					System.out.println(buffer);
				}
			}
			catch (IOException e) {
				try {
					in.close();
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
		}
		
	};

}
