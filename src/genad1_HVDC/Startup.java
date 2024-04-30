package genad1_HVDC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import genad1_HVDC.utils.Commons;
import genad1_HVDC.utils.CommonsStr;

public class Startup {
	
	public static void main(String[] args) {
		
		SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date2 = new Date();
		String dateStr2 = sdf2.format(date2);
		
		System.out.println("***************************************************************************");
		System.out.println("    Start  (" + dateStr2 + ") ");
		System.out.println("***************************************************************************");

		Commons commons = new Commons();
		CommonsStr commonsStr = new CommonsStr();
		
		try {
			// Client Server실행, IED접속 정보를 args로 받음
//			String clientFileName = commonsStr.clientFileName;
//			String IedConfigFile = commonsStr.IedConfigFile;
			String clientFileName = args[2];
			String clientFilePath = commonsStr.clientFilePath;
			String IedConfigFile = clientFilePath + args[3];
			
			// Client Server 실행정보를 args로 받음
//			String clientIp = commonsStr.clientIp;
//			String clientPort  = commonsStr.clientPort;
			String clientIp = args[0];
			String clientPort  = args[1];
			
			String iedName = "NewIED";
			String ap_title = "1,1,9999,1";
			String ae_qualifier = "12";
			String pdusize = "65000";
			String psel = "00000001";
			String ssel = "0001";
			String tsel = "0001";
			
			Runtime runtime = Runtime.getRuntime();
			
			Map authMap = new HashMap();
			authMap.put("ap_title", ap_title);
			authMap.put("ae_qualifier", ae_qualifier);
			authMap.put("pdusize", pdusize);
			authMap.put("psel", psel);
			authMap.put("ssel", ssel);
			authMap.put("tsel", tsel);
			
			
			// Client Server 실행여부 확인
//			String command2 = commonsStr.clientAliveCommand;
			String command2 = "ps -ef | grep iec61850_client_server | grep " + clientPort + " | grep -v grep";
			Process process2 = runtime.exec(new String[] {"sh", "-c", command2});
			
			BufferedReader br = new BufferedReader(new InputStreamReader(process2.getInputStream()));
			String line = null;
			List clientList = new ArrayList<String>();
			while((line = br.readLine()) != null) {
//				System.out.println("line ---> " + line);
				clientList.add(line);
			}
			System.out.println("clientList.size() ---> " + clientList.size());
			
			if(clientList.size() < 1) {
				
				System.out.println("==============[ Client Server 실행 ]=================");
				String command = clientFilePath + clientFileName + " " + clientFilePath + " " + clientPort;
//				System.out.println("command ::: " + command);
				Process process = runtime.exec(command);
				
				Thread.sleep(3000);
			}
			

			// IED 접속정보 파일 읽기
			BufferedReader br2 = new BufferedReader(new FileReader(IedConfigFile));
			
			String readLine = null;
			while((readLine = br2.readLine()) != null) {
				
				Map eenameMap = new HashMap();
				
				// e.g) 192.168.10.101,102,rcb_SCBR_Diagnostic01,GNDMUGLU01
				//      IED_ip, IED_port, report_name, logical_device??
				String[] iedArray = readLine.split(",");
				String ied_ip = iedArray[0];
				String ied_port = iedArray[1];
				String reportsName_file = iedArray[2];
				String ld = iedArray[3];
				
//				Thread.sleep(2000);
				
				String clientUniqueId = commons.generateConnectionid("jgeosdushs");
				
				System.out.println("==============[ IED Server 연결  ]===========================");
				System.out.println("   IED ip : " + ied_ip);
				System.out.println("   IED port : " + ied_port);
				System.out.println("   Report Name (Config file) : " + reportsName_file);
				System.out.println("   LD : " + ld);
				System.out.println("===========================================================");
				
				System.out.println("iedName : " + iedName);
				System.out.println("clientIp : " + clientIp);
				System.out.println("clientPort : " + clientPort);
				System.out.println("ap_title : " + ap_title);
				System.out.println("ae_qualifier : " + ae_qualifier);
				System.out.println("pdusize : " + pdusize);
				System.out.println("psel : " + psel);
				System.out.println("ssel : " + ssel);
				System.out.println("tsel : " + tsel);

				String message = "";
				String from = "";
				
				String commandtype = "command";
				String datatype = "";
				String datatype_comment = "";
				String datatype_fc = "";
				String key = "";
				String value = "associate";
				
				String receiveString = "";
				
				// IED Server 연결 실패시 5번 다시시도
				for(int i = 1; i < 6; i++) {
					// 소켓통신
					receiveString = commons.socketConnection2(authMap, ied_ip, Integer.parseInt(ied_port), clientIp, Integer.parseInt(clientPort), commandtype, datatype, datatype_comment, datatype_fc, key, value, clientUniqueId);

					if(receiveString == null || receiveString.equals("")) {
						System.out.println(i + " 번째 IED Connection 실패! (receiveString is empty)");
						continue;
					} else {
						JsonParser jsonParser = new JsonParser();
						JsonObject jsonObject = (JsonObject)jsonParser.parse(receiveString);
						String status = jsonObject.get("status").getAsString();
						message = jsonObject.get("message").getAsString();
						from = "[ associate ]";
						
						if(!status.equals("Y")) {
							System.out.println(i + " 번째 IED Connection 실패!! (" + message + ")" );
							continue;
						} else {
							System.out.println(i + " 번째 IED Connection 성공!!!!!");
							break;
						}
					}
				}
				
				if(receiveString != null && !receiveString.equals("")) {
					
					JsonParser jsonParser = new JsonParser();
					JsonObject jsonObject = (JsonObject)jsonParser.parse(receiveString);
					String status = jsonObject.get("status").getAsString();
					String responseSecurekey = jsonObject.get("securekey").getAsString();
					message = jsonObject.get("message").getAsString();
					from = "[ associate ]";
					
					System.out.println("[ associate ] status : " + status + ", message : " + message);
					
					if(status.equals("Y")) {
						
						commandtype = "select";
						datatype_fc = "";
						datatype = "";
						datatype_comment = "";
						key = ld;
						value = "LD";
						
						System.out.println("==========================================================================");
						System.out.println("   key : " + key + " , value : " + value);
						System.out.println("==========================================================================");
						
						receiveString = commons.socketConnection(ied_ip, Integer.parseInt(ied_port), clientIp, Integer.parseInt(clientPort), commandtype, datatype, datatype_comment, datatype_fc, key, value, clientUniqueId);
						List resList = new ArrayList();
						int resListSize = 0;
						
						if(receiveString != null && !receiveString.equals("")) {
							
							jsonParser = new JsonParser();
							jsonObject = (JsonObject)jsonParser.parse(receiveString);
							status = jsonObject.get("status").getAsString();
							responseSecurekey = jsonObject.get("securekey").getAsString();
							message = jsonObject.get("message").getAsString();
							
							System.out.println("message : " + message);
							System.out.println("status : " + status);
							
							if(status.equals("Y")) {
								JsonObject jsonObject2 = jsonObject.get("response").getAsJsonObject();
								JsonArray jsonArray = jsonObject2.get(key).getAsJsonArray();
								
								for(int k = 0; k < jsonArray.size(); k++) {
									
									String tempLn = jsonArray.get(k).getAsString();
									if(!tempLn.equals("LLN0") && !tempLn.equals("LPHD1")) {
										
										commandtype = "select";
										datatype_fc = "DC";
										datatype = "";
										datatype_comment = "";
										key = ld + "/" + tempLn + ".EEName.location";
										value = "DV";
										
										System.out.println("==========================================================================");
										System.out.println("   key : " + key + " , value : " + value);
										System.out.println("==========================================================================");
										
										// 소켓통신
										receiveString = commons.socketConnection(ied_ip, Integer.parseInt(ied_port), clientIp,
												Integer.parseInt(clientPort), commandtype, datatype, datatype_comment, datatype_fc, key,
												value, clientUniqueId);
										
										if (receiveString != null && !receiveString.equals("")) {
											
											jsonParser = new JsonParser();
											jsonObject = (JsonObject) jsonParser.parse(receiveString);
											status = jsonObject.get("status").getAsString();
											responseSecurekey = jsonObject.get("securekey").getAsString();
											message = jsonObject.get("message").getAsString();
											
											System.out.println("message : " + message);
											System.out.println("status : " + status);
												
											if (status.equals("Y")) {
												
												jsonObject2 = jsonObject.get("response").getAsJsonObject();
												
												String resValue = jsonObject2.get(key).getAsString();
												System.out.println("key : " + key + ", EEName : " + resValue);
												
												// e.g) key : GNDMUGLU01/SCBR1.EEName.location, EEName : K_J9999_GLU101_CH01_CBOP_9999001
												eenameMap.put(key, resValue);
											}
										}
									}
								}
							}
						}
						
						String ln = "LLN0";						
						
						commandtype = "select";
						datatype_fc = "";
						datatype = "";
						datatype_comment = "";
						key = ld + "/" + ln;
						value = "BRCB";
						
						System.out.println("==========================================================================");
						System.out.println("   key : " + key + " , value : " + value);
						System.out.println("==========================================================================");
						
						// 소켓통신
						receiveString = commons.socketConnection(ied_ip, Integer.parseInt(ied_port), clientIp,
								Integer.parseInt(clientPort), commandtype, datatype, datatype_comment, datatype_fc, key,
								value, clientUniqueId);
						
						if(receiveString != null && !receiveString.equals("")) {
							
							jsonParser = new JsonParser();
							jsonObject = (JsonObject)jsonParser.parse(receiveString);
							status = jsonObject.get("status").getAsString();
							responseSecurekey = jsonObject.get("securekey").getAsString();
							message = jsonObject.get("message").getAsString();
							
							System.out.println("message : " + message);
							System.out.println("status : " + status);
							
							if(status.equals("Y")) {
								JsonObject jsonObject2 = jsonObject.get("response").getAsJsonObject();
								JsonArray jsonArray = jsonObject2.get(key).getAsJsonArray();
								
//								for(int k = 0; k < 1; k++) {
								for(int k = 0; k < jsonArray.size(); k++) {
									
//										reportsName = "rcb_SCBR_Diagnostic01";
									String reportsName = jsonArray.get(k).getAsString();
									
									// ied 서버에서 전달받은 reportsName과 Config파일의 reportsName이 같다면
									if(reportsName.equals(reportsName_file)) {
										
										String report_rcb_name = "";
										String report_rcb_receiver = "";

										//------------[ BRCB_SET_VALUES ]----------------------------------------------------------
//										datatype_fc = "BR";
//										key = ld + "/" + ln + "." + reportsName;
//										value = "BRCB_SET_VALUES";
//										report_rcb_name = ld + "/" + ln + "." + datatype_fc + "." + reportsName;
//										report_rcb_receiver = ld + "/" + ln + "." + datatype_fc + "." + reportsName;
//										
//										System.out.println("==========================================================================");
//										System.out.println("   key : " + key + " , value : " + value);
//										System.out.println("==========================================================================");
//										
////										clientUniqueId = commons.generateConnectionid("jgeosdushs");
//
//										// request block
//										JsonObject requestObject = new JsonObject();
//										
//										// command block
//										JsonObject commandObj = new JsonObject();
//										commandObj.addProperty("commandtype", "update"); // "select", "update"
//										commandObj.addProperty("datatype_fc", datatype_fc);
//										commandObj.addProperty("datatype", "");
//										commandObj.addProperty("datatype_comment", "");
//										commandObj.addProperty("key", key);
//										commandObj.addProperty("value", value);
//										
//										// reporting block
//										JsonObject reportingObj = new JsonObject();
//										reportingObj.addProperty("report_dataset_name", "@@@@");
//										reportingObj.addProperty("report_rpt_id", "@@@@");
//										reportingObj.addProperty("report_rcb_name", report_rcb_name);
//										reportingObj.addProperty("report_rcb_receiver", report_rcb_receiver);
//										
//										reportingObj.addProperty("report_setDataSetReference", "@@@@");
//										reportingObj.addProperty("report_setRptEna", "@@@@");
//										reportingObj.addProperty("report_setConfRev", "@@@@");
//										reportingObj.addProperty("report_setDataSet", "@@@@");
//										reportingObj.addProperty("report_setBufTm", "0");		//  @@@@  or  0
//										reportingObj.addProperty("report_setPurgeBuf", "@@@@");
//										reportingObj.addProperty("report_setIntgPd", "60000");		//  @@@@  or  6000(6초)
//										reportingObj.addProperty("report_setEntryID", "@@@@");
//										
//										reportingObj.addProperty("report_setResv", "@@@@");
//										reportingObj.addProperty("report_setResvTms", "@@@@");
//										
//										reportingObj.addProperty("report_optflds_bitsize", "@@@@");
//										reportingObj.addProperty("report_optflds_sequence_number", "@@@@");
//										reportingObj.addProperty("report_optflds_report_time_stamp", "@@@@");
//										reportingObj.addProperty("report_optflds_reason_for_inclusion", "@@@@");
//										reportingObj.addProperty("report_optflds_data_set_name", "@@@@");
//										reportingObj.addProperty("report_optflds_data_reference", "@@@@");
//										reportingObj.addProperty("report_optflds_buffer_overflow", "@@@@");
//										reportingObj.addProperty("report_optflds_entryID", "@@@@");
//										reportingObj.addProperty("report_optflds_conf_version", "@@@@");
//										reportingObj.addProperty("report_optflds_segmnt", "@@@@");
//										
//										reportingObj.addProperty("report_trgops_bitsize", "6");
//										reportingObj.addProperty("report_trgops_dchg", "0"); // 제나드에서는 Data change 만 지원함 		//  1  or  0
//										reportingObj.addProperty("report_trgops_dupd", "0");
//										reportingObj.addProperty("report_trgops_qchg", "0");
//										reportingObj.addProperty("report_trgops_intg", "1");  	//  0  or  1
//										reportingObj.addProperty("report_trgops_gi", "1"); 		//  0  or  1
//										
//										requestObject.add("command", commandObj);
//										requestObject.add("reporting", reportingObj);
//										requestObject.addProperty("client_unique_id", clientUniqueId);
//										
//										receiveString = "";
//										receiveString = commons.socketConnection_report(ied_ip, Integer.parseInt(ied_port), clientIp, Integer.parseInt(clientPort), requestObject, reportsName, "", eenameMap);
//										
//										if (receiveString != null && !receiveString.equals("")) {
////												System.out.println("receiveString ::: " + receiveString);
//											
//											jsonParser = new JsonParser();
//											jsonObject = (JsonObject)jsonParser.parse(receiveString);
//											status = jsonObject.get("status").getAsString();
//											responseSecurekey = jsonObject.get("securekey").getAsString();
//											message = jsonObject.get("message").getAsString();
//											from = "[ SET REPORT VALUES ]";
//											String data = "";
//											
//											if(status.equals("Y")) {
//												data = jsonObject.get("response").getAsString();
//												System.out.println("response string ::: " + data);
												//--------------[ BRCB_SET_VALUES - End ]--------------------------------------------------------
												
												commandtype = "select";
												datatype_fc = "BR";
												datatype = "";
												datatype_comment = "";
												key = ld + "/" + ln + "." + reportsName;
												value = "GET_BRCB_VALUES";
												String[] valueArray = {};
												
												System.out.println("==========================================================================");
												System.out.println("   key : " + key + " , value : " + value);
												System.out.println("==========================================================================");
												
												// 소켓통신
												receiveString = commons.socketConnection(ied_ip, Integer.parseInt(ied_port), clientIp,
														Integer.parseInt(clientPort), commandtype, datatype, datatype_comment, datatype_fc, key,
														value, clientUniqueId);
												
												if (receiveString != null && !receiveString.equals("")) {
													
													jsonParser = new JsonParser();
													jsonObject = (JsonObject) jsonParser.parse(receiveString);
													status = jsonObject.get("status").getAsString();
													responseSecurekey = jsonObject.get("securekey").getAsString();
													message = jsonObject.get("message").getAsString();
													from = "[ getBRCBValues - " + key + " ]";
													
													System.out.println("message : " + message);
													System.out.println("status : " + status);
													
													if (status.equals("Y")) {
														
														JsonObject jsonObject3 = jsonObject.get("response").getAsJsonObject();
														String resValue = jsonObject3.get(key).getAsString();
														
														// {HVDC/LLN0$SPDCStat_brcb,false,TEMPLATE_IED_TEST/LLN0$HVDC_SPDC_Stat,1,0111111110,1000,160,010000,5000,false,false,9a5b13619e450a00,20210819014109.039Z}
														// {GNDMUGLU01/LLN0.BR.SPDC_Status,false,GNDMUGLU01/LLN0$SPDC_Status,1,0111110110,0,0,010001,5000,false,false,18b8f36505000000,20240315025900.948Z}
														System.out.println("resValue ---> " + resValue);
														
														String tempValue = resValue.replace("{", "");
														tempValue = tempValue.replace("}", "");
														valueArray = tempValue.split(",", -1);
														int valueLength = valueArray.length;
														System.out.println("resValue Length ---> " + valueLength);
														
														String reportId = valueArray[0];	// HVDC/LLN0$SPDCStat_brcb
														String dataSet = valueArray[2]; 	// TEMPLATE_IED_TEST/LLN0$HVDC_SPDC_Stat
														
														System.out.println("reportId : " + reportId);
														System.out.println("dataSet : " + dataSet);
														
														value = "BRCB_SET_ENA";
														report_rcb_name = ld + "/" + ln + "." + datatype_fc + "." + reportsName;
														report_rcb_receiver = ld + "/" + ln + "." + datatype_fc + "." + reportsName;
														
														System.out.println("==========================================================================");
														System.out.println("   key : " + key + " , value : " + value);
														System.out.println("==========================================================================");
														
//														clientUniqueId = commons.generateConnectionid("jgeosdushs");
														
														// request block
														JsonObject requestObject2 = new JsonObject();
														
														// command block
														JsonObject commandObj2 = new JsonObject();
														commandObj2.addProperty("commandtype", "update");
														commandObj2.addProperty("datatype_fc", "");
														commandObj2.addProperty("datatype", "");
														commandObj2.addProperty("datatype_comment", "");
														commandObj2.addProperty("key", key);
														commandObj2.addProperty("value", value);
														
														// reporting block
														JsonObject reportingObj2 = new JsonObject();
														reportingObj2.addProperty("report_dataset_name", dataSet.replace("$", "."));
														reportingObj2.addProperty("report_rpt_id", reportId);
														reportingObj2.addProperty("report_rcb_name", report_rcb_name);
														reportingObj2.addProperty("report_rcb_receiver", report_rcb_receiver);
														
														reportingObj2.addProperty("report_setDataSetReference", dataSet);
														reportingObj2.addProperty("report_setRptEna", "1");
														reportingObj2.addProperty("report_setConfRev", "@@@@");
														reportingObj2.addProperty("report_setDataSet", dataSet);
														reportingObj2.addProperty("report_setBufTm", "@@@@");
														reportingObj2.addProperty("report_setPurgeBuf", "@@@@");
														reportingObj2.addProperty("report_setIntgPd", "@@@@");
														reportingObj2.addProperty("report_setEntryID", "@@@@");
														
														reportingObj2.addProperty("report_optflds_bitsize", "@@@@");
														reportingObj2.addProperty("report_optflds_sequence_number", "@@@@");
														reportingObj2.addProperty("report_optflds_report_time_stamp", "@@@@");
														reportingObj2.addProperty("report_optflds_reason_for_inclusion", "@@@@");
														reportingObj2.addProperty("report_optflds_data_set_name", "@@@@");
														reportingObj2.addProperty("report_optflds_data_reference", "@@@@");
														reportingObj2.addProperty("report_optflds_buffer_overflow", "@@@@");
														reportingObj2.addProperty("report_optflds_entryID", "@@@@");
														reportingObj2.addProperty("report_optflds_conf_version", "@@@@");
														reportingObj2.addProperty("report_optflds_segmnt", "@@@@");
														
														reportingObj2.addProperty("report_trgops_bitsize", "@@@@");
														reportingObj2.addProperty("report_trgops_dchg", "@@@@");
														reportingObj2.addProperty("report_trgops_dupd", "@@@@");
														reportingObj2.addProperty("report_trgops_qchg", "@@@@");
														reportingObj2.addProperty("report_trgops_intg", "@@@@");
														reportingObj2.addProperty("report_trgops_gi", "@@@@");
														
														requestObject2.add("command", commandObj2);
														requestObject2.add("reporting", reportingObj2);
														requestObject2.addProperty("client_unique_id", clientUniqueId);
														
														receiveString = "";
														receiveString = commons.socketConnection_report(ied_ip, Integer.parseInt(ied_port), clientIp, Integer.parseInt(clientPort), requestObject2, reportsName, "", eenameMap);
													}
												}
												//---------------[ BRCB_SET_VALUES ]----------------------------------------------------------
//											}
//										}
										//--------------------[ BRCB_SET_VALUES - End ]-----------------------------------------------------
									}
								}
							}
						}
						
						
						
					} else {
						System.out.println("associate message  : " + message);
					}
					
				} else {
					System.out.println("associate message  : receiveString is empty");
				}
			}
			
			System.out.println("***************************************************************************");
			System.out.println("    PD End     ");
			System.out.println("***************************************************************************");
			
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}
