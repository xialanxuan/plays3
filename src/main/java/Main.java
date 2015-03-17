import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;
import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.auth.BasicAWSCredentials;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;

public class Main extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    if (req.getRequestURI().endsWith("/db")) {
      showDatabase(req,resp);
    } else {
      showHome(req,resp);
    }
  }

  
  private void showHome(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

	  AWSCredentials credentials = null;
	  try{
		  credentials = new BasicAWSCredentials(System.getenv("S3_KEY"),System.getenv("S3_SECRET"));

	  }catch (Exception e){
		  throw new AmazonClientException(
    
                  e);
	  }
	  
      String bucketName = "medidatasiyang";
      String key = "sandbox_audits_sample.json";

/*	  AWSCredentials credentials = null;
           try {
          credentials = new ProfileCredentialsProvider("default").getCredentials();
      } catch (Exception e) {
          throw new AmazonClientException(
                  "Cannot load the credentials from the credential profiles file. " +
                  "Please make sure that your credentials file is at the correct " +
                  "location (/Users/siyang/.aws/credentials), and is in valid format.",
                  e);
      }  */
      AmazonS3 s3 = new AmazonS3Client(credentials);
      Region usWest2 = Region.getRegion(Regions.US_WEST_2);
      s3.setRegion(usWest2);
      InputStream input = null;
      for (Bucket bucket : s3.listBuckets()) {
          //resp.getWriter().print(" - " + bucket.getName());
          if (bucket.getName().equals(bucketName)){
        	  //resp.getWriter().print("Downloading an object");
              S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
              //resp.getWriter().print("Content-Type: "  + object.getObjectMetadata().getContentType());
              resp.getWriter().print("<br>");
              input= object.getObjectContent();
          }
      }
      
      
      
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      while (true) {
          String line = reader.readLine();
          if (line == null) break;

          resp.getWriter().print("    " + line + "<br>");
      }
      resp.getWriter().print("<br>");
      
	  
    //resp.getWriter().print("Hello from Java! ?????????");
  }

  private void showDatabase(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
	  
	  
	  
	  
    Connection connection = null;
    try {
      connection = getConnection();

      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      String out = "Hello!\n";
      while (rs.next()) {
          out += "Read from DB: " + rs.getTimestamp("tick") + "\n";
      }

      resp.getWriter().print(out);
    } catch (Exception e) {
      resp.getWriter().print("There was an error: " + e.getMessage());
    } finally {
      if (connection != null) try{connection.close();} catch(SQLException e){}
    }
  }

  
  private Connection getConnection() throws URISyntaxException, SQLException {
    URI dbUri = new URI(System.getenv("DATABASE_URL"));

    String username = dbUri.getUserInfo().split(":")[0];
    String password = dbUri.getUserInfo().split(":")[1];
    int port = dbUri.getPort();

    String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ":" + port + dbUri.getPath();

    return DriverManager.getConnection(dbUrl, username, password);
  }

  public static void main(String[] args) throws Exception {
    Server server = new Server(Integer.valueOf(System.getenv("PORT")));
    ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
    context.setContextPath("/");
    server.setHandler(context);
    context.addServlet(new ServletHolder(new Main()),"/*");
    server.start();
    server.join();
  }
  
  private static void displayTextInputStream(InputStream input) throws IOException {
      BufferedReader reader = new BufferedReader(new InputStreamReader(input));
      while (true) {
          String line = reader.readLine();
          if (line == null) break;

          System.out.println("    " + line);
      }
      System.out.println();
  }
}
