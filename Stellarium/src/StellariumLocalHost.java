import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.control.*;
import net.happybrackets.device.HB;
import net.happybrackets.device.sensors.Sensor;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.LinkedHashMap;
import java.util.Map;

public class StellariumLocalHost implements HBAction, HBReset {
    // Change to the number of audio Channels on your device
    final int NUMBER_AUDIO_CHANNELS = 1;

    // These are maximum minimum scaled values for altitude accelerometer
    final double MAX_ACCEL_ALT = 2;
    final double MIN_ACCEL_ALT = 1.000001;

    double currentAz = 0;
    double currentAlt = 0; // zero is level. 1 is Up, -1 is down, 2 is behind

    Object altAzSynchroniser = new Object();
    boolean exitThread = false;
    Robot robot =  null;
    Dimension screensize;


    Object fovSynchroniser = new Object();
    Object timerateSynchroniser = new Object();

    private double fieldOfView  = 1;

    String stellariumDevice = "localhost";


    private double timeRate = 0;
    private Object lrMoveSynchroniser = new Object();
    private Object upMoveSynchroniser = new Object();

    private double LRMovementAmount = 0;
    private double UDMovementAmount =  0;

    // use this control to send back to notify our listeners we have changed FOV
    FloatControl fovReturnControl = null;
    FloatControl postTimeSender;

    String lastAltAz = "";

