package genad1_HVDC.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

//import org.eclipse.paho.client.mqttv3.MqttClient;
//import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
//import org.eclipse.paho.client.mqttv3.MqttException;
//import org.eclipse.paho.client.mqttv3.MqttMessage;
//import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
//import org.springframework.transaction.annotation.Isolation;
//import org.springframework.transaction.annotation.Propagation;
//import org.springframework.transaction.annotation.Transactional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

//import kr.co.mems.main.model.DataModelDetailVO;
//import kr.co.mems.main.model.DataSetsVO;
//import kr.co.mems.main.model.IedInfoVO;
//import kr.co.mems.main.model.ReportsVO;
//import kr.co.mems.main.service.MainService;
//import kr.co.mems.report.controller.ReportController;
//import kr.co.mems.report.model.ReportRequestVO;
//import kr.co.mems.report.model.RptVar;
//import kr.co.mems.utils.BeanUtils;
//import kr.co.mems.utils.Commons;

public class Reports extends Thread {
	
	Commons commons = new Commons();
	CommonsStr commonsStr = new CommonsStr();

	private RptVar sharedRptVar;

	private Commons checkLib;
	
	private Boolean stoptag = false;
	private Boolean isrunning = false;
	private Boolean skipAndQuit = false;
	
	private Boolean HighResTransF_spdc1 = false;
	private Boolean HighResTransF_spdc2 = false;
	
	private String mqttIp = "172.30.1.14"; // mqtt.tnmtech.com // 172.30.1.14
	private Integer mqttPort = 1883;
	private String protocolUrl = "tcp";
	
	private String clientUniqueId = "";
	private String rptId = "";
	private String iedIp = "";
	private Integer iedPort = 0;
	private String sessionId = "";
	private String iedIndex = "";
	
	private String HighResTransF_filename = "";
	private String event_datetime = "";
	private String reportsName = "";
	private String clientIp = "";
	private Integer clientPort = 0;
	
	private String kepcoHome = commonsStr.kepcoHome;
	
	public void setRptVar(RptVar rptVar) {
		sharedRptVar = rptVar;
	}
	
	public Reports() {
		// 서비스 Bean을 가져온다! 이렇게 사용.
//		reportController = (ReportController) BeanUtils.getBean("ReportController");
//		checkLib = (Commons) BeanUtils.getBean("Commons");
//		mainService = (MainService) BeanUtils.getBean("MainService");
	}
	
