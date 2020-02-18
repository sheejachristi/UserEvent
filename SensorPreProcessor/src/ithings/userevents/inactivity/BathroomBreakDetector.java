package ithings.userevents.inactivity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;

import ithings.common.CommonDataArea;
import ithings.common.CommonUtilities;
import ithings.workflow.WorkflowBasic;

public class BathroomBreakDetector {
	public  static int MAX_BATHROOM_DELAY = 15 * 60 * 1000; // 15 minutes
	private Connection con = null;
	String eventKey;
	int sensorLoc = CommonDataArea.SENSOR_LOC_INSIDE_BATHROOM;
	public boolean unprocessed_data_available = true;
	long bathroomBreakStartTime = 0;

	public Connection getCon() {
		return con;
	}

	public boolean isConnected() {
		try {
			if (this.con != null && this.con.isValid(60000))
				return true;
			else
				return false;
		} catch (Exception exp) {
			return false;
		}
	}

	public void setCon(Connection con) {
		this.con = con;
	}

	public boolean run() {

		// String mesg = "Starting Inactivity detector";
		
		// sendUserEventNotification("Inactivity Alert",
		// "b3128776-3fb3-4fa2-a217-3af986a6e0e3","Breakfast Alert Mili",
		// "milidaniels@mailinator.com");
		// sendUserEventNotification("Inactivity
		// Alert","496c11ac-018f-4f68-8e6d-fe45fd99e19f", "Wakeup Alert Mili",
		// "milidaniels@mailinator.com");
		/*
		 * sendUserEventNotification("Inactivity Alert", "Inactivity Alert Milind",
		 * "milidaniels@mailinator.com"); sendUserEventNotification("Inactivity Alert",
		 * "Inactivity Alert Sam", "samuelharper@mailinator.com");
		 */

		// CommonUtilities.sendPushNotification(topic, "Inactivity Alert", mesg);
		// CommonUtilities.sendPushNotification(topic, "InActivityAlert", mesg);
		processConfiguredBathroomBreakEventDefs();
		return true;
	}

	// LOGIC
	// Assumption : Even if bathroom sensor triggers after bathroom event
	// Starts it will be taken as alarm condition - May be user stuck there
	// ---------------------------------------------------------------------
	// Step 1.
	// Check any bathroom sensor triggered
	// If no open bathroom break exist start a bathroom break
	// Step 2.
	// Check any bathroom break in progress
	// Check if MAX_BATHROOM_DELAY expired- No outside sensor trigger detected
	// within that time
	// Mark it as a Bathroom event Missed
	// Otherwise check any outside sensor triggered after bathroom break started, If
	// so close it