    @Override
    public void action(HB hb) {
        /***** Type your HBAction code below this line ******/
        // remove this code if you do not want other compositions to run at the same time as this one
        hb.reset();
        hb.setStatus(this.getClass().getSimpleName() + " Loaded");

        System.out.println("Try robot");
        /*
        Platform.runLater(() -> {
                    try {
                        try {
                            //new JFXPanel();

                            //robot = new Robot();

                            try{
                                //screensize = Toolkit.getDefaultToolkit().getScreenSize();

                            }catch (Exception ex){}

                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } catch (Exception ex) {
                    }
                }
        );
*/
        System.out.println("Robot finished");


        /***********************************************************
         * Create a runnable thread object
         * simply type threadFunction to generate this code
         ***********************************************************/
        new Thread(() -> {
            while (!exitThread) {
                synchronized (altAzSynchroniser){
                    try {
                        altAzSynchroniser.wait();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // we should just have
                sendAltAz(currentAz, currentAlt);
            }
        }).start();


        /****************** End threadFunction **************************/

        
        postTimeSender = new FloatControlSender(this, "Post Time", 0);
        

        /*************************************************************
         * Control to send current cooridinates
         *************************************************************/
        TextControl coordinatesSender = new TextControl(this, "Current Coordinate", "") {
            @Override
            public void valueChanged(String control_val) {
                /*** Write your DynamicControl code below this line ***/

                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl positionSend code ***/


        /*** Simply type globalTriggerControl to generate this code ***/
        TriggerControl coordinatesRequested = new TriggerControl(this, "Get Coordinates") {
            @Override
            public void triggerEvent() {
                /*** Write your DynamicControl code below this line ***/
                String coordinates = requestCoordinated();

                if (!coordinates.isEmpty()){
                    coordinatesSender.setValue(coordinates);
                }
                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl coordinatesRequested code ***/

        
        FloatControl lrDisplay = new FloatControlSender(this, "LR Movement Local", 0);
        

        /*************************************************************
         * Create a Float type Dynamic Control pair
         * Simply type globalFloatControl to generate this code
         *************************************************************/
        FloatControl leftRightMovement = new FloatSliderControl(this, "LR Movement", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/
                LRMovementAmount = control_val;
                synchronized (lrMoveSynchroniser){
                    lrMoveSynchroniser.notify();
                }

                lrDisplay.setValue(control_val);
                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);


        FloatControl udDisplay = new FloatControlSender(this, "UD Movement Local", 0);
        /*************************************************************
         * Create a Float type Dynamic Control pair
         * Simply type globalFloatControl to generate this code
         *************************************************************/
        FloatControl upDownMovement = new FloatSliderControl(this, "UP Movement", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/
                UDMovementAmount = control_val;
                udDisplay.setValue(control_val);
                synchronized (upMoveSynchroniser){
                    upMoveSynchroniser.notify();
                }
                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl upDownMovement code ***/
        /*** End DynamicControl leftRightMovement code ***/
        /***********************************************************
         * Create a runnable thread object
         * simply type threadFunction to generate this code
         ***********************************************************/
        new Thread(() -> {
            while (!exitThread) {
                synchronized (lrMoveSynchroniser) {
                    try {
                        lrMoveSynchroniser.wait();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // we should just have
                //Add the function you need to execute here
                sendMoveLR(LRMovementAmount);
            }
        }).start();

        /***********************************************************
         * Create a runnable thread object
         * simply type threadFunction to generate this code
         ***********************************************************/
        new Thread(() -> {
            while (!exitThread) {
                synchronized (upMoveSynchroniser) {
                    try {
                        upMoveSynchroniser.wait();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // we should just have
                //Add the function you need to execute here
                sendMoveUD(UDMovementAmount);
            }
        }).start();
        /***********************************************************
         * Create a runnable thread object
         * simply type threadFunction to generate this code
         ***********************************************************/
        new Thread(() -> {
            while (!exitThread) {
                synchronized (fovSynchroniser) {
                    try {
                        fovSynchroniser.wait();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // we should just have
                //Add the function you need to execute here
                String api = "main/fov";
                Map<String,Object> params = new LinkedHashMap<>();

                params.put("fov", fieldOfView);
                try {
                    sendPostMessage(api, params);

                }catch (Exception ex){
                    ex.printStackTrace();
                }

            }
        }).start();

        /*************************************************************
         * Create a string type Dynamic Control that displays as a text box
         * Simply type textControl to generate this code
         *************************************************************/
        TextControl stellariumAddress = new TextControl(this, "Stellarium Address", stellariumDevice) {
            @Override
            public void valueChanged(String control_val) {
                /*** Write your DynamicControl code below this line ***/

                // change where our stellarium device is
                stellariumDevice = control_val;
                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl stellariumAddress code ***/


        /***********************************************************
         * Create a runnable thread object
         * simply type threadFunction to generate this code
         ***********************************************************/
        new Thread(() -> {
            while (!exitThread) {
                synchronized (timerateSynchroniser) {
                    try {
                        timerateSynchroniser.wait();

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                // we should just have
                //Add the function you need to execute here
                String api = "main/time";
                Map<String,Object> params = new LinkedHashMap<>();

                params.put("timerate", timeRate);
                sendPostMessage(api, params);
            }
        }).start();
        /*************************************************************
         * Create a Float type Dynamic Control pair that displays as a slider and text box
         * Simply type FloatSliderControl to generate this code
         *************************************************************/
        FloatControl azimuthControl = new FloatSliderControl(this, "Left / Right", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {

                synchronized (altAzSynchroniser){
                    currentAz = control_val  * Math.PI;
                    altAzSynchroniser.notify();
                }

                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);/*** End DynamicControl azimuthControl code ***/


        /*************************************************************
         * Create a Float type Dynamic Control pair that displays as a slider and text box
         * Simply type FloatSliderControl to generate this code
         *************************************************************/
        FloatControl altitudeControl = new FloatSliderControl(this, "Altitude", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/
                // we are going to convert x axis to the alt
                synchronized (altAzSynchroniser){
                    currentAlt = control_val  / 2 * Math.PI;
                    altAzSynchroniser.notify();
                }
                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);/*** End DynamicControl altitudeControl code ***/

        /*************************************************************
         * Create a Boolean type Dynamic Control that displays as a check box
         * Simply type booleanControl to generate this code
         *************************************************************/
        BooleanControl atmosphereControl = new BooleanControl(this, "Atmospehere", false) {
            @Override
            public void valueChanged(Boolean control_val) {
                /*** Write your DynamicControl code below this line ***/

                sendStelProperty("actionShow_Atmosphere", control_val);

                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl atmosphereControl code ***/

        /*************************************************************
         * Create a Boolean type Dynamic Control that displays as a check box
         * Simply type booleanControl to generate this code
         *************************************************************/
        BooleanControl groundControl = new BooleanControl(this, "Ground", true) {
            @Override
            public void valueChanged(Boolean control_val) {
                /*** Write your DynamicControl code below this line ***/

                sendStelProperty("actionShow_Ground", control_val);

                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl atmosphereControl code ***/


        /*************************************************************
         * Create a Boolean type Dynamic Control that displays as a check box
         * Simply type booleanControl to generate this code
         *************************************************************/
        BooleanControl ConstellationControl = new BooleanControl(this, "Constellation Art", false) {
            @Override
            public void valueChanged(Boolean control_val) {
                /*** Write your DynamicControl code below this line ***/

                sendStelProperty("actionShow_Constellation_Art", control_val);

                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl atmosphereControl code ***/


        /*************************************************************
         * Create a Boolean type Dynamic Control that displays as a check box
         * Simply type booleanControl to generate this code
         *************************************************************/
        BooleanControl starLabelsControl = new BooleanControl(this, "Star Labels", true) {
            @Override
            public void valueChanged(Boolean control_val) {
                /*** Write your DynamicControl code below this line ***/
                sendStelProperty("actionShow_Stars_Labels", control_val);
                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl starLabelsControl code ***/


        /*************************************************************
         * Create a Trigger type Dynamic Control that displays as a button
         * Simply type triggerControl to generate this code
         *************************************************************/
        TriggerControl mouseTrigger = new TriggerControl(this, "Click Mouse") {
            @Override
            public void triggerEvent() {
                /*** Write your DynamicControl code below this line ***/

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (robot != null && screensize != null){
                    robot.mouseMove(screensize.width / 2, screensize.height / 2);
                    robot.delay(5);
                    robot.mousePress(MouseEvent.BUTTON1_DOWN_MASK);
                    robot.mouseRelease(MouseEvent.BUTTON1_DOWN_MASK);


                    //robot.mouseMove(0, 0);
                }
                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl mouseTrigger code ***/
        /*************************************************************
         * Create a Trigger type Dynamic Control that displays as a button
         * Simply type triggerControl to generate this code
         *************************************************************/
        new TriggerControl(this, "Clear Display") {
            @Override
            public void triggerEvent() {
                /*** Write your DynamicControl code below this line ***/
                // this will crear display
                String api = "main/focus";
                Map<String,Object> params = new LinkedHashMap<>();

                sendPostMessage(api, params);

                scriptStatus();
                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl triggerControl code ***/


        /*************************************************************
         * Create a Trigger type Dynamic Control that displays as a button
         * Simply type triggerControl to generate this code
         *************************************************************/
        TriggerControl requestPosition = new TriggerControl(this, "Request Position") {
            @Override
            public void triggerEvent() {
                /*** Write your DynamicControl code below this line ***/
                String api = "main/view";

                try {
                    JSONObject message_val = sendGetMessage(api);
                    if (message_val != null) {

                        Object altAz = message_val.get("altAz");
                        if (altAz != null){
                            lastAltAz = altAz.toString();
                            System.out.println(lastAltAz);
                        }
                        //ret = message_val.getBoolean("scriptIsRunning");

                    }
                }
                catch (Exception ex){}

                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl requestPosition code ***/

        /*************************************************************
         * Create a Trigger type Dynamic Control that displays as a button
         * Simply type triggerControl to generate this code
         *************************************************************/
        TriggerControl sendLastAltAZ = new TriggerControl(this, "Send last AltAz") {
            @Override
            public void triggerEvent() {
                /*** Write your DynamicControl code below this line ***/

                if (!lastAltAz.isEmpty()){
                    sendAltAz(lastAltAz);
                }
                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl sendLastAltAZ code ***/

        /*************************************************************
         * Create a Float type Dynamic Control pair
         * Simply type globalFloatControl to generate this code
         *************************************************************/
        fovReturnControl = new FloatSliderControl(this, "FOV Return", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/
                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);

        /*************************************************************
         * Create a Float type Dynamic Control that displays as a slider and text box
         * Simply type FloatSliderControl to generate this code
         *************************************************************/
        FloatControl FOVControl = new FloatSliderControl(this, "Field of view", 20, 1, 180) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/
                fieldOfView = control_val;

                synchronized (fovSynchroniser){
                    fovSynchroniser.notify();
                }
                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);/*** End DynamicControl FOVControl code ***/

        /*************************************************************
         * Create a Float type Dynamic Control pair that displays as a slider and text box
         * Simply type FloatSliderControl to generate this code
         *************************************************************/
        FloatControl TimerateControl = new FloatSliderControl(this, "Timerate", timeRate, -0.006666667, 0.006666667) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/
                timeRate = control_val;
                synchronized (timerateSynchroniser){
                    timerateSynchroniser.notify();
                }

                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl FOVControl code ***/

        /*************************************************************
         * Create a string type Dynamic Control that displays as a text box
         * Simply type textControl to generate this code
         *************************************************************/
        TextControl scriptControl = new TextControl(this, "Script name", "") {
            @Override
            public void valueChanged(String control_val) {
                /*** Write your DynamicControl code below this line ***/
                stopScript();
                if (!control_val.isEmpty()){
                    runScript(control_val);
                }

                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl scriptControl code ***/


        /*************************************************************
         * Create a Float type Dynamic Control pair that displays as a slider and text box
         * Simply type FloatSliderControl to generate this code
         *************************************************************/
        FloatControl accelerometerSimulator = new FloatSliderControl(this, "Accel Sim", 1, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/

                float scaled_val = Sensor.scaleValue(-1, 1, MIN_ACCEL_ALT, MAX_ACCEL_ALT, control_val);
                altitudeControl.setValue(scaled_val);
                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl accelerometerSimulator code ***/

        // add stell true / false
        /*************************************************************
         * Create a string type Dynamic Control that displays as a text box
         * Simply type textControl to generate this code
         *************************************************************/
        TextControl stelPropertyName = new TextControl(this, "Stel Property", "actionShow_Atmosphere") {
            @Override
            public void valueChanged(String control_val) {
                /*** Write your DynamicControl code below this line ***/

                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl stelPropertyName code ***/

        /*************************************************************
         * Create a Boolean type Dynamic Control that displays as a check box
         * Simply type booleanControl to generate this code
         *************************************************************/
        BooleanControl stelPropertyValue = new BooleanControl(this, "Stel Value", false) {
            @Override
            public void valueChanged(Boolean control_val) {
                /*** Write your DynamicControl code below this line ***/
                String stel_value = stelPropertyName.getValue();
                sendStelProperty(stel_value, control_val);
                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl stelPropertyValue code ***/

        sendStelProperty("actionShow_Atmosphere", false);
        /***** Type your HBAction code above this line ******/
    }



    /**
     * Send a post message to Stellarium
     * Code fragments from https://stackoverflow.com/questions/4205980/java-sending-http-parameters-via-post-method-easily
     * @param api the API we are sending to
     * @param params the post parameters
     * @return true on success
     */
    synchronized boolean sendPostMessage(String api, Map<String,Object> params) {
        boolean ret = false;

        long current_time = System.currentTimeMillis();

        try {
            URL url = new URL("http://" + stellariumDevice + ":8090/api/" + api);
            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) {
                    postData.append('&');
                }

                postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                postData.append('=');
                postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
            }
            byte[] post_data_bytes = postData.toString().getBytes("UTF-8");

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(post_data_bytes.length));
            conn.setDoOutput(true);
            conn.getOutputStream().write(post_data_bytes);

            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0; )
                sb.append((char) c);
            String response = sb.toString();
            ret = response.equalsIgnoreCase("ok");
            //System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
            ret = false;
        }

        long elapsed = System.currentTimeMillis();

        postTimeSender.setValue(elapsed - current_time);
        return ret;

    }



    JSONObject sendGetMessage(String api){
        JSONObject ret = null;

        try {
            URL url = new URL("http://localhost:8090/api/" + api);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));

            StringBuilder sb = new StringBuilder();
            for (int c; (c = in.read()) >= 0; )
                sb.append((char) c);
            String response = sb.toString();
            ret = new JSONObject(response);;
            //System.out.println(response);
        } catch (Exception e) {
            e.printStackTrace();
            ret = null;
        }

        return ret;

    }

    /**
     * Set to defined altAz coordinates
     * @param coordinate the coordinates to go to
     * @return true on success
     */
    boolean sendAltAz(String coordinate){
        String api = "main/view";
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("altAz", coordinate);

        return sendPostMessage(api, params);
    }
    /**
     * Send new altAaz position to stellarium
     * @param az new azimuth in radians
     * @param alt new altitude in radians
     * @return status message
     */
    boolean sendAltAz(double az, double alt){
        String api = "main/view";
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("az", az);
        params.put("alt", alt);

        return sendPostMessage(api, params);
    }

    /**
     * Run the Stellarium script
     * @param script_name the name of the script to run
     * @return true if message was send
     */
    boolean runScript(String script_name){
        String api = "scripts/run";
        Map<String,Object> params = new LinkedHashMap<>();

        params.put("id", script_name);

        System.out.println("Run script " + script_name);
        return sendPostMessage(api, params);
    }

    /**
     * Stops the current script
     * @return true if message was sent
     */
    boolean stopScript(){
        String api = "scripts/stop";
        Map<String,Object> params = new LinkedHashMap<>();

        return sendPostMessage(api, params);
    }

    boolean scriptStatus(){
        boolean ret = false;
        //curl -G http://localhost:8090/api/scripts/status
        String api = "scripts/status";

        try {
            JSONObject message_val = sendGetMessage(api);
            if (message_val != null) {

                ret = message_val.getBoolean("scriptIsRunning");
                System.out.println("Get Script status " + ret);
            }
        }
        catch (Exception ex){}
        return ret;
    }

    /**
     * Sends a stell property value
     * @param name the name of the parameter
     * @param val the value of the parameter
     * @return true if successful
     */
    boolean sendStelProperty(String name, Object val){
        String api = "stelproperty/set";
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("id", name);
        params.put("value", val);

        return sendPostMessage(api, params);
    }

    /**
     * Make stellarium move left or right in azimuth simulating arrow keys
     * a negative value signifies left
     * @param qty qty to move
     * @return true on success
     */
    boolean sendMoveLR(double qty){
        String api = "main/move";
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("x", qty);

        return sendPostMessage(api, params);
    }

    /**
     * Make stellarium move up or down simulating arrow keys
     * a negative value signifies down
     * @param qty qty to move
     * @return true on success
     */
    boolean sendMoveUD(double qty){
        String api = "main/move";
        Map<String,Object> params = new LinkedHashMap<>();
        params.put("y", qty);

        return sendPostMessage(api, params);
    }

    /**
     * get the current coordinates of
     * @return current coordinates in x,y,z
     */
    String requestCoordinated(){
        String ret = "";

        String api = "main/view";

        try {
            JSONObject message_val = sendGetMessage(api);
            if (message_val != null) {

                Object altAz = message_val.get("altAz");
                if (altAz != null){
                    ret = altAz.toString();
                }

            }
        }
        catch (Exception ex){}
        return ret;
    }
    //<editor-fold defaultstate="collapsed" desc="Debug Start">

    /**
     * This function is used when running sketch in IntelliJ IDE for debugging or testing
     *
     * @param args standard args required
     */
    public static void main(String[] args) {

        try {
            HB.runDebug(MethodHandles.lookup().lookupClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void doReset() {
        exitThread = true;
    }
    //</editor-fold>

}
