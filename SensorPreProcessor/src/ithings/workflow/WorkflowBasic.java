package ithings.workflow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.*;

import ithings.common.CommonDataArea;
import ithings.common.CommonUtilities;

public class WorkflowBasic {
	private Connection con;
	public String alertUUid;
	public int count=0;
	public int sendStatus=0;
	public String body;
	public Connection getCon() {
		return con;
	}


	public void setCon(Connection con) {
		this.con = con;
	}

	

	public String getAlertUUid() {
		return alertUUid;
	}


	public void setAlertUUid(String alertUUid) {
		this.alertUUid = alertUUid;
	}
    

	public int getSendStatus() {
		return sendStatus;
	}


	public void setSendStatus(int sendStatus) {
		this.sendStatus = sendStatus;
	}


	public String getBody() {
		return body;
	}


	public void setBody(String body) {
		this.body = body;
	}


	public int getCount() {
		return count;
	}


	public void setCount(int count) {
		this.count = count;
	}
	public boolean unprocessed_data_available = false;
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
	
	public boolean run() {
		processConfiguredWorkFlowEventDefs();
		return true;
	}
	
	void processConfiguredWorkFlowEventDefs() {
		String subsql = "select alertUUId,eventId,status,eventtype,gapTime,missedStatus,eventStatus from  "+ CommonDataArea.table_TeleHealthcareFlow__Alerts + " where status=0 or status=1";
		 
		try {
			Statement stmt = con.createStatement();
		 
		 
				//PreparedStatement preparedStmt = con.prepareStatement(subsql);
				ResultSet subCount  =stmt.executeQuery(subsql);
				System.out.println("sql---:"+subsql);
				while(subCount.next()) {
					 String alertuuid= subCount.getString("alertUUId");
					 String eventid= subCount.getString("eventId");
					 int status= subCount.getInt("status");
					 String eventType=subCount.getString("eventtype");
					 int gap= subCount.getInt("gapTime");
					 String missedStatus= subCount.getString("missedStatus");
					 String eventStatus= subCount.getString("eventStatus");
					// unprocessed_data_available=true;
					 String subsql1= "select eventName,subscriber  from " + CommonDataArea.table_TeleHealthcareFlow__Event
								+ " where eventKey like '" + eventid + "%'";
					 System.out.println(subsql1);
					      try
					      {
					    	  Statement stmt1 = con.createStatement();
					    	  //PreparedStatement preparedStmt1 = con.prepareStatement(subsql1);
								ResultSet subCount1  =stmt1.executeQuery(subsql1);
								while(subCount1.next())
								{
									String belongsTo=subCount1.getString("eventName");
									String subscriber=subCount1.getString("subscriber");
									String subsql2= "select eventName,category  from " + CommonDataArea.table_TeleHealthcareFlow__SubscriberEventDef
											+ " where name like '" + belongsTo + "%' and  subscriber like '" + subscriber + "%'";
								      try
								      {
								    	  Statement stmt2 = con.createStatement();
								    	  //PreparedStatement preparedStmt2 = con.prepareStatement(subsql2);
											ResultSet subCount2  =stmt2.executeQuery(subsql2);
											while(subCount2.next())
											{
												String eventName=subCount2.getString("eventName");
												String category=subCount2.getString("category");
												
												if(status==0)// notsend
												{
													
													TimeUnit.SECONDS.sleep(10);
													sendStatus=alertMonitor(alertuuid);
													
													count=0;
												
													while(sendStatus == 0 && count<3)
													{
														TimeUnit.SECONDS.sleep(20);
														boolean success=sendUserEventNotification(category, eventid, eventName, subscriber,alertuuid,eventType,gap,missedStatus,eventStatus);
														if(success)
														{
															updateAlert(eventid,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_SEND,alertuuid,count);
															updateEventAction(eventid,subscriber,"Assigned","Alert send to");
														}
														else
														{
															updateAlert(eventid,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_NOTSEND,alertuuid,count);
														}
														
														sendStatus = alertMonitor(alertuuid);
														
														count++; 
														if(count==3)
														{
															TimeUnit.SECONDS.sleep(20);
															if(sendStatus == 0)
															{
															boolean a=updateAlertNotDeliver(eventid,CommonDataArea.ALERTSTATUS_NOTSEND,alertuuid,count);	
															}
															 count=0;
															 sendStatus = alertMonitor(alertuuid);
															 
											
														}
														
													}
													
													
													
													
												}
												else if(status==1)//not delivered
												{
													TimeUnit.SECONDS.sleep(10);
													sendStatus=alertMonitor(alertuuid);
													
													count=0;
												
													while(sendStatus == 1 && count<3)
													{
														TimeUnit.SECONDS.sleep(20);
														boolean success=sendUserEventNotification(category, eventid, eventName, subscriber,alertuuid,eventType,gap,missedStatus,eventStatus);
														if(success)
														{
															updateAlert(eventid,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_SEND,alertuuid,count);
															updateEventAction(eventid,subscriber,"Assigned","Alert send to");
														}
														else
														{
															updateAlert(eventid,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_SEND,alertuuid,count);
														}
														sendStatus = alertMonitor(alertuuid);
													
														count++; 
														
														if(count==3)
														{
															TimeUnit.SECONDS.sleep(20);
															   if(sendStatus==1)
															   {
															boolean a=updateAlertNotDeliver(eventid,CommonDataArea.ALERTSTATUS_NOTDELIVERED,alertuuid,count);	
														 
															 
															   }
															   count=0;
																 sendStatus = alertMonitor(alertuuid);
															 
														}
														
													}
													
													
													
													
													
												}
												
											}
								    	  
								      }
								      catch (Exception e) {
								    	  e.printStackTrace();
									}
									
									
									
									
									
									
								}
					    	  
					      }
					      catch (Exception e) {
					    	  e.printStackTrace();
						}
					 
					 
					 
					 
					
				}
				 if(!unprocessed_data_available)
						con.close();	
				
	}
		 catch(Exception exp) {
			 exp.printStackTrace();
		 }
	}


