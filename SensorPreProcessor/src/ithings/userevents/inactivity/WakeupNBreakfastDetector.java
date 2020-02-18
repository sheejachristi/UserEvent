package ithings.userevents.inactivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
//import java.sql.Date;
import java.sql.PreparedStatement;
//todo bmjo
//UserEvent detection classes to provide a single interface for the event processor host 
//It is also recommended to provide a UI interface which can be used by UI renderer to display
//event current settings and to allow users to change the settings
//Weekly delivery with tight schedule not giving me luxury to spend time on 
//such things. 
//Functionality is my first priority and this may be addressed in a second iteration
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import ithings.common.CommonDataArea;
import ithings.common.CommonUtilities;
import ithings.workflow.WorkflowBasic;

import org.json.JSONException;
import org.json.JSONObject;
public class WakeupNBreakfastDetector {
	private Connection con=null;
	String eventKey ;
	public  boolean unprocessed_data_available=true;
	public  String detectEvent = "WakeUp";
	public Connection getCon() {
		return con;
	}

	public boolean isConnected() {
		try {
		if(this.con != null&&this.con.isValid(60000)) return true;
		else return false;
		}catch(Exception exp) {
			return false;
		}
	}
	public void setCon(Connection con) {
		this.con = con;
	}
	
	public boolean run() {
		//String topic ="/topics/ihealthcare";
		//String mesg = "Starting Inactivity detector";
		//String mesg = "Subscriber -> Jevel Soucy  is found Inactive for last half and hour";
		//sendUserEventNotification("Inactivity Alert", "b3128776-3fb3-4fa2-a217-3af986a6e0e3","Breakfast Alert Mili", "milidaniels@mailinator.com",false,CommonDataArea.EVENT_STATUS_DETECTED);
		//sendUserEventNotification("Inactivity Alert","496c11ac-018f-4f68-8e6d-fe45fd99e19f", "Wakeup  Alert Mili", "milidaniels@mailinator.com",false,CommonDataArea.EVENT_STATUS_DETECTED);
		/*sendUserEventNotification("Inactivity Alert", "Inactivity Alert Milind", "milidaniels@mailinator.com");
		sendUserEventNotification("Inactivity Alert", "Inactivity Alert Sam", "samuelharper@mailinator.com");*/
		
		//CommonUtilities.sendPushNotification(topic, "Inactivity Alert", mesg);
		//CommonUtilities.sendPushNotification(topic, "InActivityAlert", mesg);
		insertEventAction("ff43a8e8-4173-429a-b1bf-8d66f86bdc39");
		return processConfiguredWakeupEventDefs();
	}