	@SuppressWarnings({ "unchecked", "resource" })
	@Override
	public void run() {
		
		try {
			System.out.println("!!!!!=============== REPORT (" + reportsName + ")  =================!!!!!");
			System.out.println("Report id (" + this.getId() + ") started to listen");
			System.out.println("Report id ::: " + rptId);
			
			isrunning = true;
			// if (!stoptag) break;
			int rn = 0;
			
			String finalMsg = "";
			
			while (!stoptag) {
				// System.out.println("alived!! isRecvFinish : " + sharedRptVar.isRecvFinish() + ", isMax : " + sharedRptVar.isMax());
				if (sharedRptVar.isRecvFinish() && sharedRptVar.isEmptyQueue()) {
					System.out.println("REPORT ( " + reportsName + " ) sharedRptVar.isRecvFinish() : " + sharedRptVar.isRecvFinish());
					System.out.println("REPORT ( " + reportsName + " ) sharedRptVar.isEmptyQueue() : " + sharedRptVar.isEmptyQueue());
					break;
				}
				if (skipAndQuit) {
					System.out.println("skipAndQuit");
					break;
				}
				if (sharedRptVar.isEmptyQueue()) {
					if (sharedRptVar.isRecvFinish()) {
						stoptag = true; 
						System.out.println("[REPORT( " + reportsName + " )] stoptag = true");
					}
					continue; // ready for get
				}
				
				finalMsg = sharedRptVar.getOneRptQueue();
//				System.out.println("[REPORT] ****************** rptId ::: " + rptId + "   finalMsg ::: " + finalMsg);
				
				// 실시간 데이터(RTTransF)가 아닌경우에만 로그기록
//				if(finalMsg.contains("EvtTransF") || finalMsg.contains("HighResTransF") || finalMsg.contains("TrendTransF")) {
					System.out.println("[REPORT( " + reportsName + " )] LinkedListQueue() Size : " + sharedRptVar.getCountReportsQueue());
					System.out.println("[REPORT( " + reportsName + " )] ****************** finalMsg ::: " + finalMsg);
//				}
				
				if (finalMsg.indexOf("IED_RESPONSE_OK") > -1) continue;
				
				try {
					
					// make jsonObject to get information of report
					JsonParser jsonParser = new JsonParser();
					JsonObject jsonObject = (JsonObject) jsonParser.parse(finalMsg);
					JsonObject dataObj = jsonObject.get("response").getAsJsonObject();
					
					// binary 파일과 쌍을 이루는 response 파일을 생성할때 사용.
					JsonObject final_jsonObject = (JsonObject) jsonParser.parse(finalMsg);
					JsonObject final_dataObj = final_jsonObject.get("response").getAsJsonObject();
					
					// value, reason
					JsonArray respValue = dataObj.get("value").getAsJsonArray();
//					JsonArray reasons = dataObj.get("reason for inclusion").getAsJsonArray();
					
					JsonArray respDataRf = null;
					if (dataObj.has("data-reference")) respDataRf = dataObj.get("data-reference").getAsJsonArray();
					if (respDataRf != null) {
						
						// Report response 파일을 1개만 생성하기위해 사용.
//						boolean fileNotExist = true;
							
						for (int j = 0; j<respDataRf.size(); j++) {
							
							// GNDMUGLU01/SCBR2$ST$EvtTransF
							String tmpDataRf = respDataRf.get(j).getAsString();
							String[] tmpRf2 = tmpDataRf.split("[$]"); // GNDMUGLU01/SCBR2, ST, EvtTransF
							String firstStr = tmpRf2[0];	// GNDMUGLU01/SCBR2
							String fileLocation = firstStr.split("/")[1].toLowerCase(); // scbr1, scbr2 ...
							String fileType = tmpRf2[2];	// RTTransF, EvtTransF, TrendTransF
							String datatype_comment = "";
							
							// 이벤트, 트렌드, 실시간 레포트가 아닐경우 skip
							// SBSH, SIML ( 레포트의 value값으로 파일 생성 )
							if(!fileType.equals("EvtTransF") && !fileType.equals("TrendTransF") && !fileType.equals("RTTransF") && !finalMsg.contains("SBSH") && !finalMsg.contains("SIML")) {
								continue;
							}
							System.out.println("----> 레포트 fileType : " + fileType);
							
							// EEName
							// e.g) K_J9999_GLU101_CH01_CBOP_9999001
							Map eenameMap = sharedRptVar.getEenameMap();
							String eename = eenameMap.get(firstStr + ".EEName.location") != null ? (String)eenameMap.get(firstStr + ".EEName.location") : "";
							System.out.println("----> eename : " + eename);
							
							String[] eenameArray = eename.split("_", -1);
							
							String tempValue = respValue.get(j).getAsString();
							tempValue = tempValue.split(":::")[1];
							tempValue = tempValue.replace("{", "");
							tempValue = tempValue.replace("}", "");
							String[] valueArray = tempValue.split(",", -1);
//							String[] timeArray = valueArray[2].split("[.]");
//							String dateTime = timeArray[0];
							
							String unixTimeString = valueArray[0];
							
							SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
							Date date = new Date(); // SBSH (MLU 부싱 진단장치), SIML (MLU 유중가스 분석장치_DGA)
							
							//=======================================================================
							//   SBSH (MLU 부싱 진단장치), SIML (MLU 유중가스 분석장치_DGA)가 아니고
							//   이벤트, 트렌드, 실시간 레포트인 경우
							//=======================================================================
							if(!finalMsg.contains("SBSH") && !finalMsg.contains("SIML")) {
								
								// Unix타임 변환
								long unixTime = Long.parseLong(unixTimeString);
								date = new Date(unixTime*1000L);
//								sdf.setTimeZone(TimeZone.getTimeZone("GMT+9"));
							}
							String dateTime = sdf.format(date);
							
							
							// K_J9999_GLU101_CH01_CBOP_9999001
							// "G101_01\\EVENT\\2024\\03\\20\\K_J9999_GLU101_CH01_CBOP_9999001_22_20240320143400.dat";
							String fileFullPath = "";
							String fileName = "";
							String fileName_binary = "";
							String fileName_reportResponse = "";
							
							String thirdStr = eenameArray[2];
							if(thirdStr.startsWith("GLU")) {
								fileFullPath = "G" + thirdStr.replace("GLU", "");
							} else if(thirdStr.startsWith("MLU")) {
								fileFullPath = "M" + thirdStr.replace("MLU", "");
							}
							
							// CBOP SWPD BSHCUR TRDGA TCMOT TCPD TRPD
							String sensorKind = eenameArray[4];
							
							fileFullPath += "_" + eenameArray[3].replace("CH", "");
							
							if(sensorKind.equals("BSHCUR")) {
								fileFullPath += "\\VALUE";
							} else if(sensorKind.equals("TRDGA")) {
								fileFullPath += "\\VALUE";
							} else if(fileType.equals("EvtTransF")) {
								fileFullPath += "\\EVENT";
							} else if(fileType.equals("TrendTransF")) {
								fileFullPath += "\\TREND";
							} else if(fileType.equals("RTTransF")) {
								fileFullPath += "\\REALTIME";
							} else {
								fileFullPath += "\\VALUE";
							}
							
							fileFullPath += "\\" + dateTime.substring(0, 4) + "\\" + dateTime.substring(4, 6) + "\\" + dateTime.substring(6, 8);
							
							String sensorKind_num = "";
							if(sensorKind.equals("CBOP")) {
								if(fileType.equals("EvtTransF")) {
									sensorKind_num = "22";
								} else if(fileType.equals("TrendTransF")) {
									sensorKind_num = "21";
								} else if(fileType.equals("RTTransF")) {
									sensorKind_num = "20";
								}
							} else if(sensorKind.equals("SWPD")) {
								if(fileType.equals("EvtTransF")) {
									sensorKind_num = "02";
								} else if(fileType.equals("TrendTransF")) {
									sensorKind_num = "01";
								} else if(fileType.equals("RTTransF")) {
									sensorKind_num = "00";
								}
							} else if(sensorKind.equals("BSHCUR")) {
								sensorKind_num = "99";
							} else if(sensorKind.equals("TRDGA")) {
								sensorKind_num = "99";
							} else if(sensorKind.equals("TCMOT")) {
								if(fileType.equals("EvtTransF")) {
									sensorKind_num = "42";
								} else if(fileType.equals("TrendTransF")) {
									sensorKind_num = "41";
								} else if(fileType.equals("RTTransF")) {
									sensorKind_num = "40";
								}
							} else if(sensorKind.equals("TCPD")) {
								if(fileType.equals("EvtTransF")) {
									sensorKind_num = "72";
								} else if(fileType.equals("TrendTransF")) {
									sensorKind_num = "71";
								} else if(fileType.equals("RTTransF")) {
									sensorKind_num = "70";
								}
							} else if(sensorKind.equals("TRPD")) {
								if(fileType.equals("EvtTransF")) {
									sensorKind_num = "12";
								} else if(fileType.equals("TrendTransF")) {
									sensorKind_num = "11";
								} else if(fileType.equals("RTTransF")) {
									sensorKind_num = "10";
								}
							}
							
							// value값을 파일로 생성하는 경우에는 파일 끝에 레포트 이름 append
							if(finalMsg.contains("SBSH_Measurand") || finalMsg.contains("SIML_Measurand")) {
								fileName += eename + "_" + sensorKind_num + "_" + dateTime + "_" + "measurand.dat";
							} else if(finalMsg.contains("SBSH_Status") || finalMsg.contains("SIML_Status")) {
								fileName += eename + "_" + sensorKind_num + "_" + dateTime + "_" + "status.dat";
							} else if(finalMsg.contains("SBSH_SettingValues")) {
								fileName += eename + "_" + sensorKind_num + "_" + dateTime + "_" + "settingvalues.dat";
							} else if(finalMsg.contains("SIML_Diagnostic") || finalMsg.contains("SBSH_Diagnostic")) {
								fileName += eename + "_" + sensorKind_num + "_" + dateTime + "_" + "diagnostic.dat";
							} else {
								// binary 파일
								fileName += eename + "_" + sensorKind_num + "_" + dateTime + ".dat";
								fileName_binary += eename + "_" + sensorKind_num + "_" + dateTime + "_binary.dat";
//								fileName_reportResponse += eename + "_" + sensorKind_num + "_" + dateTime + "_" + unixTimeString + ".dat";
								
								// Report response 파일
								if(finalMsg.contains("SCBR_Measurand") || finalMsg.contains("SPDC_Measurand") || finalMsg.contains("SLTC_Measurand")) {
									fileName_reportResponse += eename + "_" + sensorKind_num + "_" + dateTime + "_measurand.dat";
								} else if(finalMsg.contains("SCBR_Status") || finalMsg.contains("SPDC_Status") || finalMsg.contains("SLTC_Status")) {
									fileName_reportResponse += eename + "_" + sensorKind_num + "_" + dateTime + "_status.dat";
								} else if(finalMsg.contains("SCBR_SettingValues") || finalMsg.contains("SPDC_SettingValues") || finalMsg.contains("SLTC_SettingValues")) {
									fileName_reportResponse += eename + "_" + sensorKind_num + "_" + dateTime + "_settingvalues.dat";
								} else if(finalMsg.contains("SCBR_Diagnostic") || finalMsg.contains("SPDC_Diagnostic") || finalMsg.contains("SLTC_Diagnostic")) {
									fileName_reportResponse += eename + "_" + sensorKind_num + "_" + dateTime + "_diagnostic.dat";
								}
							}
							
							System.out.println("fileFullPath ::: " + fileFullPath);
							System.out.println("fileName ::: " + fileName);
							System.out.println("fileName_binary ::: " + fileName_binary);
							System.out.println("fileName_reportResponse ::: " + fileName_reportResponse);
							
							// 폴더 생성
							String fileFullPath_local = fileFullPath.replace("\\", "/");
							String uploadDir = kepcoHome + "upload/" + fileFullPath_local + "/";
							String completeDir = kepcoHome + "complete/" + fileFullPath_local + "/";
//							
							File uploadDirFile = new File(uploadDir);
							File completeDirFile = new File(completeDir);
							
							if(!uploadDirFile.exists()) {
								uploadDirFile.mkdirs();
							}
							if(!completeDirFile.exists()) {
								completeDirFile.mkdirs();
							}
							
							//=======================================================================
							//   FILE_GET을 이용하지않고 value값으로 파일 생성
							//   SBSH (MLU 부싱 진단장치), SIML (MLU 유중가스 분석장치_DGA)
							//=======================================================================
							if(finalMsg.contains("SBSH") || finalMsg.contains("SIML")) {
								
								// 파일 생성
								FileWriter fileWriter = new FileWriter(uploadDir + fileName); //  /home/KEPCO_iec61850/upload/M101_01/VALUE/2024/03/29/K_J9999_MLU101_CH01_BSHCUR_9999201_99_20240329143243_measurand.dat
								PrintWriter printWriter = new PrintWriter(fileWriter);
								printWriter.println(finalMsg);
								printWriter.close();
								
								// 파일 이동
								File uploadFile = new File(uploadDir + fileName);		//	/home/KEPCO_iec61850/upload/M101_01/VALUE/2024/03/29/K_J9999_MLU101_CH01_BSHCUR_9999201_99_20240329143243_measurand.dat
								File completeFile = new File(completeDir + fileName);	//	/home/KEPCO_iec61850/complete/M101_01/VALUE/2024/03/29/K_J9999_MLU101_CH01_BSHCUR_9999201_99_20240329143243_measurand.dat
								uploadFile.renameTo(completeFile);
								
								// 레포트 json 전체를 파일로 저장 후 빠져나감.
								break;
							} 
							//=======================================================================
							//   FILE_GET을 이용해 파일 생성
							//=======================================================================
							else {
								
								// request block
								JsonObject requestObject = new JsonObject();
								
								// command block
								JsonObject commandObj = new JsonObject();
								commandObj.addProperty("commandtype", "select"); 
								commandObj.addProperty("datatype_fc", ""); 
								commandObj.addProperty("datatype", "");
								commandObj.addProperty("datatype_comment", ""); 
								commandObj.addProperty("key", "");
								commandObj.addProperty("value", "FILE_GET");
								
								// control block
								JsonObject fileObj = new JsonObject();
								fileObj.addProperty("filename", fileFullPath + "\\" + fileName); // G101_01\\EVENT\\2024\\03\\20\\K_J9999_GLU101_CH01_CBOP_9999001_22_20240320143400.dat
								fileObj.addProperty("file_command", "DOWNLOAD"); 
								fileObj.addProperty("file_size", "");
								
								requestObject.add("command", commandObj);
								requestObject.add("file_control", fileObj);
								requestObject.addProperty("client_unique_id", clientUniqueId);
								
								requestObject.addProperty("downloadPath", uploadDir);
								String receiveString = commons.socketConnection_file_get(iedIp, iedPort, clientIp, clientPort, requestObject, reportsName, event_datetime);
								
								if (receiveString != null && !receiveString.equals("")) {
//								System.out.println("receiveString ::: " + receiveString);
									
									jsonParser = new JsonParser();
									jsonObject = (JsonObject)jsonParser.parse(receiveString);
									String status = jsonObject.get("status").getAsString();
									// String responseSecurekey = jsonObject.get("securekey").getAsString();
									String message = jsonObject.get("message").getAsString();
									
									if(status.equals("Y")) {
										
										// bin 파일 이동
										File uploadFile = new File(uploadDir + fileName);		//	/home/KEPCO_iec61850/upload/G101_01/EVENT/2024/03/20/K_J9999_GLU101_CH01_CBOP_9999001_22_20240320143400.dat
										File completeFile = new File(completeDir + fileName_binary);	//	/home/KEPCO_iec61850/complete/G101_01/EVENT/2024/03/20/K_J9999_GLU101_CH01_CBOP_9999001_22_20240320143400_binary.dat
										uploadFile.renameTo(completeFile);
										
//										if(fileNotExist) {
											
											// Report response 파일을 1개만 생성하기위해 사용.
//											fileNotExist = false;
										
											// Report response 데이터중 현재 값만 남기기.
											JsonArray final_respDataRf = new JsonArray();
											JsonArray final_respValue = new JsonArray();
											
											// 같은 채널의 속성을 전부 추가
											for (int k = 0; k<respDataRf.size(); k++) {
												
												String tmpDataRf2 = respDataRf.get(k).getAsString();

												// e.g) firstStr = GNDMUGLU01/SCBR2
												if(tmpDataRf2.startsWith(firstStr + "$")) {
													String tempValue2 = respValue.get(k).getAsString();
													
													final_respDataRf.add(tmpDataRf2);
													final_respValue.add(tempValue2);
												}
											}
											
											final_dataObj.add("data-reference", final_respDataRf);
											final_dataObj.add("value", final_respValue);
											
											final_jsonObject.add("response", final_dataObj);
											
											System.out.println("final_jsonObject ------> " + final_jsonObject.toString());
											
											// Report response 파일 생성
											FileWriter fileWriter = new FileWriter(uploadDir + fileName_reportResponse); // 
											PrintWriter printWriter = new PrintWriter(fileWriter);
//											printWriter.println(finalMsg);
											printWriter.println(final_jsonObject.toString()); // 현재 값만 파일로 생성
											printWriter.close();
											
											// Report response 파일 이동
											File uploadFile2 = new File(uploadDir + fileName_reportResponse);		//	/home/KEPCO_iec61850/upload/G101_01/EVENT/2024/03/20/K_J9999_GLU101_CH01_CBOP_9999001_22_20240320143400_diagnostic.dat
											File completeFile2 = new File(completeDir + fileName_reportResponse);	//	/home/KEPCO_iec61850/complete/G101_01/EVENT/2024/03/20/K_J9999_GLU101_CH01_CBOP_9999001_22_20240320143400_diagnostic.dat
											uploadFile2.renameTo(completeFile2);
//										}
										
									} else {
										System.out.println("FILE_GET message (" + reportsName + ")  : " + message);
									}
									
								} else {
									System.out.println("FILE_GET message (" + reportsName + ")  : receiveString is empty");
								}
							}
						}
					}
					
				} catch(Exception e) {
					System.out.println("--- normal Error(Reports while inner)");
					e.printStackTrace();
				}
			}
			
		} catch (Exception e) {
			System.out.println("--- normal Error(Reports)");
			e.printStackTrace();
		}
		System.out.println("Report Thread("+this.getId()+") is stopped!!!");
		stoptag = false;
		isrunning = false;
	}
	