	public boolean assignEventInact(String eventKey,String subscriber,String event,String eventName,int inactiveGapMin)
	{
		
		
		try {
			String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__assignedCareTaker + " where subscriber like '"+subscriber+"%' and ___smart_state___='active'";
				//PreparedStatement preparedStmt = con.prepareStatement(sql);
			Statement stmt = con.createStatement();
				ResultSet caretakercount  =stmt.executeQuery(sql);
				
		
				while(caretakercount.next()) {
					
					String takerid = caretakercount.getString("aCaretakerId");
					String takerphone = caretakercount.getString("caretaker");
					int takerpriority = caretakercount.getInt("priority");
					unprocessed_data_available=true;
					alertUUid = UUID.randomUUID().toString();
					insertAlert(alertUUid,eventKey,takerphone,System.currentTimeMillis(),event,eventName,inactiveGapMin,null,null);
					updateEventAction(eventKey,subscriber,"To be Assign","Alert to be sent to");
					CommonUtilities.sendLogToStdout("TakerId--"+takerid, "takerphone--"+takerphone);
					
					 
					 try {
						 String subsql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Subscriber + " where emailId like '"+subscriber+"%'";
							//PreparedStatement preparedStmt1 = con.prepareStatement(subsql);
						 Statement stmt1 = con.createStatement();
							ResultSet subCount  =stmt1.executeQuery(subsql);
							
							while(subCount.next()) {
								String name = subCount.getString("name");
								String phone = subCount.getString("phone");
								String sub_photo = subCount.getString("sub_photo");
								String topic ="/topics/ihealthcare";
							    body = "Subscriber ->"+ name + " is Inactive for last "+ inactiveGapMin +" minutes";
								//BMJO to do read IP from  a config file or table
								String imageUrl =sub_photo;// "http://178.128.165.237//php//photos//" +phone+ ".jpg";
								boolean success=CommonUtilities.sendPushNotification(event, body,eventName,eventKey, name, subscriber, phone, imageUrl,"detected",alertUUid);
								if(success)
								{
									updateAlert(eventKey,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_SEND,alertUUid,count);
									updateEventAction(eventKey,subscriber,"Assigned","Alert send to");
								}
								else
								{
									updateAlert(eventKey,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_NOTSEND,alertUUid,count);
								}
							
							
															
							}
							
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return false;
						} 
					 if(!unprocessed_data_available)
							con.close();	 
				}
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false; 
			
			}
		return true;
	}
	