	// For the convinience No I take first sensor as bathroom sensor and
	// second one as outside sensor. Once new template design complete it will be
	// used
	// Event status - InProgress
	// Generated - Successfully completed and an event generated
	// Missed - Event failed , no return recorded
	void processConfiguredBathroomBreakEventDefs() {
		try {

			SimpleDateFormat format = new SimpleDateFormat("HH:mm aa");
			SimpleDateFormat dateFull = new SimpleDateFormat("yyyy-MM-dd:HH:mm aa");

			long eventTime =0;
			long lastProcessedTime = 0;
			String alertGenOption="NA";
			Statement stmt = con.createStatement();
			String query = "select *  from " + CommonDataArea.table_TeleHealthcareFlow__SubscriberEventDef
					+ " where category like '%BathroomBreak%' and ___smart_state___  like 'active'";
			ResultSet list = stmt.executeQuery(query);
			String subscriber = "";
			String eventName = "";
			CommonUtilities.sendLogToStdout(
					"*********************START BATHROOM CHECK ITERATION************************",
					"Version ->" + CommonDataArea.version);

			boolean passed = true;
			unprocessed_data_available = false;
			while (list.next()) {
				
					lastProcessedTime = list.getLong("lastProcessedTime");
					if((lastProcessedTime==0)||(lastProcessedTime<CommonDataArea.START_OF_2019)) {
						lastProcessedTime = System.currentTimeMillis() - 5 * 60 * 1000;
						updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
					}
					if ((lastProcessedTime + 60000) > System.currentTimeMillis()) {
						// unprocessed_data_available=false;
						continue;
					}
					unprocessed_data_available = true;
					Date curTimeDt = new Date();
					curTimeDt.setTime(lastProcessedTime);
					int curHourMin = (curTimeDt.getHours()) * 60 + curTimeDt.getMinutes();
					subscriber = list.getString("subscriber");
					eventName = list.getString("name");
					CommonUtilities.sendLogToStdout("@@@@@@@@Processing Time ->", dateFull.format(curTimeDt));
					CommonUtilities.sendLogToStdout("Checking for Bathroom EventDef in SubscriberEventDef ",
							"EventDef found for Subscriber ->" + subscriber + " Event Name " + eventName);
					// get sensors configured for the event from
					String query2 = "select *  from " + CommonDataArea.table_SubscriberEventDetail
							+ " where belongsTo like '" + eventName + "%' and  subscriber like '" + subscriber + "' order by sequence asc";
					Statement stmt2 = con.createStatement();
					ResultSet eventSensors = stmt2.executeQuery(query2);
					System.out.println(query2);
					CommonUtilities.sendLogToStdout("Checking Bathroom set for current time",
							"Event found for Subscriber ->" + subscriber + " Event Name " + eventName);
					int recsFound = 0;
					int startHourMin = 0;
					int endHourMin = 0;
					int maxBathroomDelay = MAX_BATHROOM_DELAY;
					sensorLoc = CommonDataArea.SENSOR_LOC_INSIDE_BATHROOM;
					while (eventSensors.next()) {
						String sensorTag="";
						// Take the period set for event detection
						if (sensorLoc == CommonDataArea.SENSOR_LOC_INSIDE_BATHROOM) {
							
							String timeStr = eventSensors.getString("betweenTime");
							maxBathroomDelay = eventSensors.getInt("timeOut");
							alertGenOption = eventSensors.getString("generateEvent");
							if(alertGenOption==null)alertGenOption="2";
							if(maxBathroomDelay==0) maxBathroomDelay=5;
							maxBathroomDelay=maxBathroomDelay*(60*1000);
							int indexStr = timeStr.indexOf(":::");
							timeStr = timeStr.substring(indexStr + 3);
							sensorTag = eventSensors.getString("tag");
							System.out.println("sensorTag"+sensorTag);
							String starTimeStr = "";
							String endTimeStr = "";
							long startTime = 0;// = Long.parseLong(starTimeStr);
							long endTime = 0;
							//;// = Long.parseLong(endTimeStr);
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
								startHourMin = (startTimeDt.getHours()) * 60 + startTimeDt.getMinutes();

								endTimeDt = new Date(endTime);
								endTimeDt.setTime(endTime);
								endTimeStr = format.format(endTimeDt);
								endHourMin = (endTimeDt.getHours()) * 60 + endTimeDt.getMinutes();

								CommonUtilities.sendLogToStdout("Bathroom Event",
										"Time set for Wakeup Event->StartTime=" + starTimeStr + " EndTime="
												+ endTimeStr);
							} catch (JSONException e) {
								CommonUtilities.sendLogToStdout("Bathroom Event",
										"Exception Parsing  time string" + e.getMessage());
							}
							
							if(!IsBathroomBreakInProgress(subscriber, eventName)) {
								// Check whether bathroom break detected
								if ((curHourMin > startHourMin) && (curHourMin < endHourMin)) {
										if(!checkNCreateABatthroomBreak(startHourMin, endHourMin, sensorTag, lastProcessedTime, subscriber, eventName)) {
											CommonUtilities.sendLogToStdout("Bathroom Event","Not found for-> "+sensorTag);
											lastProcessedTime += CommonDataArea.idleTime;  
											 //outside event
											updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
										}
											
								}else {
								
								CommonUtilities.sendLogToStdout("Bathroom Event","Event "+eventName+" Not set for current time ");
								lastProcessedTime += CommonDataArea.idleTime;  
																				 //outside event
								updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
								}
								break; //If bathroom break not in progress break the loop as there is no need to check whether outside sensor triggered
							}//todo BMJO check why this check is needed
							else if(isBathroomEventFinishRecently(subscriber, eventName,lastProcessedTime))
							{
								CommonUtilities.sendLogToStdout("Bathroom Event","Event "+eventName+" Happened recently ");
								lastProcessedTime +=CommonDataArea.idleTime;
								 //outside event
								updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
								break;
							}
							sensorLoc=CommonDataArea.SENSOR_LOC_OUTSIDE_BATHROOM;
					}else if (sensorLoc == CommonDataArea.SENSOR_LOC_OUTSIDE_BATHROOM) {
						sensorTag = eventSensors.getString("tag");
						if((lastProcessedTime-bathroomBreakStartTime)>maxBathroomDelay) {
							//Update bath room event as missed
							CommonUtilities.sendLogToStdout("Bathroom Event","Event MISSED "+eventName);
							updateBathroomEventMissed(eventKey,lastProcessedTime);
							if((alertGenOption.contains(CommonDataArea.ALERTGEN_ONMISSED))||(alertGenOption.contains(CommonDataArea.ALERTGEN_ONBOTH))||(alertGenOption.contains(CommonDataArea.ALERTGEN_NA))) {
								//sendUserEventNotification("Bathroom Event Missed",eventKey,eventName,subscriber,true, CommonDataArea.EVENT_STATUS_MISSED);
								//////////////////////////////////////////
								WorkflowBasic wf=new WorkflowBasic();
								  wf.setCon(con);
								   wf.assignEventBathroom(eventKey,subscriber,"Bathroom Event Missed", eventName,true,CommonDataArea.EVENT_STATUS_MISSED);
							///////////////////////////////////////
							}
						}else {
							//Check outside sensor triggered
							if(checkOutsideSensorTriggered(bathroomBreakStartTime, lastProcessedTime, sensorTag, subscriber)) {
								updateBathroomEventClosed(eventKey,lastProcessedTime);
								if((alertGenOption.contains(CommonDataArea.ALERTGEN_ONEVENT))||(alertGenOption.contains(CommonDataArea.ALERTGEN_ONBOTH))||(alertGenOption.contains(CommonDataArea.ALERTGEN_NA))) {
									//sendUserEventNotification("Bathroom Event Closed",eventKey,eventName,subscriber,false, CommonDataArea.EVENT_STATUS_DETECTED);
								////////////////////////////////////////////////
									WorkflowBasic wf=new WorkflowBasic();
									  wf.setCon(con);
									   wf.assignEventBathroom(eventKey,subscriber,"Bathroom Event Closed", eventName,false,CommonDataArea.EVENT_STATUS_DETECTED);
								
								///////////////////////////////////////////////
								}
							}
						}
					}
					
					lastProcessedTime += CommonDataArea.idleTime;
						 //outside event
					updateLatUpdatedTime(lastProcessedTime, subscriber, eventName);
				
			}
			if (!unprocessed_data_available)
				con.close();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
	}

	boolean checkOutsideSensorTriggered(long eventTime, long curTime, String sensorTag, String subscriber) {
		try {
			String query3 = "select timeStamp from sptest_TeleHealthcareFlow__SensorTrigHistory where tag like '"
					+ sensorTag + "%' and subscriber like '" + subscriber + "%' and timeStamp > " + eventTime
					+ " and timeStamp<" + curTime + " order by Id ASC Limit 1";

			Statement stmt3 = con.createStatement();
			ResultSet activityCount = stmt3.executeQuery(query3);

			if (activityCount.next()) { // Record found
				CommonUtilities.sendLogToStdout("Bathroom Event","Outside sensor trigger detected");
				return true;
			} else
			{
				CommonUtilities.sendLogToStdout("Bathroom Event","Not detected outside sensor trigger->"+sensorTag+" Event Time ->"+ eventTime);
				CommonUtilities.sendLogToStdout("Bathroom Event",query3);
				return false;
			}
		} catch (Exception exp) {
			return false;
		}
	}

	boolean  checkNCreateABatthroomBreak(int startHourMIn, int endHourMin, String sensorTag, long curTime, String subscriber,
			String eventName) {
		try {
			CommonUtilities.sendLogToStdout("Bathroom Event",
					"Bathroom Event Monitoring Set for curTime for sensor ->" + sensorTag);

			long monitorStartTime = curTime - CommonDataArea.idleTime * 2;

			String query3 = "select timeStamp from sptest_TeleHealthcareFlow__SensorTrigHistory where tag like '"
					+ sensorTag + "%' and subscriber like '" + subscriber + "%' and timeStamp > " + monitorStartTime
					+ " and timeStamp < " + curTime
					+ " order by Id ASC Limit 1";

			Statement stmt3 = con.createStatement();
			ResultSet activityCount = stmt3.executeQuery(query3);

			if (activityCount.next()) { // Record found

				CommonUtilities.sendLogToStdout("Bathroom Event",
						"Event Found for Sensor ->" + sensorTag + "Activated ->active");
				CommonUtilities.sendLogToStdout("Bathroom Event Query used ", query3);
				long eventTime = activityCount.getLong(1);
				bathroomBreakStartTime = eventTime;
				insertEvent(eventName, subscriber, eventTime);
				activityCount.close();
				
				return true;
			}
			return false;
		} catch (Exception exp) {
			return false;
		}
	}

	boolean IsBathroomBreakInProgress(String subscriber, String eventName) {
		try {
			String sql = "select eventKey,time from  " + CommonDataArea.table_TeleHealthcareFlow__Event
					+ " where subscriber like '" + subscriber + "%'  and eventName like '%" + eventName
					+ "%' and status = 'InProgress'";
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			ResultSet activityCount = preparedStmt.executeQuery();
			boolean ret = false;
			if (activityCount.next()) {
				eventKey = activityCount.getString(1);
				bathroomBreakStartTime = activityCount.getLong(2);
				CommonUtilities.sendLogToStdout("Bathroom Event","Bathroom event in progress :"+eventName);
				ret = true;
			} 
			else  //Check if any bath room finished in between last idle time gap. 
			{
				
				ret = false;
			}
			activityCount.close();
			return ret;
		} catch (Exception exp) {
			return false;
		} finally {

		}
	}
	
	boolean isBathroomEventFinishRecently(String subscriber, String eventName, long curTime) {
		try {
		String sql = "select eventKey,time from  " + CommonDataArea.table_TeleHealthcareFlow__Event
				+ " where subscriber like '" + subscriber + "%'  and eventName like '%" + eventName
				+ "%' and time >= " +(curTime- CommonDataArea.idleTime*2);
		PreparedStatement preparedStmt = con.prepareStatement(sql);
		ResultSet activityCount = preparedStmt.executeQuery();
		boolean ret = false;
		if (activityCount.next()) {
			eventKey = activityCount.getString(1);
			bathroomBreakStartTime = activityCount.getLong(2);
			ret = true;
		}else
		{	
			ret = false;
		}
		activityCount.close();
		return ret;
		} catch (Exception exp) {
			return false;
		}
	}

	boolean sendUserEventNotification(String event, String eventKey, String evntName, String subscriber,boolean missed, String status) {
		String sql = "select * from  " + CommonDataArea.table_TeleHealthcareFlow__Subscriber + " where emailId like '"
				+ subscriber + "%'";
		try {
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			ResultSet activityCount = preparedStmt.executeQuery();

			while (activityCount.next()) {
				String name = activityCount.getString("name");
				String phone = activityCount.getString("phone");
				String topic = "/topics/ihealthcare";
				String body = "Subscriber ->" + name + " A Bathroom Event Detected ";
				if(missed) body ="Subscriber ->" + name + " A MISSED Bathroom Event Detected ";
				// BMJO to do read IP from a config file or table
				String imageUrl = "http://178.128.165.237//php//photos//" + phone + ".jpg";

				CommonUtilities.sendPushNotification(event, body, evntName, name, eventKey, subscriber, phone,
						imageUrl,status);
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
		long millis = c.getTimeInMillis();
		return millis;
	}

	long getStartofDayMills(long millsOfDay, int startTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis = c.getTimeInMillis() + (long) startTimeHourMins * 60l * 1000l;
		;
		return millis;
	}

	long getStartTimeMin(long millsOfDay) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis = c.getTimeInMillis();
		return millis;
	}

	long getTodaysEndTimeMills(int endTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis = c.getTimeInMillis() + (long) endTimeHourMins * 60l * 1000l;
		return millis;
	}

	long getTodaysEndTimeMills(long millsOfDay, int endTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis = c.getTimeInMillis() + (long) endTimeHourMins * 60l * 1000l;
		return millis;
	}

	long adjustHourMinToDay(long millsOfDay, int endTimeHourMins) {
		Calendar c = Calendar.getInstance();
		c.setTimeInMillis(millsOfDay);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		long millis = c.getTimeInMillis() + (long) endTimeHourMins * 60l * 1000l;
		return millis;
	}

	// If same event with posted for subscriber in last haf hour
	boolean checkSameEventExist(String eventName, String subscriber, int startTime, int endTime, long eventTime) {

		long startTimeMills = adjustHourMinToDay(eventTime, startTime);
		long endTimeMillis = adjustHourMinToDay(eventTime, endTime);
		String sql = "select * from  " + CommonDataArea.table_TeleHealthcareFlow__Event + " where subscriber like '"
				+ subscriber + "%'  and time > " + startTimeMills + " and eventName like '%" + eventName + "%'";
		// System.out.println(sql);
		try {
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			ResultSet activityCount = preparedStmt.executeQuery();
			while (activityCount.next()) {
				CommonUtilities.sendLogToStdout("Wakeup Detected",
						"Already reported event ->" + eventName + " for -> " + subscriber);
				return true;
			}
			CommonUtilities.sendLogToStdout("Wakeup Detected",
					"Not Reported this event ->" + eventName + " for -> " + subscriber);

			return false;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	boolean insertMissedEvent(String eventName, String subscriber, long eventTime) {
		try {
			String eventKey = UUID.randomUUID().toString();
			String sql = "INSERT INTO " + CommonDataArea.table_TeleHealthcareFlow__Event
					+ " (eventKey,eventObject,eventName,time, subscriber,timeStr,status) values (?,?,?,?,?,?,'missed')";
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date today = Calendar.getInstance().getTime();
			today.setTime(eventTime);
			String dateStr = df.format(today);
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			preparedStmt.setString(1, eventKey);
			preparedStmt.setString(2, this.getClass().getName());
			preparedStmt.setString(3, eventName);
			preparedStmt.setLong(4, eventTime);
			preparedStmt.setString(5, subscriber);
			preparedStmt.setString(6, dateStr);
			preparedStmt.execute();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	boolean updateLatUpdatedTime(long lastUpdatedTime, String subscriber, String eventName) {
		try {
			String query = "update " + CommonDataArea.table_TeleHealthcareFlow__SubscriberEventDef
					+ " set lastProcessedTime =? where name = ?";
			PreparedStatement preparedStmt = con.prepareStatement(query);
			preparedStmt.setLong(1, lastUpdatedTime);
			preparedStmt.setString(2, eventName);
			preparedStmt.executeUpdate();
			return true;
		} catch (Exception exp) {
			return false;
		}
	}

	boolean updateBathroomEventMissed(String eventKey, long missedTime) {
		try {
			String query = "update " + CommonDataArea.table_TeleHealthcareFlow__Event
					+ " set status =? , endTime = ? ,endTimeStr = ? where eventKey = ? ";
			SimpleDateFormat dateFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date today = Calendar.getInstance().getTime();
			today.setTime(missedTime);	
			PreparedStatement preparedStmt = con.prepareStatement(query);
			preparedStmt.setString(1, "Missed");
			preparedStmt.setLong(2, missedTime);
			preparedStmt.setString(3, dateFull.format(today));
			preparedStmt.setString(4, eventKey);
			preparedStmt.executeUpdate();
			insertEventAction(eventKey);
			return true;
		} catch (Exception exp) {
			return false;
		}
	}

	boolean updateBathroomEventClosed(String eventKey,long closedTime) {
		try {
			String query = "update " + CommonDataArea.table_TeleHealthcareFlow__Event
					+ " set status =? , endTime = ? ,endTimeStr = ? where eventKey = ? ";
			SimpleDateFormat dateFull = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date today = Calendar.getInstance().getTime();
			today.setTime(closedTime);	
			PreparedStatement preparedStmt = con.prepareStatement(query);
			preparedStmt.setString(1, "Generated");
			preparedStmt.setLong(2, closedTime);
			preparedStmt.setString(3, dateFull.format(today));
			preparedStmt.setString(4, eventKey);
			preparedStmt.executeUpdate();
			insertEventAction(eventKey);
			return true;
		} catch (Exception exp) {
			return false;
		}
	}

	boolean insertEvent(String eventName, String subscriber, long eventTime) {
		try {
			eventKey = UUID.randomUUID().toString();
			String sql = "INSERT INTO " + CommonDataArea.table_TeleHealthcareFlow__Event
					+ " (eventKey,eventObject,eventName,time, subscriber,timeStr,status) values (?,?,?,?,?,?,?)";
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date today = Calendar.getInstance().getTime();
			today.setTime(eventTime);
			String dateStr = df.format(today);
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			preparedStmt.setString(1, eventKey);
			preparedStmt.setString(2, this.getClass().getName());
			preparedStmt.setString(3, eventName);
			preparedStmt.setLong(4, eventTime);
			preparedStmt.setString(5, subscriber);
			preparedStmt.setString(6, dateStr);
			preparedStmt.setString(7, "InProgress");
			preparedStmt.execute();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}

	boolean insertEventAction(String eventKey) {
		try {
			
			String sql = "INSERT INTO " + CommonDataArea.table_TeleHealthcareFlow__EventAction
					+ " (eventKey,actionStatus,actionNote,ActionTime) values (?,?,?,?)";
			DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date today = Calendar.getInstance().getTime();
			String dateStr = df.format(today);
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			preparedStmt.setString(1, eventKey);
			preparedStmt.setString(2, "UnAssigned");
			preparedStmt.setString(3, "NeW Event- assignment Pending");
			preparedStmt.setLong(4, System.currentTimeMillis());
			preparedStmt.execute();
			return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
	}
}