	public String getReportsName() {
		return reportsName;
	}

	public void setReportsName(String reportsName) {
		this.reportsName = reportsName;
	}

	public String getClientIp() {
		return clientIp;
	}

	public void setClientIp(String clientIp) {
		this.clientIp = clientIp;
	}

	public Integer getClientPort() {
		return clientPort;
	}

	public void setClientPort(Integer clientPort) {
		this.clientPort = clientPort;
	}

	public String getIedIp() {
		return iedIp;
	}

	public void setIedIp(String iedIp) {
		this.iedIp = iedIp;
	}

	public Integer getIedPort() {
		return iedPort;
	}

	public void setIedPort(Integer iedPort) {
		this.iedPort = iedPort;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getIedIndex() {
		return iedIndex;
	}

	public void setIedIndex(String iedIndex) {
		this.iedIndex = iedIndex;
	}

	public String getRptId() {
		return this.rptId;
	}
	
	public void setRptId(String rptId) {
		this.rptId = rptId;
	}
	
	public String getClientUniqueId() {
		return this.clientUniqueId;
	}
	
	public void setClientUniqueId(String clientUniqueId) {
		this.clientUniqueId = clientUniqueId;
	}

	public Boolean isRunning() {
		return isrunning;
	}
	
	public void setstoptag() {
		stoptag = true;
	}
}
