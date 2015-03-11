package testing;

import java.util.*;
import java.io.*;
import java.net.*;

// A Webserver that waits for clients to connect, then starts a separate
// thread to handle the request.

public class WebServer {
  private static ServerSocket serverSocket;

  public static void main(String[] args) throws IOException {
    serverSocket=new ServerSocket(7065);  // Start, listen on port 80
    while (true) {
      try {
        Socket s=serverSocket.accept();  // Wait for a client to connect
        new ClientHandler(s);  // Handle the client in a separate thread
      }
      catch (Exception x) {
        System.out.println(x);
      }
    }
  }
}

// A ClientHandler reads an HTTP request and responds
class ClientHandler extends Thread {
  private Socket socket;  // The accepted socket 

  // Start the thread 
  public ClientHandler(Socket s) {
    socket=s;
    start();
  }

  // Read the HTTP request, respond, and close the connection
  public void run() {
    try {

      // Open connections to the socket
      BufferedReader in=new BufferedReader(new InputStreamReader(
        socket.getInputStream()));
      PrintStream out=new PrintStream(new BufferedOutputStream(
        socket.getOutputStream()));

      // Read filename from first input line "GET /filename.html ..."
      // or if not in this format, treat as a file not found.
      String s=in.readLine();
      System.out.println(s);  // Log the request

      // Attempt to serve the file.  Catch FileNotFoundException and
      // return an HTTP error "404 Not Found".  Treat invalid requests
      // the same way.
      String filename="";
      StringTokenizer st=new StringTokenizer(s);
      try {

        // Parse the filename from the GET command
        if (st.hasMoreElements() && st.nextToken().equalsIgnoreCase("GET")
            && st.hasMoreElements())
          filename=st.nextToken();
        else
          throw new FileNotFoundException();  // Bad request

        // Append trailing "/" with "index.html"
        if (filename.endsWith("/"))
          filename+="index.html";

        // Remove leading / from filename
        while (filename.indexOf("/")==0)
          filename=filename.substring(1);

        // Replace "/" with "\" in path for PC-based servers
        filename=filename.replace('/', File.separator.charAt(0));

        // Check for illegal characters to prevent access to superdirectories
        if (filename.indexOf("..")>=0 || filename.indexOf(':')>=0
            || filename.indexOf('|')>=0)
          throw new FileNotFoundException();

        // If a directory is requested and the trailing / is missing,
        // send the client an HTTP request to append it.  (This is
        // necessary for relative links to work correctly in the client).
        if (new File(filename).isDirectory()) {
          filename=filename.replace('\\', '/');
          out.print("HTTP/1.1 301 Moved Permanently\r\n"+
            "Location: /"+filename+"/\r\n\r\n");
          out.close();
          return;
        }

        // Open the file (may throw FileNotFoundException)
        InputStream f=new FileInputStream(filename);

        // Determine the MIME type and print HTTP header
        String mimeType="text/plain";
        if (filename.endsWith(".html") || filename.endsWith(".htm"))
          mimeType="text/html";
        else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg"))
          mimeType="image/jpeg";
        else if (filename.endsWith(".gif"))
          mimeType="image/gif";
        out.print("HTTP/1.0 200 OK\r\n"+
          "Content-type: "+mimeType+"\r\n\r\n");

        // Send file contents to client, then close the connection
        byte[] a=new byte[4096];
        int n;
        while ((n=f.read(a))>0)
          out.write(a, 0, n);
        out.close();
      }
      catch (FileNotFoundException x) {
        out.println("HTTP/1.1 Error 404! File Not Found\r\n"+
          "Content-type: text/html\r\n\r\n"+
          "<html><head></head><body>"+filename+" not found</body></html>\n");
        out.close();
      }
    }
    catch (IOException x) {
      System.out.println(x);
    }
  }
}

