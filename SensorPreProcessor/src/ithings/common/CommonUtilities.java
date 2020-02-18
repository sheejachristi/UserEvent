package ithings.common;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

public class CommonUtilities {
	public final static String AUTH_KEY_FCM = "AAAAA48uNUU:APA91bFk__efkmr3Qy-C8wBQpXCEdW6vfM6Kmu-rfYQeQJW5HLUbr2uYZc7UhbXRPs2sr6VzCYIiy_E3VovNgHAipdknyqgrH5E0yr5ewiJaDSsBXfAIUFQIAfu8FA46HdUGFHXID_A6";
	public final static String API_URL_FCM = "https://fcm.googleapis.com/fcm/send";
	 
    public static boolean sendPushNotification(String deviceToken, String title, String body) {
    	 try {
	        String result = "";
	        URL url = new URL(API_URL_FCM);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	 
	        conn.setUseCaches(false);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	 
	        conn.setRequestMethod("POST");
	        conn.setRequestProperty("Authorization", "key=" + AUTH_KEY_FCM);
	        conn.setRequestProperty("Content-Type", "application/json");
	 
	        JSONObject json = new JSONObject();
	       // json.put("click_action",".SubscriberActivity");
	        json.put("to", deviceToken.trim());
	        json.put("priority", "high");
	        
	        JSONObject info = new JSONObject();
	        info.put("title", title); // Notification title
	        info.put("body", body); // Notification
	                                                                // body
	        json.put("data", info);
	        //json.put("time_to_live", "86400");
	       
	            OutputStreamWriter wr = new OutputStreamWriter(
	                    conn.getOutputStream());
	            wr.write(json.toString());
	            wr.flush();
	 
	            BufferedReader br = new BufferedReader(new InputStreamReader(
	                    (conn.getInputStream())));
	 
	            String output;
	            System.out.println("Output from Server .... \n");
	            while ((output = br.readLine()) != null) {
	                System.out.println(output);
	            }
	           return true;
	        } catch (Exception e) {
	            e.printStackTrace();
	            return false;
	        }
		}
    
    public static boolean sendPushNotification( String title, String body,String eventName,String eventKey,String name, String email, String phone, String imageUrl, String status,String alertUUId) {
   	 try {
	        String result = "";
	        String deviceToken = CommonDataArea.topic;
	        URL url = new URL(API_URL_FCM);
	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	 
	        conn.setUseCaches(false);
	        conn.setDoInput(true);
	        conn.setDoOutput(true);
	 
	        conn.setRequestMethod("POST");
	        conn.setRequestProperty("Authorization", "key=" + AUTH_KEY_FCM);
	        conn.setRequestProperty("Content-Type", "application/json");
	 
	        JSONObject json = new JSONObject();
	       // json.put("click_action",".SubscriberActivity");
	        json.put("to", deviceToken.trim());
	        JSONObject info = new JSONObject();
	        info.put("title", title); // Notification title
	        info.put("body", body); // Notification
	        info.put("eventName", eventName); // Notification
	        info.put("eventKey", eventKey); // Notification
	        info.put("subscriberName", name);
	        info.put("email", email);
	        info.put("phone", phone);
	        info.put("status", status);
	        info.put("imageURL", imageUrl);
	        info.put("alertUUId", alertUUId);
	        info.put("timeMills", System.currentTimeMillis());
	                                                                // body
	        json.put("data", info);
	        json.put("priority","high");
	        //json.put("time_to_live", "86400");
	            OutputStreamWriter wr = new OutputStreamWriter(
	                    conn.getOutputStream());
	            wr.write(json.toString());
	            wr.flush();
	 
	            BufferedReader br = new BufferedReader(new InputStreamReader(
	                    (conn.getInputStream())));
	 
	            String output;
	            System.out.println("Output from Server .... \n");
	            while ((output = br.readLine()) != null) {
	                System.out.println(output);
	            }
	           CommonUtilities.sendLogToStdout("Firebase Mesg Det","Event Name ->"+eventName +" Title ->"+title +" body ->"+body+" Eventkey ->"+eventKey
+"subscriberName->"+ name +" email-> "+email+" phone->"+phone+" alertUUId->"+alertUUId);
		       return true;
	        } catch (Exception e) {
	            e.printStackTrace();
	            return false;
	            
	        }
		}
    
    
    public static boolean sendPushNotification( String title, String body,String eventName,String eventKey,String name, String email, String phone, String imageUrl, String status) {
      	 try {
   	        String result = "";
   	        String deviceToken = CommonDataArea.topic;
   	        URL url = new URL(API_URL_FCM);
   	        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
   	 
   	        conn.setUseCaches(false);
   	        conn.setDoInput(true);
   	        conn.setDoOutput(true);
   	 
   	        conn.setRequestMethod("POST");
   	        conn.setRequestProperty("Authorization", "key=" + AUTH_KEY_FCM);
   	        conn.setRequestProperty("Content-Type", "application/json");
   	 
   	        JSONObject json = new JSONObject();
   	       // json.put("click_action",".SubscriberActivity");
   	        json.put("to", deviceToken.trim());
   	        JSONObject info = new JSONObject();
   	        info.put("title", title); // Notification title
   	        info.put("body", body); // Notification
   	        info.put("eventName", eventName); // Notification
   	        info.put("eventKey", eventKey); // Notification
   	        info.put("subscriberName", name);
   	        info.put("email", email);
   	        info.put("phone", phone);
   	        info.put("status", status);
   	        info.put("imageURL", imageUrl);
   	
   	        info.put("timeMills", System.currentTimeMillis());
   	                                                                // body
   	        json.put("data", info);
   	        json.put("priority","high");
   	        //json.put("time_to_live", "86400");
   	            OutputStreamWriter wr = new OutputStreamWriter(
   	                    conn.getOutputStream());
   	            wr.write(json.toString());
   	            wr.flush();
   	 
   	            BufferedReader br = new BufferedReader(new InputStreamReader(
   	                    (conn.getInputStream())));
   	 
   	            String output;
   	            System.out.println("Output from Server .... \n");
   	            while ((output = br.readLine()) != null) {
   	                System.out.println(output);
   	            }
   	           CommonUtilities.sendLogToStdout("Firebase Mesg Det","Event Name ->"+eventName +" Title ->"+title +" body ->"+body+" Eventkey ->"+eventKey
   +"subscriberName->"+ name +" email-> "+email+" phone->"+phone);
   		       return true;
   	        } catch (Exception e) {
   	            e.printStackTrace();
   	            return false;
   	            
   	        }
   		}
    
    
    
    public static void  sendLogToStdout(String tag, String desc) {
    	Date date = new Date();		
    	DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss ->");
    	String strDate = dateFormat.format(date);

    	System.out.println(strDate+ tag +":::"+  desc);
    	 
    }
    
    public static String readSettingsString(String itemName, Connection con) {
    	String sql = "select StringVal from  " + CommonDataArea.table_TeleHealthcareFlow__Settings + " where Name like '"
				+ itemName + "%'";
		try {
			System.out.println(sql);
			PreparedStatement preparedStmt = con.prepareStatement(sql);
			ResultSet activityCount = preparedStmt.executeQuery();

			while (activityCount.next()) {
				String val = activityCount.getString("StringVal");		
				return val;
			}
			return null;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
    }
}