	//Check in the database for inactivity Event Definitions
	//configured and process it
	//Note :- Event detection window time to be configured in main 
	//table , which is missing now.
	boolean processConfiguredWakeupEventDefs() {
		try {
		
		SimpleDateFormat format = new SimpleDateFormat("HH:mm aa");
		SimpleDateFormat dateFrmat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat dateFull = new SimpleDateFormat("yyyy-MM-dd:HH:mm aa");
				
	
		long eventTime=0;
		long lastProcessedTime = 0;
		String alertGenOption="NA";
		Statement stmt=con.createStatement(); 
		String query = "select *  from " +CommonDataArea.table_TeleHealthcareFlow__SubscriberEventDef + " where category like '%" +detectEvent +"%' and ___smart_state___  like 'active' LIMIT 0 , 30";
		ResultSet list=stmt.executeQuery(query);
		String subscriber ="";
		String eventName = "";
		CommonUtilities.sendLogToStdout("*********************START" +detectEvent +" CHECK ITERATION************************","Version ->"+CommonDataArea.version);
          System.out.println(query);
		boolean passed = true;
		unprocessed_data_available=false;
		while(list.next())  {
			
			
			
				lastProcessedTime = list.getLong("lastProcessedTime");
				System.out.println(list.getLong("lastProcessedTime")+"---"+list.getString("name"));
			if((lastProcessedTime==0)||(lastProcessedTime<CommonDataArea.START_OF_2019)) {
				lastProcessedTime = System.currentTimeMillis() - 5 * 60 * 1000;
				updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
			}
			if((lastProcessedTime+60000)>System.currentTimeMillis()) {
				//unprocessed_data_available=false;
				continue;
			}
			unprocessed_data_available=true;
			Date curTimeDt = new Date();
			curTimeDt.setTime(lastProcessedTime);
			int curHourMin =  (curTimeDt.getHours())*60+ curTimeDt.getMinutes();
			subscriber = list.getString("subscriber");
			eventName = list.getString("name");
			CommonUtilities.sendLogToStdout("@@@@@@@@Processing Time ->",dateFull.format(curTimeDt));
			
			String query2 = "select *  from " +CommonDataArea.table_SubscriberEventDetail + " where belongsTo like '"+ eventName + "' and  subscriber like '"+subscriber+"'";
			Statement stmt2 = con.createStatement(); 
			ResultSet eventSensors =stmt2.executeQuery(query2);
			  System.out.println(query2);
			CommonUtilities.sendLogToStdout("Checking Event set for current time","Event found for Subscriber ->"+subscriber +" Event Name "+ eventName);
			int recsFound=0;
			int startHourMin=0;
			int endHourMin=0;
			//************************************************************************************/
			//Check any of the sensor triggered during the period to detect 
			//wakeup event. In the current implementation if any one of sensor triggered may take
			//as a wake up event
			//***********************************************************************************/
			while(eventSensors.next())  {
				//Take the period set for event detection
				
				String timeStr = eventSensors.getString("betweenTime");
				int indexStr = timeStr.indexOf(":::");
				timeStr =timeStr.substring(indexStr+3);
				String sensorTag = eventSensors.getString("tag");
				String starTimeStr ="";
				String endTimeStr ="";
				long startTime=0;// = Long.parseLong(starTimeStr);
				long endTime=0;;// = Long.parseLong(endTimeStr);
				Date startTimeDt;
				Date endTimeDt;
				
				alertGenOption = eventSensors.getString("generateEvent");
				System.out.println(alertGenOption);
				if(alertGenOption==null)
					alertGenOption="2";
				recsFound++;
				try {
					JSONObject tomJsonObject = new JSONObject(timeStr);
					startTime = tomJsonObject.getLong("startTime");
					endTime =  tomJsonObject.getLong("endTime");
					
					startTimeDt = new Date();
					startTimeDt.setTime(startTime);
					starTimeStr = format.format(startTimeDt);
					startHourMin = (startTimeDt.getHours())*60+ startTimeDt.getMinutes();
					
					endTimeDt= new Date(endTime);
					endTimeDt.setTime(endTime);
					endTimeStr = format.format(endTimeDt);
					endHourMin =  (endTimeDt.getHours())*60+ endTimeDt.getMinutes();
					if(endHourMin<=startHourMin) {
						passed = false;
						break;
					}
					CommonUtilities.sendLogToStdout("Event Time","Time set for  Event->StartTime="+starTimeStr+" EndTime="+endTimeStr);
				} catch (JSONException e) {
					CommonUtilities.sendLogToStdout("Error","Exception Parsing  time string"+e.getMessage());
				}  
				
			
				//Check whether sensor triggered during the period
				if((curHourMin > startHourMin)&&(curHourMin < endHourMin)) {
					
					CommonUtilities.sendLogToStdout("Event Details","Event Monitoring Set for curTime for sensor ->"+sensorTag);
					//String query3 = "select timestamp from sptest_TelehealthcareFlow__SensorStatus where tag like '"+ sensorTag + "%' and subscriber like '"+ subscriber 
					//		+"%' and sensorStatus like 'active' and date like '%"+ curDateStr +"%' and minute BETWEEN "+startHourMin +" AND "+ endHourMin+ " order by Id ASC Limit 1";
					//Adjust start hour min sensor trigerday's starthour
					long curStartHourMinMills = adjustHourMinToDay(lastProcessedTime, curHourMin);
					long curEndHourMinMills = adjustHourMinToDay(lastProcessedTime, curHourMin+CommonDataArea.idleTime/60000);
									
					String query3 ="select timeStamp from sptest_TeleHealthcareFlow__SensorTrigHistory where tag like '"+ sensorTag + "%' and subscriber like '"+ subscriber +"%' and timeStamp > "+curStartHourMinMills 
							+" and  timeStamp <" + curEndHourMinMills +" order by Id ASC Limit 1";
					System.out.println(query3);
					Statement stmt3 =con.createStatement();
					ResultSet activityCount  =stmt3.executeQuery(query3);
					
					if(activityCount.next()){ //Record found
							passed=true;
							CommonUtilities.sendLogToStdout("Checking for Event","Sensor ->"+sensorTag+ "Activated ->active");
							//CommonUtilities.sendLogToStdout("Checking for Wakeup Event",query3);
							eventTime = activityCount.getLong(1);
						
					}else {
						CommonUtilities.sendLogToStdout("Checking for Event","Sensor ->"+sensorTag+ " Inactive");
						passed=false;
					}
				}else {
					CommonUtilities.sendLogToStdout("Event","NO Event Monitoring Set for curTime for sensor ->"+sensorTag);
					passed=false;
				}
				//One sensor triggered break and mark it as a wakeup event
				if(passed) break;
			}
			
			//if event conditions passed then insert in event table
			if(passed) {
				CommonUtilities.sendLogToStdout("Event Detected","Checking whether already reported");
				if(!checkSameEventExist(eventName,subscriber,startHourMin,endHourMin,lastProcessedTime))
				{
			    CommonUtilities.sendLogToStdout("Event Detected","Insering to DB");
				insertEvent(eventName,subscriber,lastProcessedTime);
				
				if((alertGenOption.contains(CommonDataArea.ALERTGEN_ONEVENT))||(alertGenOption.contains(CommonDataArea.ALERTGEN_ONBOTH))||(alertGenOption.contains(CommonDataArea.ALERTGEN_NA))) {
					CommonUtilities.sendLogToStdout("Event Detected","Sending ONEVENT firebase message");
				
				//sendUserEventNotification(detectEvent +" Event",eventKey,eventName,subscriber,true,CommonDataArea.EVENT_STATUS_DETECTED);
				//////////////////////////////////////////////////////////
				
					CommonUtilities.sendLogToStdout("eventKey","Detect Event");
					System.out.println(eventKey+"----event generate");
					 WorkflowBasic wf=new WorkflowBasic();
					  wf.setCon(con);
					   wf.assignEventWake(eventKey,subscriber,detectEvent +" Event",eventName,false,CommonDataArea.EVENT_STATUS_DETECTED);
				
				/////////////////////////////////////////////////////////////
				}
				//CommonUtilities.sendPushNotification(topic, "InActivityAlert", mesg);
				}
			}else {
				//Event Missed generated Missed Wakeup event
				Date curEndTimeDt= new Date();
				if(lastProcessedTime>getTodaysEndTimeMills(lastProcessedTime,endHourMin)) {
				CommonUtilities.sendLogToStdout("EventDetection","Event detction window over");
				int endHourMinCur =  (curEndTimeDt.getHours()+1)*60+ curEndTimeDt.getMinutes();
				if(!checkSameEventExist(eventName,subscriber,startHourMin,endHourMinCur,lastProcessedTime))
				{
					insertMissedEvent(eventName, subscriber,lastProcessedTime);
					if((alertGenOption.contains(CommonDataArea.ALERTGEN_ONMISSED))||(alertGenOption.contains(CommonDataArea.ALERTGEN_ONBOTH))||(alertGenOption.contains(CommonDataArea.ALERTGEN_NA))) {
						CommonUtilities.sendLogToStdout("Event Detected","Sending MISSED EVENT firebase message");
						//sendUserEventNotification(detectEvent +" Event Missed",eventKey,eventName,subscriber,false,CommonDataArea.EVENT_STATUS_MISSED);
						//////////////////////////////////////////////////////////
						
						CommonUtilities.sendLogToStdout("eventKey","Detect Missed Event");
						System.out.println(eventKey+"----missed event");
						WorkflowBasic wf=new WorkflowBasic();
						wf.setCon(con);
						wf.assignEventWake(eventKey,subscriber,detectEvent +" Event Missed",eventName,true,CommonDataArea.EVENT_STATUS_MISSED);

						/////////////////////////////////////////////////////////////
					
					
					
					}
				}
				
				}
			}
			
			CommonUtilities.sendLogToStdout("Event","####################################################");
			lastProcessedTime+=CommonDataArea.idleTime;
			updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
		}
		if(!unprocessed_data_available)
		con.close();
		return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} 
	}
	
