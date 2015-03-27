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
import java.util.*;
import com.google.gson.Gson;  
import com.google.gson.JsonArray;  
import com.google.gson.JsonElement;  
import com.google.gson.JsonObject;  
import com.google.gson.JsonParser; 

public class Main extends HttpServlet {
    private final String bucketName = "medidatasiyang";
    private final String key = "mockData.json";
	
	
  @Override
/*  Our Audit data is stored on Amazon S3. 
 *  doGet method will connect Ameazon S3, extract data from S3 and show in heroku website
*/  
  
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
	  
	  AWSCredentials credentials = null;
	  try{
		  credentials = new BasicAWSCredentials(System.getenv("S3_KEY"),System.getenv("S3_SECRET"));
	  }catch (Exception e){
		  throw new AmazonClientException(e);
	  }
	  
      AmazonS3 s3 = new AmazonS3Client(credentials);
      Region usWest2 = Region.getRegion(Regions.US_WEST_2);
      s3.setRegion(usWest2);
      InputStream input = null;
      for (Bucket bucket : s3.listBuckets()) {
          if (bucket.getName().equals(bucketName)){
              S3Object object = s3.getObject(new GetObjectRequest(bucketName, key));
              resp.getWriter().print("<br>");
              input = object.getObjectContent();
          }
      }
      
      if(input != null){
    	  BufferedReader reader = new BufferedReader(new InputStreamReader(input));
    	  StringBuilder json = new StringBuilder();
    	  while (true) {
    		  String line = reader.readLine();
    		  if (line == null) break;
    		  json.append(line);
    		  //resp.getWriter().print("    " + line + "<br>");
    	  }
    	  //resp.getWriter().print(json.toString());
          ArrayList<ArrayList<String>> result = processJson(json.toString());
          //resp.getWriter().print(result);
          ArrayList<Entry> heartRate = CalculateAverage.calculateAverage(result, 0, 3, 15);
          resp.getWriter().print(heartRate);
      }      
  }
  
  /*Gson is a Java library that can be used to convert Java Objects into their JSON representation. 
  It can also be used to convert a JSON string to an equivalent Java object. 
  Gson can work with arbitrary Java objects including pre-existing objects that you do not have source-code of.
  This method will convert json to a ArrayList
  */
  public ArrayList<ArrayList<String>> processJson(String json){
	  ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
	  Gson gson = new Gson();  
      JsonParser parser = new JsonParser();  
      JsonArray jsonArray = parser.parse(json).getAsJsonArray();
      for(int i = 0 ; i < jsonArray.size(); i++){
    	  JsonObject singleObject = jsonArray.get(i).getAsJsonObject();
    	  JsonObject insideWhichChanged = singleObject.getAsJsonObject("which_changed");
    	  JsonArray arrayInsideChanges = insideWhichChanged.getAsJsonArray("changes");
    	  
    	  //get all the field name into result
		  if(result.isEmpty() && arrayInsideChanges.size() > 0){
    		  ArrayList<String> fieldname = new ArrayList<String>();
			  for(int j = 0 ; j < arrayInsideChanges.size(); j ++){	  
				  JsonObject objInsideChanges = arrayInsideChanges.get(j).getAsJsonObject();
				  JsonElement objFieldName = objInsideChanges.get("field");
				  fieldname.add(objFieldName.getAsString());
	    	  }
			  result.add(new ArrayList(fieldname));
		  }		  
		  ArrayList<String> seq = new ArrayList<String>();    	  
    	  for(int k = 0 ; k < arrayInsideChanges.size(); k ++){
    		  JsonObject objInsideChanges = arrayInsideChanges.get(k).getAsJsonObject();
    		  JsonElement objNum = objInsideChanges.get("new");
    		  seq.add(objNum.getAsString());
    	   }
		   result.add(new ArrayList(seq));
		   seq = new ArrayList<String>();
	  }
      return result;
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
}
