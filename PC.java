import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class PC {
	private static final int PORT = 8000;
	static Socket s;
	
	public static void main(String[] args) {
		try {
			Runtime.getRuntime().exec(("C:\\Users\\pzhang\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe forward tcp:8000 tcp:9000"));
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			s = new Socket("localhost", PORT);
			System.out.println("Socket Created");
			PrintWriter pw = new PrintWriter(s.getOutputStream());
			pw.print("Hey Server!\n");
			pw.flush();
			
			new Thread(readFromServer).start();
			Thread closeSocketOnShutdown = new Thread() {
				public void run() {
					try {
						s.close();
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
				in = new BufferedReader(new InputStreamReader(s.getInputStream()));
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