	boolean sendUserEventNotification(String event,String eventKey, String evntName, String subscriber, boolean missed,String status) {
		 String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Subscriber + " where emailId like '"+subscriber+"%'";
		 try {
				PreparedStatement preparedStmt = con.prepareStatement(sql);
				ResultSet activityCount  =preparedStmt.executeQuery();
				
				while(activityCount.next()) {
					String name = activityCount.getString("name");
					String phone = activityCount.getString("phone");
					String sub_photo = activityCount.getString("sub_photo");
					
					String body ="";
					if(missed)
						body = "Subscriber ->"+ name + " A MISSED Wakeup event detected ";
					else 
						body = "Subscriber ->"+ name + " A Wakeup event detected ";
					//BMJO to do read IP from  a config file or table
					String imageUrl = sub_photo;
					
					CommonUtilities.sendPushNotification(event, body,evntName, eventKey,name, subscriber, phone, imageUrl,status);
					return true;
				}
				return false;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
	}
	long getStartofDayMills() {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis =c.getTimeInMillis();
		return millis;
	}
	
	long getStartofDayMills(long millsOfDay,int startTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis =c.getTimeInMillis()+(long)startTimeHourMins*60l*1000l;;
		return millis;
	}
	

	long getStartTimeMin(long millsOfDay) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis =c.getTimeInMillis();
		return millis;
	}
	
