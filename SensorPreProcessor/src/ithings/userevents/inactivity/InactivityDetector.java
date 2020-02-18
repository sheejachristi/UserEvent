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
public class InactivityDetector {
	private Connection con;
	String eventKey;
	static int inactiveTimeGap = 30*60*1000;// Current set up 10 minutes
	public boolean unprocessed_data_available =false;
	int inactiveGapMin;

	public Connection getCon() {
		return con;
	}

	public void setCon(Connection con) {
		this.con = con;
	}
	
	public boolean run() {
		
		//String mesg = "Starting Inactivity detector";
		//String mesg = "Subscriber -> Jevel Soucy  is found Inactive for last half and hour";
		//CommonUtilities.sendPushNotification(topic,"InactivityAlert","EventAlert","evntName","2001", "steve@sadlerconsultancy.com",  "steve@sadlerconsultancy.com", "8989898", "");
		//sendUserEventNotification("Inactivity Alert", "2001","Inactivity Alert Jewel", "ImmanuelJones@mailinator.com");
		//sendUserEventNotification("Inactivity Alert", "Inactivity Alert Haryy", "HarryThomas@mailinator.com");
		//sendUserEventNotification("Inactivity Alert", "Inactivity Alert Milind", "milidaniels@mailinator.com");
		//sendUserEventNotification("Inactivity Alert", "Inactivity Alert Sam", "samuelharper@mailinator.com");
	
		//CommonUtilities.sendPushNotification(topic, "Inactivity Alert", mesg);
		//CommonUtilities.sendPushNotification(topic, "InActivityAlert", mesg);
		return processConfiguredIncativityEventDefs();
	}

