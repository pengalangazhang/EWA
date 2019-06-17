import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class PC {
	private static final int PORT = 8000;
	
	public static void main(String[] args) {
		Scanner scan = new Scanner(System.in);
		String message = scan.next();
		try {
			Socket s = new Socket ("localhost", PORT);
			PrintWriter pw = new PrintWriter(s.getOutputStream());	
			pw.print(message);
			pw.flush();
			pw.close();
			s.close();
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}

}