	public boolean assignEventBathroom(String eventKey,String subscriber,String event,String evntName,boolean missed, String status)
	{                         
		
		
		try {
			String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__assignedCareTaker + " where subscriber like '"+subscriber+"%' and ___smart_state___='active'";
				//PreparedStatement preparedStmt = con.prepareStatement(sql);
			Statement stmt = con.createStatement();
				ResultSet caretakercount  =stmt.executeQuery(sql);
				
		
				while(caretakercount.next()) {
					
					String takerid = caretakercount.getString("aCaretakerId");
					String takerphone = caretakercount.getString("caretaker");
					int takerpriority = caretakercount.getInt("priority");
					unprocessed_data_available=true;
					alertUUid = UUID.randomUUID().toString();
					String missedStatus="";
					if(missed==true)
					{
						missedStatus="true";	
					}
					else
					{
						missedStatus="false";
					}
					System.out.println(eventKey+"---"+evntName);
					insertAlert(alertUUid,eventKey,takerphone,System.currentTimeMillis(),event,evntName,0,missedStatus,status);
					updateEventAction(eventKey,subscriber,"To be Assign","Alert to be sent to");
					CommonUtilities.sendLogToStdout("TakerId--"+takerid, "takerphone--"+takerphone);
					
					 
					 try {
						 String subsql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Subscriber + " where emailId like '"+subscriber+"%'";
							//PreparedStatement preparedStmt1 = con.prepareStatement(subsql);
						 Statement stmt1 = con.createStatement();
							ResultSet subCount  =stmt1.executeQuery(subsql);
							
							while(subCount.next()) {
								String name = subCount.getString("name");
								String phone = subCount.getString("phone");
								String sub_photo = subCount.getString("sub_photo");
								String topic ="/topics/ihealthcare";
								body = "Subscriber ->" + name + " A Bathroom Event Detected ";
								if(missed) body ="Subscriber ->" + name + " A MISSED Bathroom Event Detected ";
								// BMJO to do read IP from a config file or table
								String imageUrl =sub_photo;// "http://178.128.165.237//php//photos//" +phone+ ".jpg";
								boolean success=CommonUtilities.sendPushNotification(event, body, evntName,eventKey,name, subscriber, phone,
										imageUrl,status,alertUUid);
							
								if(success)
								{
									updateAlert(eventKey,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_SEND,alertUUid,count);
									updateEventAction(eventKey,subscriber,"Assigned","Alert send to");
									
								}
								else
								{
									updateAlert(eventKey,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_NOTSEND,alertUUid,count);
								}
								
								
								
								
								
								
								
							}
							
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return false;
						}
					 
				}
				if(!unprocessed_data_available)
					con.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			
			}
		return true;
	}
	
	
	public boolean assignEventWake(String eventKey,String subscriber,String event,String evntName,boolean missed, String status)
	{                         
		
		
		try {
			String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__assignedCareTaker + " where subscriber like '"+subscriber+"%' and ___smart_state___='active'";
				//PreparedStatement preparedStmt = con.prepareStatement(sql);
			Statement stmt = con.createStatement();
				ResultSet caretakercount  =stmt.executeQuery(sql);
				
		
				while(caretakercount.next()) {
					
					String takerid = caretakercount.getString("aCaretakerId");
					String takerphone = caretakercount.getString("caretaker");
					int takerpriority = caretakercount.getInt("priority");
					unprocessed_data_available=true;
					alertUUid = UUID.randomUUID().toString();
					String missedStatus="";
					if(missed==true)
					{
						missedStatus="true";	
					}
					else
					{
						missedStatus="false";
					}
					System.out.println(eventKey+"---"+evntName);
					insertAlert(alertUUid,eventKey,takerphone,System.currentTimeMillis(),event,evntName,0,missedStatus,status);
					updateEventAction(eventKey,subscriber,"To be Assign","Alert to be sent to");
					CommonUtilities.sendLogToStdout("TakerId--"+takerid, "takerphone--"+takerphone);
					
					 
					 try {
						 String subsql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Subscriber + " where emailId like '"+subscriber+"%'";
							
						 Statement stmt1 = con.createStatement();
						 ResultSet subCount  =stmt1.executeQuery(subsql);
							
							while(subCount.next()) {
								String name = subCount.getString("name");
								String phone = subCount.getString("phone");
								String sub_photo = subCount.getString("sub_photo");
								String topic ="/topics/ihealthcare";
								//String body ="";
								if(missed)
									body = "Subscriber ->"+ name + " A Missed"+ event +" detected ";
								else 
									body = "Subscriber ->"+ name + " A "+ event +" detected ";
								//BMJO to do read IP from  a config file or table
								String imageUrl = sub_photo;
								boolean success=CommonUtilities.sendPushNotification(event, body, evntName,eventKey,name, subscriber, phone,
										imageUrl,status,alertUUid);
							
								if(success)
								{
									
									updateAlert(eventKey,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_SEND,alertUUid,count);
									updateEventAction(eventKey,subscriber,"Assigned","Alert send to");
								
								}
								else
								{
									updateAlert(eventKey,System.currentTimeMillis(),CommonDataArea.ALERTSTATUS_NOTSEND,alertUUid,count);
								}
								
								
								
								
							}
							
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							return false;
						}
					 
				}
				if(!unprocessed_data_available)
					con.close();	
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			
			}
		return true;
	}
	
	
	
