package ithings.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class CommonDataArea {
	public static String tenant="sptest";
	public static String table_deviceTable;
	public static String table_deviceStatus;
	public static String table_sensorTrigHist;
	public static String table_subscriberEventTimeDetail;
	public static String table_SubscriberEventDetail;
	public static String table_TeleHealthcareFlow__SubscriberEventDef;
	public static String table_TeleHealthcareFlow__assignedCareTaker;
	public static String table_TeleHealthcareFlow__CareTaker;
	public static String table_TeleHealthcareFlow__Event;
	public static String table_TeleHealthcareFlow__Alerts;
	public static String table_TeleHealthcareFlow__EventAction;
	public static String table_TeleHealthcareFlow__Subscriber;
	public static String table_TeleHealthcareFlow__Settings;
	public static String topic= "/topics/ihealthcare";//"/topics/ihealthcare";
	public static long minute = 60*1000;
	public static long dayMills = 24l*60l*60l*1000l;
	static int inactiveTimeGap = 10*60*1000;// Current set up 10 minutes
	public static int SENSOR_LOC_INSIDE_BATHROOM =0;
	public static int SENSOR_LOC_OUTSIDE_BATHROOM =1;
	
	public static String EVENT_STATUS_MISSED = "missed";
	public static String EVENT_STATUS_DETECTED = "detected";
	public static String EVENT_STATUS_FAIL = "fail";
	
	public static String ALERTGEN_ONEVENT ="1";
	public static String ALERTGEN_ONMISSED = "0";
	public static String ALERTGEN_ONBOTH = "2";
	public static String ALERTGEN_NA = "NA";
	
	public static int ALERTSTATUS_NOTSEND = 0;
	public static int ALERTSTATUS_SEND = 1;
	public static int ALERTSTATUS_DELIVERED = 2;
	public static int ALERTSTATUS_NOTDELIVERED= 4;
	public static int ALERTSTATUS_OPENED = 3;
	public static int ALERTSTATUS_CLOSED = 5;
	
	
	
	public static long START_OF_2019 = 1546300800000l;
	public static int idleTime = 120000;
	
	public static String version = "V20-2019-04-08-1";
	//V10-2019-02-12-1
	//Modified event genaration based on event history , rather than pre-processed
	//By resetting lastProcessedTime in eventdetails  components can be forced to do the processing again
	//Modified both wakeup an inactivity detection to use this method
	//V11-2019-02-14-1
	//Bathroom Break Detection Component added
	//V12
	//Adjusted event desc text send through firebase message
	//Included a status tag in firebase message
	//V13
	//Reduced Log
	//Bathroom and Inactivity events  reads timeout from table
	//V14
	//Merged wakeup and breakfast components
	//V15 -2019-03-08
	//Bathroom time out is minutes. Missed to convert to millis. This is fixed
	//V16 -2019-03-11
	//Wakeup/breakfast already reported check -event time range limited between start of event def time and curtime+idletime
	//V16-2019-03-14-1
	//EventActionHistory Not diplaying issue- found that by mistake a new event UUID created for each call to insert action history
	//V17-2019-03-15-1
	//Increased the priority of firebase message to high
	//V18-2019-03-20-1
	//Corrected log time stamp which displayed minutes in Month
	//GenerateEvent field in eventdetails  is stored as string by web team, modified code to manage that
	//V19-2019-03-25-1
	//found this chnage missing in the code ->Increased the priority of firebase message to high 
	//added it now
	//V20-2019-04-08-1
	//Workflow component is added and moved alert generations to this component to make it more structured.
	//Implemented Alert Delivery and Tracking. Alert if not received by app in 20 seconds , resend it.

	
	public static void init() {
		table_deviceTable = tenant + "_TeleHealthcareFlow__Device";
		table_deviceStatus = tenant +  "_TelehealthcareFlow__SensorStatus";
		table_sensorTrigHist = tenant + "_TeleHealthcareFlow__SensorTrigHistory";
		table_subscriberEventTimeDetail = tenant + "_TeleHealthcareFlow__SubscriberEventTimeDetail";
		table_SubscriberEventDetail = tenant + "_TeleHealthcareFlow__SubscriberEventDetail";
		table_TeleHealthcareFlow__SubscriberEventDef =  tenant + "_TeleHealthcareFlow__SubscriberEventDef";
		table_TeleHealthcareFlow__Event = tenant + "_TeleHealthcareFlow__Event";
		table_TeleHealthcareFlow__Subscriber = tenant + "_TeleHealthcareFlow__Subscriber";
		table_TeleHealthcareFlow__EventAction = tenant + "_TeleHealthcareFlow__EventActionHistory";
		table_TeleHealthcareFlow__Settings = tenant + "_TeleHealthcareFlow__Settings";
		table_TeleHealthcareFlow__assignedCareTaker= tenant + "_TeleHealthcareFlow__AssignedCareTaker";
		table_TeleHealthcareFlow__CareTaker= tenant + "_TeleHealthcareFlow__CareTaker";
		table_TeleHealthcareFlow__Alerts= tenant + "_TeleHealthcareFlow__Alerts";
		
	}
	
	//public static Connection con;
	
	public static Connection getMySQLCon() {
		Connection con=null;
		try {
			
			con=DriverManager.getConnection(  
					"jdbc:mysql://localhost/smarttest","smarttest","smarttest");
			return con;
			
		} catch (SQLException e) {
			
			CommonUtilities.sendLogToStdout("Local Db Connection Error", "Fall back and try connect dev server");
		}
		try {
		
		con=DriverManager.getConnection(  
							"jdbc:mysql://178.128.165.237:3306/smarttest","bmjo","100$Bill");
		//con=DriverManager.getConnection(  
		//		"jdbc:mysql://178.128.165.237:3306/smarttest","smarttest","smarttest");
	    return con;
		} catch (SQLException e) {
			
			CommonUtilities.sendLogToStdout("Dev Server Db Connection Error", "Failed connect to any db");
			return null;
		}
	}
}