	//Check in the database for inactivity Event Definitions
	//configured and process it
	//Note :- Event detection window time to be configured in main 
	//table , which is missing now.
	boolean processConfiguredIncativityEventDefs() {
		try {
			SimpleDateFormat format = new SimpleDateFormat("HH:mm aa");
			long curTime = System.currentTimeMillis() - CommonDataArea.minute;
			SimpleDateFormat dateFull = new SimpleDateFormat("yyyy-MM-dd:HH:mm aa");

			long lastProcessedTime = 0;
			long eventTime = 0;
			unprocessed_data_available = false;

			Statement stmt = con.createStatement();
			String query = "select *  from " + CommonDataArea.table_TeleHealthcareFlow__SubscriberEventDef
					+ " where (category like '%Inactivity%' or category like '%InActivity%') and ___smart_state___  like 'active'";
			ResultSet list = stmt.executeQuery(query);
			String subscriber = "";
			String eventName = "";
			CommonUtilities.sendLogToStdout(
					"*********************START INACTIVITY CHECK ITERATION************************",
					"Version ->" + CommonDataArea.version);
			// CommonUtilities.sendLogToStdout("****************START INACTIVITY CHECK
			// ITERATION*********************","EventDef found for Subscriber ->"+subscriber
			// +" Event Name "+ eventName);

			boolean passed = true;
			while (list.next()) {
				try {

					lastProcessedTime = list.getLong("lastProcessedTime");
					if ((lastProcessedTime == 0) || (lastProcessedTime < CommonDataArea.START_OF_2019)) {
						lastProcessedTime = System.currentTimeMillis() - 5 * 60 * 1000;
						updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
					}
					if ((lastProcessedTime + 60000) > System.currentTimeMillis()) {
						continue;
					}
					unprocessed_data_available = true;
					Date curTimeDt = new Date();
					curTimeDt.setTime(lastProcessedTime);
					int curHourMin = curTimeDt.getHours() * 60 + curTimeDt.getMinutes();
			

					subscriber = list.getString("subscriber");
					eventName = list.getString("name");
					CommonUtilities.sendLogToStdout("@@@@@@@@Processing Time ->", dateFull.format(curTimeDt));
					CommonUtilities.sendLogToStdout("Checking for Inactivity EventDef in SubscriberEventDef ",
							"EventDef found for Subscriber ->" + subscriber + " Event Name " + eventName);
					// get sensors configured for the event from
					String query2 = "select *  from " + CommonDataArea.table_SubscriberEventDetail
							+ " where belongsTo like '" + eventName + "%' and  subscriber like '" + subscriber + "%'";
					System.out.println(query2);
					Statement stmt2 = con.createStatement();
					ResultSet eventSensors = stmt2.executeQuery(query2);

					int recsFound = 0;
					int startHourMin = 0;
					int endHourMin = 0;
					passed = false;
					String inactiveSensors = "";
					
					while (eventSensors.next()) {
						String timeStr = eventSensors.getString("betweenTime");
						inactiveTimeGap = eventSensors.getInt("timeOut");
						
						inactiveGapMin = inactiveTimeGap;
						if (inactiveTimeGap == 0)
							inactiveTimeGap = 15;
						inactiveTimeGap = inactiveTimeGap * 60 * 1000;// convert to mills

						int indexStr = timeStr.indexOf(":::");
						timeStr = timeStr.substring(indexStr + 3);
						String sensorTag = eventSensors.getString("tag");
						String starTimeStr = "";
						String endTimeStr = "";
						long startTime = 0;// = Long.parseLong(starTimeStr);
						long endTime = 0;
						;// = Long.parseLong(endTimeStr);
						Date startTimeDt;
						Date endTimeDt;

						recsFound++;
						try {
							JSONObject tomJsonObject = new JSONObject(timeStr);
							startTime = tomJsonObject.getLong("startTime");
							endTime = tomJsonObject.getLong("endTime");
							
							startTimeDt = new Date();
							startTimeDt.setTime(startTime);
							starTimeStr = format.format(startTimeDt);
							
							startHourMin = startTimeDt.getHours() * 60 + startTimeDt.getMinutes();

							endTimeDt = new Date(endTime);
							endTimeDt.setTime(endTime);
							endTimeStr = format.format(endTimeDt);
							endHourMin = endTimeDt.getHours() * 60 + endTimeDt.getMinutes();

							CommonUtilities.sendLogToStdout("Incativity",
									"Sensosr - " + sensorTag + " Time set for Inactivity->StartTime=" + starTimeStr
											+ " EndTime=" + endTimeStr + " Inactivity Time->" + inactiveGapMin);
						} catch (JSONException e) {
							CommonUtilities.sendLogToStdout("Incativity",
									"Exception Parsing  time string" + e.getMessage());
						}
                              System.out.println(curHourMin+"---"+startHourMin+"----"+endHourMin);     
						if ((curHourMin > startHourMin) && (endHourMin > curHourMin)) {
							// CommonUtilities.sendLogToStdout("Incativity","Sensosr - " + sensorTag+ " Time
							// set for Inactivity->StartTime="+starTimeStr+" EndTime="+endTimeStr);
							// CommonUtilities.sendLogToStdout("Incativity","Inactivity Monitoring Set for
							// curTime for sensor ->"+sensorTag);

							long watchStartTime = lastProcessedTime - inactiveTimeGap;// (10*60*1000);
							long curEndHourMinMills = lastProcessedTime - CommonDataArea.idleTime;

							// String query3 = "select count(*) from sptest_TelehealthcareFlow__SensorStatus
							// where tag like '"+ sensorTag + "%' and subscriber like '"+ subscriber +"%'
							// and sensorStatus like 'active' and timestamp > "+watchStartTime;
							String query3 = "select timeStamp from sptest_TeleHealthcareFlow__SensorTrigHistory where tag like '"
									+ sensorTag + "%' and subscriber like '" + subscriber + "%' and timeStamp > "
									+ watchStartTime + " and  timeStamp<" + curEndHourMinMills
									+ " order by Id ASC Limit 1";
							
							Statement stmt3 = con.createStatement();
							ResultSet activityCount = stmt3.executeQuery(query3);

							if (activityCount.next()) {
								if (activityCount.getLong(1) > 0) {
									passed = true;
									// CommonUtilities.sendLogToStdout("Checking for Inactivity","Sensor
									// ->"+sensorTag+ "Not Inactive");
								} else {
									CommonUtilities.sendLogToStdout("Checking for Inactivity", "Sensor ->" + sensorTag
											+ " Inactive for last ->" + inactiveGapMin + " minutes");
									inactiveSensors += (sensorTag + " , ");
									passed = false;
								}
							} else {
								CommonUtilities.sendLogToStdout("Checking for Inactivity",
										"Sensor ->" + sensorTag + " Inactive for last->" + inactiveGapMin + " minutes");
								passed = false;
								inactiveSensors += (sensorTag + "\r\n");
							}
						} else {
							CommonUtilities.sendLogToStdout("Incativity",
									"NO Inactivity Monitoring Set for curTime for sensor ->" + sensorTag);
							passed = true;
						}
						if (passed)
							break;
					}

					// if event conditions passed then insert in event table
					if (!passed) {
						CommonUtilities.sendLogToStdout("Inactivity Detected", "Checking whether already reported");
						if (!checkSameEventExist(eventName, subscriber, startHourMin, endHourMin, lastProcessedTime)) {
							CommonUtilities.sendLogToStdout("Inactivity Detected",
									"Insering to DB Following sensors found inactive ->" + inactiveSensors);
							
							insertEvent(eventName, subscriber, lastProcessedTime);
							CommonUtilities.sendLogToStdout("Inactivity Detected", "Sending firebase message");

							// String mesg = "Subscriber ->"+ subscriber +" is found Inactive for last half
							// and hour and sensors "+ inactiveSensors+ " are incative";
							/////////////////////////////////////////////
							  WorkflowBasic wf=new WorkflowBasic();
							  wf.setCon(con);
							   wf.assignEventInact(eventKey,subscriber,"InAactivityAlert", eventName,inactiveGapMin);
							  
							/////////////////////////////////////////////
							//sendUserEventNotification("InAactivityAlert", eventKey, eventName, subscriber);
							// CommonUtilities.sendPushNotification(topic, "InActivityAlert", mesg);
						}
					} else {
						CommonUtilities.sendLogToStdout("Inactivity", "Inactivity detection not enabled or detected");
					}

				} catch (SQLException exp) {
					CommonUtilities.sendLogToStdout("SQLException", "Exception ->" + exp.getMessage());
				}
				lastProcessedTime += CommonDataArea.idleTime;
				updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
			}
			if (!unprocessed_data_available)
				con.close();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			System.out.println(e.getMessage());
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
			 System.out.println(exp.getMessage());
			 exp.printStackTrace();
			 return false;
		 }
	}
	