	boolean insertAlert(String alertUUid,String eventId,String careTakerId,long curTime,String eventType,String eventName,int gap,String missedStatus,String eventStatus) {
		try {
		 
		 String sql = "INSERT INTO "+ CommonDataArea.table_TeleHealthcareFlow__Alerts + " (alertUUId,eventId,assignedTo,alertType,assignedTime,status,eventName,eventtype,gapTime,missedStatus,eventStatus) values (?,?,?,?,?,?,?,?,?,?,?)";
		 PreparedStatement preparedStmt = con.prepareStatement(sql);
		 preparedStmt.setString(1, alertUUid);
		 preparedStmt.setString(2, eventId);
		 preparedStmt.setString(3,careTakerId);
		 preparedStmt.setString(4, "firebase");
		 preparedStmt.setLong(5, curTime);
		 preparedStmt.setInt(6, CommonDataArea.ALERTSTATUS_NOTSEND);
		 preparedStmt.setString(7, eventName);
		 preparedStmt.setString(8, eventType);
		 preparedStmt.setInt(9,gap);
		 preparedStmt.setString(10,missedStatus);
		 preparedStmt.setString(11,eventStatus);
		 
		 preparedStmt.execute();	
		 return true;
		}
		catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
		} 
	}
	

	boolean updateAlert(String eventId,long curTime,int status,String alertUUid,int count) {
		 try
		 {
			 
		  String query = "update "+ CommonDataArea.table_TeleHealthcareFlow__Alerts +" set sendTime =?,status=?,retryCount=? where eventId =? and alertUUId=?";
		   PreparedStatement preparedStmt = con.prepareStatement(query);
	      preparedStmt.setLong  (1, curTime);
	      preparedStmt.setInt(2,status);
	      preparedStmt.setInt(3,count);
	      preparedStmt.setString(4, eventId);
	      preparedStmt.setString(5, alertUUid);
	     
	      preparedStmt.executeUpdate();
	      
	      return true;
		 }catch(Exception exp) {
			 return false;
		 }
	}

	boolean updateAlertNotDeliver(String eventId,int status,String alertUUid,int count) {
		 try
		 {
			 
		  String query = "update "+ CommonDataArea.table_TeleHealthcareFlow__Alerts +" set status=4,retryCount='"+count+"' where eventId ='"+eventId+"' and alertUUId='"+alertUUid+"'";
		  Statement stmt2 = con.createStatement();
			 stmt2.executeUpdate(query);
		  //PreparedStatement preparedStmt = con.prepareStatement(query);
	      //preparedStmt.setInt(1,status);
	      //preparedStmt.setString(2, eventId);
	      //preparedStmt.setString(3, alertUUid);
	      //preparedStmt.setString(4, careTakerId);
	      
	     // preparedStmt.executeUpdate();
	      
	      return true;
		 }catch(Exception exp) {
			 return false;
		 }
	}
	
	int alertMonitor(String alertId)
	{
		
		 
		 try {
			 String subsql = "select status from  "+ CommonDataArea.table_TeleHealthcareFlow__Alerts + " where alertUUId like '"+alertId+"'";
			 int status=0;
				PreparedStatement preparedStmt1 = con.prepareStatement(subsql);
				ResultSet subCount  =preparedStmt1.executeQuery();
				
				while(subCount.next()) {
					 status= subCount.getInt("status");
					
				}
			return status;	
	}
		 catch(Exception exp) {
			 System.out.println(exp);
			 return 0; 
		 }
		
		 
	}
	
	boolean updateEventAction(String eventId,String subscriber,String status,String note) {
		
		
		
		try {
			String mail;
			
			
			String sql = "select caretaker from  "+ CommonDataArea.table_TeleHealthcareFlow__assignedCareTaker + " where subscriber like '"+subscriber+"%' and ___smart_state___='active' order by priority asc limit 1";
				PreparedStatement preparedStmt = con.prepareStatement(sql);
				ResultSet caretakercount  =preparedStmt.executeQuery();
				
		
				while(caretakercount.next()) {
					
					String takerphone = caretakercount.getString("caretaker");
					
					String sql1="select email,name from "+ CommonDataArea.table_TeleHealthcareFlow__CareTaker + " where phone like '"+takerphone+"'";
					  try {
						  
						  PreparedStatement preparedStmt1 = con.prepareStatement(sql1);
							ResultSet taker  =preparedStmt1.executeQuery();
							
					
							while(taker.next()) {
								
								mail=taker.getString("email");
								String name=taker.getString("name");
								try
								 {
									 
								  String query = "update "+ CommonDataArea.table_TeleHealthcareFlow__EventAction +" set assignedTo=?,actionStatus =?,actionNote=? where eventKey =?";
								
								  PreparedStatement preparedStmt2 = con.prepareStatement(query);
								  preparedStmt2.setString(1, mail);
							      preparedStmt2.setString(2, status);
							      preparedStmt2.setString(3, note+" "+name );
							      preparedStmt2.setString(4, eventId);
							     
							      preparedStmt2.executeUpdate();
							      
							      return true;
								 }catch(Exception exp) {
									 return false;
								 }
								
								
							}
						  
					  }
					  catch (Exception e) {
						// TODO: handle exception
						  e.printStackTrace();
					}
					

					
				}
		return true;		
		}
		catch(Exception exp) {
			 return false; 
		 }

	}
	                                                             
	
	boolean sendUserEventNotification(String event,String eventKey, String evntName, String subscriber,String alertUUid,String eventType,int gap,String missedStatus,String eventStatus) {
		
		  
		 
		 try {
			 String sql = "select * from  "+ CommonDataArea.table_TeleHealthcareFlow__Subscriber + " where emailId like '"+subscriber+"%'";
				PreparedStatement preparedStmt = con.prepareStatement(sql);
				ResultSet activityCount  =preparedStmt.executeQuery();
				
				while(activityCount.next()) {
					String name = activityCount.getString("name");
					String phone = activityCount.getString("phone");
					String sub_photo = activityCount.getString("sub_photo");
					String topic ="/topics/ihealthcare";
					String body="";
					String category="";
				
					  if (event.equals("Inactivity"))
					  {
					body = "Subscriber ->"+ name + " is Inactive for last "+ gap +" minutes";
					category="InAactivityAlert";
					  }
					  else if(event.equals("BathroomBreak"))
					  {
						  body = "Subscriber ->"+ name + " A Bathroom Event Detected ";
						  if(missedStatus=="true")
					        {
							  body ="Subscriber ->" + name + " A MISSED Bathroom Event Detected ";
					        }
						  else
						  {
							  body = "Subscriber ->"+ name + " A Bathroom Event Detected "; 
						  }
						  category="BathroomBreak";
					  }
					  else if(event.equals("WakeUp"))
					  {
						 
						   
						  if(missedStatus=="true")
						  {
								body = "Subscriber ->"+ name + " A Missed WakeUp Event Detected ";
						  }
							else 
							{
								body = "Subscriber ->"+ name + " A WakeUp Event Detected ";
							}
						  
						  
						  category="WakeUpAlert";
					  }
					  else if(event.equals("Breakfast"))
					  {
						  body = "Subscriber ->"+ name + " A Breakfast Event Detected ";
						  
						  
						  if(missedStatus=="true")
						  {
								body = "Subscriber ->"+ name + " A Missed Breakfast Event Detected ";
						  }
							else 
							{
								body = "Subscriber ->"+ name + " A Breakfast Event Detected ";
							}
						  category="BreakfastAlert";
					  }
					
					//BMJO to do read IP from  a config file or table
					String imageUrl =sub_photo;// "http://178.128.165.237//php//photos//" +phone+ ".jpg";
					CommonUtilities.sendPushNotification(eventType,body,evntName,eventKey,name,subscriber, phone, imageUrl,eventStatus,alertUUid);
					return true;
				}
				return false;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
	}
}	
