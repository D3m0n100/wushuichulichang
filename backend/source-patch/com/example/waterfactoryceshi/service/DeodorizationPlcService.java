package com.example.waterfactoryceshi.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DeodorizationPlcService extends AbstractS7200SmartPlcService {
   private static final long DEFAULT_PULSE_MS = 300L;
   private static final Set<String> PULSE_PARAMETERS = Collections.unmodifiableSet(new HashSet(Arrays.asList("DEO_FAN_START_V3_4", "DEO_FAN_STOP_V3_5", "DEO_PREWASH_PUMP_1_STOP_V0_3", "DEO_PREWASH_PUMP_2_START_V0_4", "DEO_PREWASH_PUMP_2_STOP_V0_5", "DEO_HUMIDIFIER_RESET_V1000_6", "DEO_BIO_HEATER_MANUAL_STOP_V4_6", "DEO_BIO_HEATER_MANUAL_START_V4_5", "DEO_PREWASH_HEATER_MANUAL_STOP_V4_2", "DEO_PREWASH_HEATER_MANUAL_START_V4_1", "DEO_PREWASH_FILL_OPEN_V6_1", "DEO_PREWASH_FILL_CLOSE_V6_4", "DEO_PREWASH_FILL_STOP_V6_2", "DEO_PREWASH_DRAIN_OPEN_V7_1", "DEO_PREWASH_DRAIN_CLOSE_V7_4", "DEO_PREWASH_DRAIN_STOP_V7_2", "DEO_BIO_FILL_OPEN_V8_1", "DEO_BIO_FILL_CLOSE_V8_4", "DEO_BIO_FILL_STOP_V8_2", "DEO_BIO_DRAIN_OPEN_V10_1", "DEO_BIO_DRAIN_CLOSE_V10_4", "DEO_BIO_DRAIN_STOP_V10_2")));
   private static final Set<String> TOGGLE_PARAMETERS = Collections.unmodifiableSet(new HashSet(Arrays.asList("DEO_HUMIDIFIER_MANUAL_V9_6", "DEO_HUMIDIFIER_MODE_SWITCH_V20_0", "DEO_BIO_HEATER_MODE_SWITCH_V4_4", "DEO_PREWASH_HEATER_MODE_SWITCH_V4_0", "DEO_PREWASH_FILL_MODE_SWITCH_V6_0", "DEO_PREWASH_DRAIN_MODE_SWITCH_V7_0", "DEO_BIO_FILL_MODE_SWITCH_V8_0", "DEO_BIO_DRAIN_MODE_SWITCH_V10_0", "DEO_TOUCHSCREEN_ESTOP_M0_1")));

   public DeodorizationPlcService(DeodorizationPlcConnectionPool connectionPool, @Value("${plc.deodorization.ip}") String plcIp, @Value("${plc.deodorization.rack:0}") int rack, @Value("${plc.deodorization.slot:1}") int slot) {
      super(connectionPool, "Deodorization", "S7-200 SMART SR40", plcIp, rack, slot);
   }

   protected void initDefinitions() {
      this.registerReadOnlyMotorDevice("DEO_FAN", "风机", 0, 1, 0, 2);
      this.registerReadOnlyMotorDevice("DEO_PREWASH_PUMP_1", "预洗一泵", 0, 3, 0, 4);
      this.registerReadOnlyMotorDevice("DEO_PREWASH_PUMP_2", "预洗二泵", 0, 5, 0, 6);
      this.registerReadOnlyMotorDevice("DEO_BIO_PUMP", "生物泵", 0, 7, 1, 0);
      this.registerReadOnlyMotorDevice("DEO_PREWASH_HEATER", "预洗电加热", 1, 1, (Integer)null, (Integer)null);
      this.registerReadOnlyMotorDevice("DEO_BIO_HEATER", "生物电加热", 1, 2, (Integer)null, (Integer)null);
      this.registerReadOnlyValveDevice("DEO_PREWASH_FILL_VALVE", "预洗补水阀", 1, 3, 1, 4);
      this.registerReadOnlyValveDevice("DEO_PREWASH_DRAIN_VALVE", "预洗排水阀", 1, 5, 1, 6);
      this.registerReadOnlyValveDevice("DEO_BIO_FILL_VALVE", "生物补水阀", 1, 7, 2, 0);
      this.registerReadOnlyValveDevice("DEO_BIO_DRAIN_VALVE", "生物排水阀", 2, 1, 2, 2);
      this.registerIntInputParameter("DEO_PREWASH_PH_AIW16", "预洗PH值", 16);
      this.registerIntInputParameter("DEO_BIO_PH_AIW18", "生物PH值", 18);
      this.registerIntInputParameter("DEO_PREWASH_LEVEL_AIW20", "预洗液位", 20);
      this.registerIntInputParameter("DEO_BIO_LEVEL_AIW22", "生物液位", 22);
      this.registerIntInputParameter("DEO_PREWASH_TEMP_AIW24", "预洗温度", 24);
      this.registerIntInputParameter("DEO_BIO_TEMP_AIW26", "生物温度", 26);
      this.registerIntInputParameter("DEO_WIND_PRESSURE_AIW28", "风压值", 28);
      this.registerIntInputParameter("DEO_FAN_FREQ_AIW30", "风机频率", 30);
      this.registerIntInputParameter("DEO_H2S_AIW32", "硫化氢", 32);
      this.registerIntInputParameter("DEO_NH3_AIW34", "氨气", 34);
      this.registerIntInputParameter("DEO_PREWASH_FLOW_AIW36", "预洗流量", 36);
      this.registerIntInputParameter("DEO_BIO_FLOW_AIW38", "生物流量", 38);
      this.registerBoolInputParameter("DEO_ESTOP_I0_0", "急停", 0, 0);
      this.registerBoolInputParameter("DEO_FAN_RUNNING_I0_1", "风机运行", 0, 1);
      this.registerBoolInputParameter("DEO_FAN_FAULT_I0_2", "风机故障", 0, 2);
      this.registerBoolInputParameter("DEO_PREWASH_1_RUNNING_I0_3", "预洗一运行", 0, 3);
      this.registerBoolInputParameter("DEO_PREWASH_1_FAULT_I0_4", "预洗一故障", 0, 4);
      this.registerBoolInputParameter("DEO_PREWASH_2_RUNNING_I0_5", "预洗二运行", 0, 5);
      this.registerBoolInputParameter("DEO_PREWASH_2_FAULT_I0_6", "预洗二故障", 0, 6);
      this.registerBoolInputParameter("DEO_BIO_PUMP_RUNNING_I0_7", "生物泵运行", 0, 7);
      this.registerBoolInputParameter("DEO_BIO_PUMP_FAULT_I1_0", "生物泵故障", 1, 0);
      this.registerBoolInputParameter("DEO_PREWASH_HEATER_RUNNING_I1_1", "预洗电加热运行", 1, 1);
      this.registerBoolInputParameter("DEO_BIO_HEATER_RUNNING_I1_2", "生物电加热运行", 1, 2);
      this.registerBoolInputParameter("DEO_PREWASH_FILL_OPEN_FB_I1_3", "预洗补水阀开到位", 1, 3);
      this.registerBoolInputParameter("DEO_PREWASH_FILL_CLOSE_FB_I1_4", "预洗补水阀关到位", 1, 4);
      this.registerBoolInputParameter("DEO_PREWASH_DRAIN_OPEN_FB_I1_5", "预洗排水阀开到位", 1, 5);
      this.registerBoolInputParameter("DEO_PREWASH_DRAIN_CLOSE_FB_I1_6", "预洗排水阀关到位", 1, 6);
      this.registerBoolInputParameter("DEO_BIO_FILL_OPEN_FB_I1_7", "生物补水阀开到位", 1, 7);
      this.registerBoolInputParameter("DEO_BIO_FILL_CLOSE_FB_I2_0", "生物补水阀关到位", 2, 0);
      this.registerBoolInputParameter("DEO_BIO_DRAIN_OPEN_FB_I2_1", "生物排水阀开到位", 2, 1);
      this.registerBoolInputParameter("DEO_BIO_DRAIN_CLOSE_FB_I2_2", "生物排水阀关到位", 2, 2);
      this.registerBoolOutputParameter("DEO_FAN_Q0_0", "风机启停", 0, 0, false);
      this.registerBoolOutputParameter("DEO_PREWASH_PUMP_1_Q0_1", "预洗一泵启停", 0, 1, false);
      this.registerBoolOutputParameter("DEO_PREWASH_PUMP_2_Q0_2", "预洗二泵启停", 0, 2, false);
      this.registerBoolOutputParameter("DEO_BIO_PUMP_Q0_3", "生物泵启停", 0, 3, false);
      this.registerBoolOutputParameter("DEO_PREWASH_HEATER_Q0_4", "预洗电加热启停", 0, 4, false);
      this.registerBoolOutputParameter("DEO_BIO_HEATER_Q0_5", "生物电加热启停", 0, 5, false);
      this.registerBoolOutputParameter("DEO_PREWASH_FILL_OPEN_Q0_6", "预洗补水阀开", 0, 6, false);
      this.registerBoolOutputParameter("DEO_PREWASH_FILL_CLOSE_Q0_7", "预洗补水阀关", 0, 7, false);
      this.registerBoolOutputParameter("DEO_PREWASH_DRAIN_OPEN_Q1_0", "预洗排水阀开", 1, 0, false);
      this.registerBoolOutputParameter("DEO_PREWASH_DRAIN_CLOSE_Q1_1", "预洗排水阀关", 1, 1, false);
      this.registerBoolOutputParameter("DEO_BIO_FILL_OPEN_Q1_2", "生物补水阀开", 1, 2, false);
      this.registerBoolOutputParameter("DEO_BIO_FILL_CLOSE_Q1_3", "生物补水阀关", 1, 3, false);
      this.registerBoolOutputParameter("DEO_BIO_DRAIN_OPEN_Q1_4", "生物排水阀开", 1, 4, false);
      this.registerBoolOutputParameter("DEO_BIO_DRAIN_CLOSE_Q1_5", "生物排水阀关", 1, 5, false);
      this.registerBoolOutputParameter("DEO_GENERAL_FAULT_Q1_6", "综合故障", 1, 6, false);
      this.registerBoolVParameter("DEO_FAN_START_V3_4", "风机启动", 3, 4, true);
      this.registerBoolVParameter("DEO_FAN_STOP_V3_5", "风机停止", 3, 5, true);
      this.registerBoolVParameter("DEO_PREWASH_PUMP_1_STOP_V0_3", "预洗一泵停止", 0, 3, true);
      this.registerBoolVParameter("DEO_PREWASH_PUMP_2_START_V0_4", "预洗二泵启动", 0, 4, true);
      this.registerBoolVParameter("DEO_PREWASH_PUMP_2_STOP_V0_5", "预洗二泵停止", 0, 5, true);
      this.registerBoolVParameter("DEO_HUMIDIFIER_MANUAL_V9_6", "加湿泵手动", 9, 6, true);
      this.registerBoolVParameter("DEO_HUMIDIFIER_MODE_SWITCH_V20_0", "加湿泵手自动转换", 20, 0, true);
      this.registerBoolVParameter("DEO_HUMIDIFIER_RESET_V1000_6", "加湿泵故障复位", 1000, 6, true);
      this.registerIntVParameter("DEO_HUMIDIFIER_AUTO_ON_TIME_VW38", "加湿泵自动开时间(秒)", 38, true);
      this.registerIntVParameter("DEO_HUMIDIFIER_AUTO_OFF_TIME_VW36", "加湿泵自动关时间(分)", 36, true);
      this.registerIntVParameter("DEO_HUMIDIFIER_STOP_TIME_DISPLAY_VW3000", "加湿泵停止时间显示", 3000, false);
      this.registerBoolVParameter("DEO_HUMIDIFIER_FAULT_V1000_0", "加湿泵故障", 1000, 0, false);
      this.registerRealVParameter("DEO_BIO_HEATER_AUTO_STOP_VD488", "生物自动加热停止", 488, true);
      this.registerRealVParameter("DEO_BIO_HEATER_AUTO_START_VD484", "生物自动加热启动", 484, true);
      this.registerBoolVParameter("DEO_BIO_HEATER_MANUAL_STOP_V4_6", "生物加热手动启动停止", 4, 6, true);
      this.registerBoolVParameter("DEO_BIO_HEATER_MANUAL_START_V4_5", "生物加热手动启动按钮", 4, 5, true);
      this.registerBoolVParameter("DEO_BIO_HEATER_MODE_SWITCH_V4_4", "生物加热手自动转换", 4, 4, true);
      this.registerRealVParameter("DEO_PREWASH_HEATER_AUTO_STOP_VD480", "预洗自动加热停止", 480, true);
      this.registerRealVParameter("DEO_PREWASH_HEATER_AUTO_START_VD476", "预洗自动加热启动", 476, true);
      this.registerBoolVParameter("DEO_PREWASH_HEATER_MANUAL_STOP_V4_2", "预洗加热手动停止按钮", 4, 2, true);
      this.registerBoolVParameter("DEO_PREWASH_HEATER_MANUAL_START_V4_1", "预洗加热手动启动按钮", 4, 1, true);
      this.registerBoolVParameter("DEO_PREWASH_HEATER_MODE_SWITCH_V4_0", "预洗加热手自动转换", 4, 0, true);
      this.registerBoolVParameter("DEO_PREWASH_FILL_OPEN_V6_1", "预洗补水阀开", 6, 1, true);
      this.registerBoolVParameter("DEO_PREWASH_FILL_CLOSE_V6_4", "预洗补水阀关", 6, 4, true);
      this.registerBoolVParameter("DEO_PREWASH_FILL_STOP_V6_2", "预洗补水阀停", 6, 2, true);
      this.registerBoolVParameter("DEO_PREWASH_FILL_MODE_SWITCH_V6_0", "预洗补水阀手自动转换", 6, 0, true);
      this.registerRealVParameter("DEO_PREWASH_FILL_OPEN_SET_VD492", "预洗补水阀开设定", 492, true);
      this.registerRealVParameter("DEO_PREWASH_FILL_CLOSE_SET_VD496", "预洗补水阀关设定", 496, true);
      this.registerBoolVParameter("DEO_PREWASH_DRAIN_OPEN_V7_1", "预洗排水阀开", 7, 1, true);
      this.registerBoolVParameter("DEO_PREWASH_DRAIN_CLOSE_V7_4", "预洗排水阀关", 7, 4, true);
      this.registerBoolVParameter("DEO_PREWASH_DRAIN_STOP_V7_2", "预洗排水阀停", 7, 2, true);
      this.registerBoolVParameter("DEO_PREWASH_DRAIN_MODE_SWITCH_V7_0", "预洗排水阀手自动转换", 7, 0, true);
      this.registerRealVParameter("DEO_PREWASH_DRAIN_OPEN_SET_VD500", "预洗排水阀开设定", 500, true);
      this.registerRealVParameter("DEO_PREWASH_DRAIN_CLOSE_SET_VD504", "预洗排水阀关设定", 504, true);
      this.registerBoolVParameter("DEO_BIO_FILL_OPEN_V8_1", "生物补水阀开", 8, 1, true);
      this.registerBoolVParameter("DEO_BIO_FILL_CLOSE_V8_4", "生物补水阀关", 8, 4, true);
      this.registerBoolVParameter("DEO_BIO_FILL_STOP_V8_2", "生物补水阀停", 8, 2, true);
      this.registerBoolVParameter("DEO_BIO_FILL_MODE_SWITCH_V8_0", "生物补水阀手自动转换", 8, 0, true);
      this.registerRealVParameter("DEO_BIO_FILL_OPEN_SET_VD508", "生物补水阀开设定", 508, true);
      this.registerRealVParameter("DEO_BIO_FILL_CLOSE_SET_VD512", "生物补水阀关设定", 512, true);
      this.registerBoolVParameter("DEO_BIO_DRAIN_OPEN_V10_1", "生物排水阀开", 10, 1, true);
      this.registerBoolVParameter("DEO_BIO_DRAIN_CLOSE_V10_4", "生物排水阀关", 10, 4, true);
      this.registerBoolVParameter("DEO_BIO_DRAIN_STOP_V10_2", "生物排水阀停", 10, 2, true);
      this.registerBoolVParameter("DEO_BIO_DRAIN_MODE_SWITCH_V10_0", "生物排水阀手自动转换", 10, 0, true);
      this.registerRealVParameter("DEO_BIO_DRAIN_OPEN_SET_VD516", "生物排水阀开设定", 516, true);
      this.registerRealVParameter("DEO_BIO_DRAIN_CLOSE_SET_VD520", "生物排水阀关设定", 520, true);
      this.registerRealVParameter("DEO_PREWASH_PH_VD300", "预洗PH值", 300, false);
      this.registerRealVParameter("DEO_BIO_PH_VD304", "生物PH值", 304, false);
      this.registerRealVParameter("DEO_PREWASH_LEVEL_VD308", "预洗液位", 308, false);
      this.registerRealVParameter("DEO_PREWASH_LEVEL_HIGH_VD400", "预洗液位上限", 400, true);
      this.registerRealVParameter("DEO_PREWASH_LEVEL_LOW_VD404", "预洗液位下限", 404, true);
      this.registerRealVParameter("DEO_BIO_LEVEL_VD312", "生物液位", 312, false);
      this.registerRealVParameter("DEO_BIO_LEVEL_HIGH_VD408", "生物液位上限", 408, true);
      this.registerRealVParameter("DEO_BIO_LEVEL_LOW_VD412", "生物液位下限", 412, true);
      this.registerRealVParameter("DEO_PREWASH_TEMP_VD316", "预洗温度", 316, false);
      this.registerRealVParameter("DEO_PREWASH_TEMP_HIGH_VD416", "预洗温度上限", 416, true);
      this.registerRealVParameter("DEO_PREWASH_TEMP_LOW_VD420", "预洗温度下限", 420, true);
      this.registerRealVParameter("DEO_BIO_TEMP_VD320", "生物温度", 320, false);
      this.registerRealVParameter("DEO_BIO_TEMP_HIGH_VD424", "生物温度上限", 424, true);
      this.registerRealVParameter("DEO_BIO_TEMP_LOW_VD428", "生物温度下限", 428, true);
      this.registerRealVParameter("DEO_WIND_PRESSURE_VD324", "风压值", 324, false);
      this.registerRealVParameter("DEO_WIND_PRESSURE_HIGH_VD432", "风压值上限", 432, true);
      this.registerRealVParameter("DEO_WIND_PRESSURE_LOW_VD436", "风压值下限", 436, true);
      this.registerRealVParameter("DEO_FAN_FREQ_VD328", "风机频率", 328, false);
      this.registerRealVParameter("DEO_H2S_VD332", "硫化氢值", 332, false);
      this.registerRealVParameter("DEO_H2S_HIGH_VD440", "硫化氢上限", 440, true);
      this.registerRealVParameter("DEO_H2S_LOW_VD444", "硫化氢下限", 444, true);
      this.registerRealVParameter("DEO_NH3_VD336", "氨气值", 336, false);
      this.registerRealVParameter("DEO_NH3_HIGH_VD448", "氨气上限", 448, true);
      this.registerRealVParameter("DEO_NH3_LOW_VD452", "氨气下限", 452, true);
      this.registerRealVParameter("DEO_PREWASH_FLOW_VD340", "预洗流量值", 340, false);
      this.registerRealVParameter("DEO_PREWASH_FLOW_HIGH_VD456", "预洗流量上限", 456, true);
      this.registerRealVParameter("DEO_PREWASH_FLOW_LOW_VD460", "预洗流量下限", 460, true);
      this.registerRealVParameter("DEO_BIO_FLOW_VD344", "预生物流量值", 344, false);
      this.registerRealVParameter("DEO_BIO_FLOW_HIGH_VD464", "生物流量上限", 464, true);
      this.registerRealVParameter("DEO_BIO_FLOW_LOW_VD468", "生物流量下限", 468, true);
      this.registerRealVParameter("DEO_FAN_FREQ_SET_VD472", "风机频率设定", 472, true);
      this.registerBoolVParameter("DEO_SYSTEM_ONE_KEY_START_STOP_V2_5", "除臭系统一键启停", 2, 5, true);
      this.registerBoolVParameter("DEO_SYSTEM_MODE_SWITCH_V0_2", "除臭系统手自动转换", 0, 2, true);
      this.registerBoolVParameter("DEO_TOUCHSCREEN_ESTOP_V0_1", "触摸屏急停", 0, 1, true);
      this.registerBoolVParameter("DEO_ESTOP_INDICATOR_V0_0", "急停灯", 0, 0, false);
      this.registerIntVParameter("DEO_SYSTEM_START_DELAY_VW530", "一键启动延时时间", 530, true);
      this.registerIntVParameter("DEO_SYSTEM_STOP_DELAY_VW532", "一键停止延时时间", 532, true);
      this.registerBoolMParameter("DEO_TOUCHSCREEN_ESTOP_M0_1", "触摸屏急停", 0, 1, true);
      this.registerBoolMParameter("DEO_ESTOP_INDICATOR_M0_0", "急停指示", 0, 0, false);
   }

   public boolean writeParameter(String parameterCode, String value) {
      String code = parameterCode == null ? null : parameterCode.toUpperCase();
      if (!PULSE_PARAMETERS.contains(code)) {
         if (TOGGLE_PARAMETERS.contains(code)) {
            String tagCode = code + "_TAG";
            Map<String, Object> values = this.readTags(Collections.singletonList(tagCode));
            Object currentValue = values.get(tagCode);
            if (!(currentValue instanceof Boolean)) {
               throw new IllegalStateException("Unable to read BOOL toggle parameter: " + code);
            } else {
               return this.writeBoolTag(tagCode, !(Boolean)currentValue);
            }
         } else {
            return super.writeParameter(parameterCode, value);
         }
      } else {
         String tagCode = code + "_TAG";
         boolean enabled = "1".equals(value) || "true".equalsIgnoreCase(value);
         return !enabled ? this.writeBoolTag(tagCode, false) : this.pulseBoolTag(tagCode, 300L);
      }
   }

   private void registerReadOnlyMotorDevice(String code, String name, int runningByte, int runningBit, Integer faultByte, Integer faultBit) {
      String runningTag = this.registerBoolInputTag(code + "_RUNNING_TAG", name + "运行", runningByte, runningBit, false);
      String faultTag = faultByte == null ? null : this.registerBoolInputTag(code + "_FAULT_TAG", name + "故障", faultByte, faultBit, false);
      this.registerDevice(code, name, "motor", runningTag, (String)null, (String)null, faultTag);
   }

   private void registerReadOnlyValveDevice(String code, String name, int openFbByte, int openFbBit, int closeFbByte, int closeFbBit) {
      String openFbTag = this.registerBoolInputTag(code + "_OPEN_FB_TAG", name + "开到位", openFbByte, openFbBit, false);
      String closeFbTag = this.registerBoolInputTag(code + "_CLOSE_FB_TAG", name + "关到位", closeFbByte, closeFbBit, false);
      this.registerDevice(code, name, "valve", (String)null, (String)null, (String)null, (String)null, openFbTag, closeFbTag);
   }

   private void registerIntInputParameter(String code, String name, int byteOffset) {
      this.registerParameter(code, name, this.registerIntInputWordTag(code + "_TAG", name, byteOffset, false));
   }

   private void registerBoolInputParameter(String code, String name, int byteOffset, int bitOffset) {
      this.registerParameter(code, name, this.registerBoolInputTag(code + "_TAG", name, byteOffset, bitOffset, false));
   }

   private void registerBoolOutputParameter(String code, String name, int byteOffset, int bitOffset, boolean writable) {
      this.registerParameter(code, name, this.registerBoolOutputTag(code + "_TAG", name, byteOffset, bitOffset, writable));
   }

   private void registerBoolVParameter(String code, String name, int byteOffset, int bitOffset, boolean writable) {
      this.registerParameter(code, name, this.registerBoolVTag(code + "_TAG", name, byteOffset, bitOffset, writable));
   }

   private void registerBoolMParameter(String code, String name, int byteOffset, int bitOffset, boolean writable) {
      this.registerParameter(code, name, this.registerBoolMTag(code + "_TAG", name, byteOffset, bitOffset, writable));
   }

   private void registerIntVParameter(String code, String name, int byteOffset, boolean writable) {
      this.registerParameter(code, name, this.registerIntVTag(code + "_TAG", name, byteOffset, writable));
   }

   private void registerRealVParameter(String code, String name, int byteOffset, boolean writable) {
      this.registerParameter(code, name, this.registerRealVTag(code + "_TAG", name, byteOffset, writable));
   }
}
