package de.fhmue.tobxtreme.v2;

import java.util.UUID;


public class LGS_Constants
{

    //Service UUIDs
    public final static UUID UUID_SERVICE_ENVIRONMENT        = UUID.fromString("7d36eed5-ca05-42f3-867e-4d800a459ca1");
    public final static UUID UUID_SERVICE_CONFIGURATION      = UUID.fromString("7d36eed5-ca05-42f3-867e-4d800a459ca2");
    public final static UUID UUID_SERVICE_FSM                = UUID.fromString("ad42a590-b7af-4082-b8f4-b4b48798e696");

    //Data Characteristics UUIDs
    public final static UUID UUID_CHARACTERISTIC_BRIGHT      = UUID.fromString("c50956f6-cb78-487e-9566-b883ff3e5d53");
    public final static UUID UUID_CHARACTERISTIC_TEMPERATURE = UUID.fromString("b33102eb-43a0-4da1-8183-ed169c0f1720");
    public final static UUID UUID_CHARACTERISTIC_VOC         = UUID.fromString("6bb014e9-a0c1-47b7-939d-f97b8e4f7877");
    public final static UUID UUID_CHARACTERISTIC_CO2         = UUID.fromString("4e1fcadd-cdbf-46bc-8faa-4b06320cfa2c");
    public final static UUID UUID_CHARACTERISTIC_HUMIDITY    = UUID.fromString("4e311cb9-a68b-44b7-aa97-a591190aa08e");
    public final static UUID UUID_CHARACTERISTIC_PRESSURE    = UUID.fromString("666b7e99-e973-4860-9006-c78cb95da5aa");

    //Settings Characteristics UUIDs
    public final static UUID UUID_CHARACTERISTIC_SETTING_REPRATE   = UUID.fromString("286cc204-4b3f-4f82-8ebb-667372b15669");
    public final static UUID UUID_CHARACTERISTIC_SETTING_CRITTEMP  = UUID.fromString("286cc204-4b3f-4f82-8ebb-667372b1566a");
    public final static UUID UUID_CHARACTERISTIC_SETTING_CRITPRES  = UUID.fromString("286cc204-4b3f-4f82-8ebb-667372b1566b");
    public final static UUID UUID_CHARACTERISTIC_SETTING_CRITCO2   = UUID.fromString("286cc204-4b3f-4f82-8ebb-667372b1566c");
    public final static UUID UUID_CHARACTERISTIC_SETTING_CRITHUM   = UUID.fromString("286cc204-4b3f-4f82-8ebb-667372b1566d");
    public final static UUID UUID_CHARACTERISTIC_SETTING_CRITVOC   = UUID.fromString("286cc204-4b3f-4f82-8ebb-667372b1566e");
    public final static UUID UUID_CHARACTERISTIC_SETTING_OUTPUTACT = UUID.fromString("286cc204-4b3f-4f82-8ebb-667372b1566f");

    //FSM Characteristics UUIDs
    public final static UUID UUID_CHARACTERISTIC_REQUEST      = UUID.fromString("ad42a590-b7af-4082-b8f4-b4b48798e697");
    public final static UUID UUID_CHARACTERISTIC_NOTIFYREADY  = UUID.fromString("ad42a590-b7af-4082-b8f4-b4b48798e698");
    public final static UUID UUID_CHARACTERISTIC_READPACKAGE  = UUID.fromString("ad42a590-b7af-4082-b8f4-b4b48798e699");


    public static final UUID UUID_DESCRIPTOR_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    /**
     * Other Constants
     */
    public static final String LGS_DEVICENAME   = "LGS.tb1.0";      //Name des Sensors
}