	long adjustHourMinToDay(long millsOfDay,int endTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis =c.getTimeInMillis()+(long)endTimeHourMins*60l*1000l;
		return millis;
	}
	
	boolean sendUserEventNotification(String event,String eventKey, String evntName, String subscriber) {
		 String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Subscriber + " where emailId like '"+subscriber+"%'";
		 try {
				PreparedStatement preparedStmt = con.prepareStatement(sql);
				ResultSet activityCount  =preparedStmt.executeQuery();
				
				while(activityCount.next()) {
					String name = activityCount.getString("name");
					String phone = activityCount.getString("phone");
					String sub_photo = activityCount.getString("sub_photo");
					String topic ="/topics/ihealthcare";
					String body = "Subscriber ->"+ name + " is Inactive for last "+ inactiveGapMin +" minutes";
					//BMJO to do read IP from  a config file or table
					String imageUrl =sub_photo;// "http://178.128.165.237//php//photos//" +phone+ ".jpg";
					CommonUtilities.sendPushNotification(event, body,evntName,eventKey, name, subscriber, phone, imageUrl,"detected");
					return true;
				}
				return false;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
	}
	//If same event with posted for subscriber in last haf hour
	boolean checkSameEventExist(String eventName, String subscriber, int startTime , int endTime, long eventTime ) {
		
		 long startTimeMills;// = adjustHourMinToDay(eventTime,startTime);
		 long endTimeMillis =  adjustHourMinToDay(eventTime,endTime);
		 startTimeMills = eventTime- inactiveTimeGap;
		 String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Event + " where subscriber like '"+ subscriber+ "'  and time > "+startTimeMills + " and time < "+ eventTime+" and eventName like '" +eventName + "' limit 1";
		 //System.out.println(sql);
		 try {
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			ResultSet activityCount  =preparedStmt.executeQuery();
			if(activityCount.next()) {
				CommonUtilities.sendLogToStdout("Wakeup Detected","Already reported event ->"+ eventName +" for -> "+subscriber);
				return true;
			}
			else
			{
			CommonUtilities.sendLogToStdout("Wakeup Detected","Not Reported this event ->"+ eventName +" for -> "+subscriber);

			return false;
		 }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		 System.out.println("INSERT INTO "+ CommonDataArea.table_TeleHealthcareFlow__Event + " (eventKey,eventObject,eventName,time, subscriber,timeStr) values ("+eventKey+","+this.getClass().getName()+","+eventName+","+eventTime+","+subscriber+","+dateStr+")");
		 
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
