
import java.util.ArrayList;

public class CalculateAverage {
    
	 
	
	 public static ArrayList<Entry> calculateAverage(ArrayList<ArrayList<String>> incomingData,int dataIndex, int timeIndex, int n) {
		
	 ArrayList<Entry> average=new ArrayList<Entry>();
	 for(int i=1;i<incomingData.size();i=i+n){
		 double total=0;
		 for (int j=i;j<=n;j++){
		 total =total+Double.parseDouble(incomingData.get(j).get(dataIndex));		 
		 }		 
		 average.add(new entry(incomingData.get(i).get(timeIndex),Double.toString(total/n)));	
	 } 	
	 return average;
	}
}
