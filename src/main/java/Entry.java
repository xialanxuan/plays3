public class Entry{
	
private String time;
private String data;

public Entry(String newTime,String newData){
this.time = newTime;
this.data=newData;
}

public String toString(){
	return time+","+data;
}
}