	long getTodaysEndTimeMills(int endTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis =c.getTimeInMillis()+(long)endTimeHourMins*60l*1000l;
		return millis;
	}
	
	long getTodaysEndTimeMills(long millsOfDay,int endTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis =c.getTimeInMillis()+(long)endTimeHourMins*60l*1000l;
		return millis;
	}
	
	long adjustHourMinToDay(long millsOfDay,int endTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		System.out.println(c.getTimeInMillis());
		long millis =c.getTimeInMillis()+(long)endTimeHourMins*60l*1000l;
		return millis;
	}
	//If same event with posted for subscriber in last haf hour
	boolean checkSameEventExist(String eventName, String subscriber, int startTime , int endTime, long eventTime ) {
		
		 long startTimeMills = adjustHourMinToDay(eventTime,startTime);
		 long endTimeMillis =  eventTime+CommonDataArea.idleTime;
		 String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Event + " where subscriber like '"+ subscriber+ "'  and time > "+startTimeMills +" and time <= "+ endTimeMillis + " and eventName like '" +eventName + "' limit 1";
		 System.out.println(sql);
		 try {
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			ResultSet activityCount  =preparedStmt.executeQuery();
			if(activityCount.next()) {
				CommonUtilities.sendLogToStdout("Event Detected","Already reported event ->"+ eventName +" for -> "+subscriber);
				return true;
			}
			else
			{
			CommonUtilities.sendLogToStdout("Event Detected","Not Reported this event ->"+ eventName +" for -> "+subscriber);

			return false;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	boolean checkSameMissedEventExist(String eventName, String subscriber, int startTime , int endTime, long eventTime ) {
		
		 long startTimeMills = adjustHourMinToDay(eventTime,startTime);
		 System.out.println(startTimeMills+"----");
		 long endTimeMillis =  eventTime+CommonDataArea.idleTime;
		 String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Event + " where subscriber like '"+ subscriber+ "'  and time > "+startTimeMills +" and time <= "+ endTimeMillis + " and status ='missed' and eventName like '" +eventName + "' limit 1";
		 System.out.println(sql);
		 try {
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			ResultSet activityCount  =preparedStmt.executeQuery();
			if(activityCount.next()) {
				CommonUtilities.sendLogToStdout("Event Detected","Already reported event ->"+ eventName +" for -> "+subscriber);
				return true;
			}
			else
			{
			CommonUtilities.sendLogToStdout("Event Detected","Not Reported this event ->"+ eventName +" for -> "+subscriber);

			return false;
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
	
	
	
	boolean insertMissedEvent(String eventName, String subscriber,long eventTime) {
		try {
		 eventKey = UUID.randomUUID().toString();
		 String sql = "INSERT INTO "+ CommonDataArea.table_TeleHealthcareFlow__Event + " (eventKey,eventObject,eventName,time, subscriber,timeStr,status) values (?,?,?,?,?,?,'missed')";
		 DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Date today =  Calendar.getInstance().getTime();  
		 today.setTime(eventTime);
		 String dateStr = df.format(today);
		 PreparedStatement preparedStmt = con.prepareStatement(sql);
		 preparedStmt.setString(1, eventKey);
		 preparedStmt.setString(2, this.getClass().getName());
		 preparedStmt.setString(3,eventName);
		 preparedStmt.setLong(4, eventTime);
		 preparedStmt.setString(5,subscriber);
		 preparedStmt.setString(6,dateStr);
		 preparedStmt.execute();
		 insertEventAction(eventKey);
		 return true;
		}
		catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
		} 
	}
	
	boolean updateLatUpdatedTime(long lastUpdatedTime, String subscriber, String eventName) {
		 try
		 {
		  String query = "update "+ CommonDataArea.table_TeleHealthcareFlow__SubscriberEventDef +" set lastProcessedTime =? where name = ?";
	      PreparedStatement preparedStmt = con.prepareStatement(query);
	      preparedStmt.setLong  (1, lastUpdatedTime);
	      preparedStmt.setString(2, eventName);
	      preparedStmt.executeUpdate();
	      return true;
		 }catch(Exception exp) {
			 return false;
		 }
	}
	
	boolean insertEvent(String eventName, String subscriber, long eventTime) {
		try {
		 eventKey = UUID.randomUUID().toString();
		 String sql = "INSERT INTO "+ CommonDataArea.table_TeleHealthcareFlow__Event + " (eventKey,eventObject,eventName,time, subscriber,timeStr) values (?,?,?,?,?,?)";
		 DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Date today =  Calendar.getInstance().getTime();  
		 today.setTime(eventTime);
		 String dateStr = df.format(today);
		 PreparedStatement preparedStmt = con.prepareStatement(sql);
		 preparedStmt.setString(1, eventKey);
		 preparedStmt.setString(2, this.getClass().getName());
		 preparedStmt.setString(3,eventName);
		 preparedStmt.setLong(4, eventTime);
		 preparedStmt.setString(5,subscriber);
		 preparedStmt.setString(6,dateStr);
		 preparedStmt.execute();
		 insertEventAction(eventKey);	
		 return true;
		}
		catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
		} 
	}
	

	boolean insertEventAction(String eventKey) {
		try {
		 
		 String sql = "INSERT INTO "+ CommonDataArea.table_TeleHealthcareFlow__EventAction + " (eventKey,actionStatus,actionNote,ActionTime) values (?,?,?,?)";
		 DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		 Date today =  Calendar.getInstance().getTime();  
		 String dateStr = df.format(today);
		 PreparedStatement preparedStmt = con.prepareStatement(sql);
		 preparedStmt.setString(1, eventKey);
		 preparedStmt.setString(2, "UnAssigned");
		 preparedStmt.setString(3,"NeW Event- assignment Pending");
		 preparedStmt.setLong(4, System.currentTimeMillis());
		 preparedStmt.execute();
		 return true;
		}
		catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
		} 
	}
	
	
}
