package ithings.sensors.preprocessor;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import ithings.common.CommonDataArea;
import ithings.common.CommonUtilities;
import ithings.userevents.inactivity.BathroomBreakDetector;
import ithings.userevents.inactivity.InactivityDetector;
import ithings.userevents.inactivity.WakeupNBreakfastDetector;
import ithings.workflow.WorkflowBasic;

public class Preprocessor {

	static String tenant="sptest";
	static String table_deviceTable;
	static String table_deviceStatus;
	static String table_sensorTrigHist;
	static long minute = 60*1000;
	static long dayMills = 24l*60l*60l*1000l;
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		table_deviceTable = tenant + "_TeleHealthcareFlow__Device";
		table_deviceStatus = tenant +  "_TelehealthcareFlow__SensorStatus";
		table_sensorTrigHist = tenant + "_TeleHealthcareFlow__SensorTrigHistory";
		//DriverManager.registerDriver(new com.mysql.jdbc.Driver ());
		
		CommonDataArea.init();
		Connection con= CommonDataArea.getMySQLCon();
	
		CommonDataArea.topic = CommonUtilities.readSettingsString("FirebaseTopic", con);
		
		////////////
	
	//	WorkflowBasic wf=new WorkflowBasic();
	//	 wf.setCon(con);
	//     wf.run();
		 // wf.assignEventWake("a4cf2321-d77d-49a7-8796-8bc8f186d57e","royj@mailinator.com","Wakeup Event", "Wake3.30-5",false,"detected");
	 
	   
		////////////
		 
		 CommonUtilities.sendLogToStdout("UserEventProcessor","Starting");
		if((args.length>0)&&(args[0].equals("Preprocessor"))) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					while(true) {
						try {
							
					Preprocessor pp = new Preprocessor();
					pp.preProcessData();
					CommonUtilities.sendLogToStdout("Preprocessor","Iteration completed sleep 1 min ");
					Thread.sleep(60000);
					}catch(Exception exp) {
						
					}
					
				}}
			}).start();;
		}
		CommonUtilities.sendLogToStdout("UserEventProcessor","Checking for InactivityMon");
		if((args.length>0)&&(args[0].equals("InactivityMon"))) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					InactivityDetector id = new InactivityDetector();
					id.setCon(CommonDataArea.getMySQLCon());
					while(true) {
						try {
							
							id.run();
							if(!id.unprocessed_data_available) {
								CommonUtilities.sendLogToStdout("Inactivity Monitor","Iteration completed Sleep 1.5 Min\r\n\r\n\r\n\r\n");
								Thread.sleep(90000);
								id = new InactivityDetector();
								id.setCon(CommonDataArea.getMySQLCon());
							}else Thread.sleep(20);
							
						}catch(Exception exp) {
							
						}
					}
				}
			}).start();
		}
		CommonUtilities.sendLogToStdout("UserEventProcessor","Checking  for Wakeup");
		if((args.length>0)&&(args[0].equals("WakeupMon"))) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					WakeupNBreakfastDetector wd = new WakeupNBreakfastDetector();
					wd.detectEvent="WakeUp";
					wd.setCon(CommonDataArea.getMySQLCon());
					while(true) {
						
						try {
							
							wd.run();
							if(!wd.unprocessed_data_available) {
							CommonUtilities.sendLogToStdout("Wakeup Event","Iteration completed Sleep 1 Min\r\n\r\n\r\n");
							Thread.sleep(90000);
							wd = new WakeupNBreakfastDetector();
							wd.detectEvent="WakeUp";
							wd.setCon(CommonDataArea.getMySQLCon());
							}else {
								Thread.sleep(20);
							}
							if(!wd.isConnected()) wd.setCon(CommonDataArea.getMySQLCon());
						}catch(Exception exp) {
							
						}
					}
				}
			}).start();
		}
		
		CommonUtilities.sendLogToStdout("UserEventProcessor","Checking  for Breakfast Mon");
		if((args.length>0)&&(args[0].equals("BreakfastMon"))) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					WakeupNBreakfastDetector wd = new WakeupNBreakfastDetector();
					wd.detectEvent="Breakfast";
					wd.setCon(CommonDataArea.getMySQLCon());
					while(true) {
						
						try {
							
							wd.run();
							if(!wd.unprocessed_data_available) {
							CommonUtilities.sendLogToStdout("Breakfast Event","Iteration completed Sleep 1 Min\r\n\r\n\r\n");
							Thread.sleep(90000);
							wd = new WakeupNBreakfastDetector();
							wd.detectEvent="Breakfast";
							wd.setCon(CommonDataArea.getMySQLCon());
							}else {
								Thread.sleep(20);
							}
							if(!wd.isConnected()) wd.setCon(CommonDataArea.getMySQLCon());
						}catch(Exception exp) {
							
						}
					}
				}
			}).start();
		}
		CommonUtilities.sendLogToStdout("UserEventProcessor","Checking  for BathroomMon");
		if((args.length>0)&&(args[0].equals("BathroomMon"))) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					BathroomBreakDetector bbd = new BathroomBreakDetector();
					bbd.setCon(CommonDataArea.getMySQLCon());
					while(true) {
						
						try {
							CommonUtilities.sendLogToStdout("Bathroom Event","Iteration completed Sleep 1 Min\r\n\r\n\r\n");
							bbd.run();
							if(!bbd.unprocessed_data_available) {
							Thread.sleep(90000);
							bbd = new BathroomBreakDetector();
							bbd.setCon(CommonDataArea.getMySQLCon());
							}else {
								Thread.sleep(20);
							}
							if(!bbd.isConnected()) bbd.setCon(CommonDataArea.getMySQLCon());
						}catch(Exception exp) {
							
						}
					}
				}
			}).start();
		}
		CommonUtilities.sendLogToStdout("UserEventProcessor","Checking  for WorkFlow");
		if((args.length>0)&&(args[0].equals("WorkFlow"))) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					// TODO Auto-generated method stub
					WorkflowBasic wb=new WorkflowBasic();
					
					wb.setCon(CommonDataArea.getMySQLCon());
					while(true) {
						
						try {
							
							wb.run();
							if(!wb.unprocessed_data_available) {
							CommonUtilities.sendLogToStdout("WorkFlow","Iteration completed Sleep 1 Min\r\n\r\n\r\n");
							Thread.sleep(90000);
							wb = new WorkflowBasic();
							
							wb.setCon(CommonDataArea.getMySQLCon());
							}else {
								Thread.sleep(20);
							}
							if(!wb.isConnected()) wb.setCon(CommonDataArea.getMySQLCon());
						}catch(Exception exp) {
							System.out.println(exp);
						}
					}
				}
			}).start();
		}
		
		
		
	}
	Connection con;
	public void preProcessData() {
		try {
			String deviceId;
			String subscriber;
			String tag;
			con=CommonDataArea.getMySQLCon();
			Statement stmt=con.createStatement();  
			
			ResultSet devList=stmt.executeQuery("select * from "+ table_deviceTable + " where  ___smart_state___ like 'active%'");
			//Iterate for each active device in the DB
			long curTime = System.currentTimeMillis();
			while(devList.next())  {
				deviceId = devList.getString("deviceId");
				subscriber = devList.getString("subscriber");
				tag = devList.getString("tag");
				CommonUtilities.sendLogToStdout("Preprocessor","Device ID: "+deviceId);
				//1. Get last device status timestamp in db
				long lastUpdateTimeStamp = getLastDeviceStatusTimeStamp(deviceId);
				CommonUtilities.sendLogToStdout("Preprocessor","Last Updated Time: "+lastUpdateTimeStamp);
				if(lastUpdateTimeStamp<0) {
					lastUpdateTimeStamp = getLowesTimeStampSensor(deviceId);
					CommonUtilities.sendLogToStdout("Preprocessor" ,"GetLowesTimeStampSensor: "+lastUpdateTimeStamp);
				}
				
				//2. Check in device history table for any entry for the device
				//after last updation done
				curTime = curTime-minute ; //just update  a minute less
				curTime = adjustTimeStampToStartOfMinute(curTime);
				lastUpdateTimeStamp = adjustTimeStampToStartOfMinute(lastUpdateTimeStamp);
				CommonUtilities.sendLogToStdout("Preprocessor","AdjustTimeStampToStartOfMinute: "+lastUpdateTimeStamp);
				long endTimeSlot = lastUpdateTimeStamp + minute;
				String status = "active";
				while(endTimeSlot< curTime) {
					CommonUtilities.sendLogToStdout("Preprocessor","CheckMotionSensorTrigered between start: "+lastUpdateTimeStamp +" End Time :"+endTimeSlot);
					int triggers = checkMotionSensorTrigered(lastUpdateTimeStamp,endTimeSlot,deviceId);
					if(triggers>0) status = "active"; else status = "inactive";
					CommonUtilities.sendLogToStdout("Preprocessor","Inserting to DB -> "+ lastUpdateTimeStamp + " Dev ID ->"+ deviceId + " Subscriber ->"+ subscriber + " Status->"+ status);
					insertEntryToSensorStatus(lastUpdateTimeStamp,deviceId,subscriber,tag, status);
					endTimeSlot = endTimeSlot+minute ;
					lastUpdateTimeStamp+=minute;
				}
				CommonUtilities.sendLogToStdout("Preprocessor","Finished preprocesing for  for ->"+ deviceId);
			}
			
			con.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		
		}  
	}
	
	long adjustTimeStampToStartOfMinute(long timeStamp) {
		
		long divd = timeStamp/minute;
		long adjuted = divd * minute;
		return adjuted;
	}
	
	//This function calculate the minutes of the 
	//corresponding day of the timeatmp provided
	long minuteOfTheDay(long timeStamp) {
		long daysMillsElapsed = timeStamp%dayMills;
		long minutesOfDay = daysMillsElapsed/minute;
		return minutesOfDay;
	}
	
	long getLowesTimeStampSensor(String deviceID) {
		try {
			long time=-1;
			Statement stmt=con.createStatement(); 
			String query = "Select timestamp from " +table_sensorTrigHist + " order by timestamp ASC limit 1";
			ResultSet list=stmt.executeQuery(query);
			
			while(list.next())  {
				time = list.getLong("timestamp");
			}
			if((time==0)||(time<0)) time = 1545935400000l;// System.currentTimeMillis() - (5l*24l*60l*60l*1000l); //start from previous month if zero or -1
			return time;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			} 
	}
	long getLastDeviceStatusTimeStamp(String deviceID) {
		try {
		long time=-1;
		Statement stmt=con.createStatement(); 
		String query = "Select timestamp from " +table_deviceStatus + " where deviceID like '"+ deviceID+ "%' order by timestamp DESC limit 1";
		ResultSet devList=stmt.executeQuery(query);
		
		while(devList.next())  {
			time = devList.getLong("timestamp");
		}
		return time;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} 
	}

	int checkMotionSensorTrigered(long startTimeStamp, long endTimeStamp, String deviceID) {
		try {
		int count=-1;
		Statement stmt=con.createStatement(); 
		String query = "select id  from " +table_sensorTrigHist + " where deviceID like '"+ deviceID+ "%' and timestamp between "+ startTimeStamp +" and " +endTimeStamp;
		//System.out.println("Query : " + query);
		ResultSet list=stmt.executeQuery(query);
		
		while(list.next())  {
			++count ;
		}
		return count;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		} 
	}
		
	boolean insertEntryToSensorStatus(long startTime, String deviceID, String subscriber,String tag, String status) {
		try {
			
			String query = "INSERT INTO "+ table_deviceStatus + " (deviceID,subscriber,date,minute,sensorType,sensorStatus, timestamp,tag ) values (?,?,?,?,?,?,?,?)";
			PreparedStatement preparedStmt = con.prepareStatement(query);
			
			DateFormat simple = new SimpleDateFormat("yyyy-MM-dd"); 
			Date result = new Date(startTime); 
			String dateStr = simple.format(result);
			long minuteOfDay = minuteOfTheDay(startTime);
			String sensorType = "MotionSensor";

			preparedStmt.setString(1, deviceID);
			preparedStmt.setString(2, subscriber);
			preparedStmt.setString(3, dateStr);
			preparedStmt.setInt(4, (int)minuteOfDay);
			preparedStmt.setString(5, "MotionSensor");
			preparedStmt.setString(6, status);
			preparedStmt.setLong(7, startTime);
			preparedStmt.setString(8, tag);
			preparedStmt.execute();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	return true;
	}
	
	
}
