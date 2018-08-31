import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.events.KillTrigger;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.control.*;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.device.sensors.AccelerometerListener;
import net.happybrackets.device.sensors.GyroscopeListener;
import net.happybrackets.device.sensors.Sensor;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Date;

public class PoiControl implements HBAction, HBReset {
    // Change to the number of audio Channels on your device
    final int NUMBER_AUDIO_CHANNELS = 1;

    // The string holds the Y accelerometer
    // around string is Pitch

    // Around the speakers is the yaw
    // we will use Yaw for fine up down movement

    final float BUZZ_VOL = 20;
    final float BUZZ_DURATION = 200;


    final int ALTITUDE_BUFF_SIZE = 4;
    final int AZIMUTH_BUFF_SIZE = 4;

    // the maximum altitude we can send
    final double MAX_ALT = 2;
    final double MIN_ALT = 1.000001;

    HBPerm_DataSmoother averageAltitude = new HBPerm_DataSmoother(ALTITUDE_BUFF_SIZE);
    HBPerm_DataSmoother averageAzimuth = new HBPerm_DataSmoother(AZIMUTH_BUFF_SIZE);

    float currentAltitude = 0;
    float currentAzimuth = 0;

    final int TOTAL_OSCILLATORS = 10;
    int numOscillators = 0;

    Gain masterGain = new Gain(NUMBER_AUDIO_CHANNELS, 0.1f);

    ArrayList<Envelope> envelopes = new ArrayList<>();

    final double[] fieldsOfView = new double[] {
            0.005, 0.016, 0.37, 0.14, 0.5,  0.75, 1.25, 3.5, 7.5, 15, 30, 60, 120
    };

    int currentFovIndex = fieldsOfView.length - 3;

    double remoteFOV =  0;

    Clock beepClock;

    double clockInterval = 500;

    int envelopIndex =  0;

    // We will set this when the average gyo is within range
    boolean altitudeMovementValid = false;


    final double ALTITIDE_GYRO_THRESHOLD = 0.08;

    // moving the gyro at a rate greater than this amount will change the field of view
    final double FOV_GYRO_THRESHOLD = 2;

    // we will only allow FOV to change a certain number of times
    long lastFovDate = 0; // last time FOV was changed
    final long FOV_WAIT_TIME = 1; // How long we must wait before successive FOV changes

    // maximum and minimum FIELD of view values
    final double MIN_FOV = fieldsOfView[0];
    final double MAX_FOV = fieldsOfView[fieldsOfView.length -1];

    final int MAX_CLOCK_INTERVAL = 4000;
    final int MIN_CLOCK_INTERVAL = 100;

    // If our FOV threshold is below this value, we will change the way our sensors behave
    final double FOV_SENSOR_THRESHOLD = 30;



    /**
     * Calculate what we want our clock to run based on field of view
     * @param field_of_view the field of view
     * @return clock interval
     */
    double calculateClockInterval (double field_of_view){

        // first find which index we are in
        int fov_index = 0;

        while (fieldsOfView[fov_index] < field_of_view && fov_index < fieldsOfView.length -1)
        {
            fov_index++;
        }

        return Sensor.scaleValue(0, fieldsOfView.length, MIN_CLOCK_INTERVAL, MAX_CLOCK_INTERVAL, field_of_view);
    }


    /**
     * Get the current Field of view value based on index
     * @return the current field of view for current index
     */
    double getSelectedFieldOfView(){
        return fieldsOfView[currentFovIndex];
    }

    /**
     * Change our Field of view amount
     * @param increase true if we are increasing our amount, otherwise, we are decreasing
     * @return the field of view
     */
    double changeFieldOfView(boolean increase){
        if (increase){
            if (currentFovIndex < fieldsOfView.length -1){
                currentFovIndex++;
            }
        }
        else {
            if (currentFovIndex > 0){
                currentFovIndex--;
            }
        }

        return fieldsOfView[currentFovIndex];
    }

    /**
     * Get current date in seconds
     * @return time in seconds
     */
    long getDateinSeconds(){
        long ret = new Date().getTime() /1000L;
        return ret;
    }

    /**
     * Play Buzz
     */
    void playBuzz(boolean direction){

        final float BUZZ_FREQ = 100;

        // if we are going up, make our frequency go up
        float end_freq = direction? BUZZ_FREQ *2: BUZZ_FREQ /2;

        Envelope freq_envelope = new Envelope(100);
        WavePlayer wp = new WavePlayer(freq_envelope, Buffer.SQUARE);
        Envelope gain_envelope = new Envelope(1);
        Gain soundAmp = new Gain(1, gain_envelope);
        wp.connectTo(soundAmp).connectTo(masterGain);

        freq_envelope.addSegment(end_freq, BUZZ_DURATION);
        gain_envelope.addSegment(BUZZ_VOL, BUZZ_DURATION);
        gain_envelope.addSegment(0, 10, new KillTrigger(soundAmp));


    }
    @Override
    public void action(HB hb) {
        /***** Type your HBAction code below this line ******/
        // remove this code if you do not want other compositions to run at the same time as this one
        hb.reset();
        hb.setStatus(this.getClass().getSimpleName() + " Loaded");



        // Connect our gain to HB
        masterGain.connectTo(hb.ac.out);

        /************************************************************
         * To create this, just type clockTimer
         ************************************************************/
        Clock hbClock = hb.createClock(5000).addClockTickListener((offset, this_clock) -> {
            /*** Write your Clock tick event code below this line ***/
            //int envelope_number = (int) (Math.random() * TOTAL_OSCILLATORS);

            Envelope e = envelopes.get(envelopIndex++ % TOTAL_OSCILLATORS);
            final float LOUD_VOL = 20;
            final float LOUD_SLOPE = 20;
            final float LOUD_DURATION = 200;

            float envelope_duration = (float)clockInterval / 5;
            if (envelope_duration > LOUD_DURATION){
                envelope_duration = LOUD_DURATION;
            }

            e.addSegment(LOUD_VOL, LOUD_SLOPE);
            e.addSegment(LOUD_VOL, envelope_duration);
            e.addSegment(1, LOUD_SLOPE);
            /*** Write your Clock tick event code above this line ***/
        });

        //hbClock.start(); we will start at the end
        /******************* End Clock Timer *************************/

        beepClock = hbClock;

        /*************************************************************
         * Create a Boolean type Dynamic Control pair that displays as a check box
         * Simply type globalBooleanControl to generate this code
         *************************************************************/
        BooleanControl disableFOVChange = new BooleanControl(this, "Disable FOV Change", false) {
            @Override
            public void valueChanged(Boolean control_val) {
                /*** Write your DynamicControl code below this line ***/

                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl disableFOVChange code ***/


        /*************************************************************
         * Create a Float type Dynamic Control pair
         * Simply type globalFloatControl to generate this code
         *************************************************************/
        FloatBuddyControl fovReturn = new FloatBuddyControl(this, "FOV Return", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/

                // act on field of view returned from Stellarium
                remoteFOV = control_val;

                hbClock.setInterval(calculateClockInterval(remoteFOV));

                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl fovReturnControl code ***/
        /*************************************************************
         * Create a Float type Dynamic Control pair
         * Simply type globalFloatControl to generate this code
         *************************************************************/
        FloatBuddyControl upSender = new FloatBuddyControl(this, "UP Movement", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/

                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl upSender code ***/

        /*************************************************************
         * Create a Float type Dynamic Control pair
         * Simply type globalFloatControl to generate this code
         *************************************************************/
        FloatBuddyControl lrSender = new FloatBuddyControl(this, "LR Movement", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/

                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl lrSender code ***/


        /*************************************************************
         * Create a Float type Dynamic Control pair
         * Simply type globalFloatControl to generate this code
         *************************************************************/
        FloatBuddyControl altitudeSender = new FloatBuddyControl(this, "Altitude", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/

                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl altitudeSender code ***/

        /*************************************************************
         * Create a Float type Dynamic Control pair
         * Simply type globalFloatControl to generate this code
         *************************************************************/
        FloatBuddyControl azimuthSender = new FloatBuddyControl(this, "Left / Right", 0, -1, 1) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/

                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);
        /*** End DynamicControl azimuthSender code ***/



        /*************************************************************
         * Create a Float type Dynamic Control that displays as a text box
         * Simply type floatTextControl to generate this code
         *************************************************************/
        FloatTextControl fieldOfViewControl = new FloatTextControl(this, "Field of view", 20) {
            @Override
            public void valueChanged(double control_val) {
                /*** Write your DynamicControl code below this line ***/
                hbClock.setInterval(calculateClockInterval(control_val));
                /*** Write your DynamicControl code above this line ***/
            }
        }.setControlScope(ControlScope.GLOBAL);/*** End DynamicControl code fieldOfViewControl ***/

        /*****************************************************
         * Add a gyroscope sensor listener. *
         * to create this code, simply type gyroscopeSensor
         *****************************************************/
        new GyroscopeListener(hb) {
            @Override
            public void sensorUpdated(float pitch, float roll, float yaw) {
                /******** Write your code below this line ********/

                // make sure we have enough gyroscope to warrant moving azimuth
                if (Math.abs(pitch) > 0.1 && Math.abs(pitch) < FOV_GYRO_THRESHOLD) {
                    if (lowResolutionMode()) {
                        float az_adjust = (float) averageAzimuth.addValue(pitch) / 100;
                        currentAzimuth += az_adjust;
                        azimuthSender.setValue(currentAzimuth);
                    }
                    else
                    {
                        // we need to send left right
                        lrSender.setValue(pitch / 8);
                    }
                }

                if (Math.abs(pitch) >= FOV_GYRO_THRESHOLD && !disableFOVChange.getValue()) {
                    long delay = getDateinSeconds() - lastFovDate;
                    if (delay > FOV_WAIT_TIME) {
                        lastFovDate = getDateinSeconds();
                        double current_fov = fieldOfViewControl.getValue();

                        double new_fov = changeFieldOfView(pitch < 0);

                        if (current_fov != new_fov){
                            fieldOfViewControl.setValue(new_fov);
                            playBuzz(pitch > 0);
                            if (!lowResolutionMode()){
                                // stop sending our arrow keys
                                lrSender.setValue(0);
                                upSender.setValue(0);
                            }
                        }

                    }
                }
                // Let us stabilise the altidue by making sure we have enough gyo on roll and yaw
                // to indicate we are actually moving. We will do a pythagorean average
                double averageGyro = Math.sqrt( roll * roll + yaw * yaw);


                altitudeMovementValid = averageGyro > ALTITIDE_GYRO_THRESHOLD;

                // see if we are going to send up / down keys
                if (Math.abs(yaw) > 0.1 && !lowResolutionMode()) {
                    // we need to send left right
                    upSender.setValue(yaw / 2);
                }

                /******** Write your code above this line ********/
            }
        }.setRounding(2);
        /*** End gyroscopeSensor code ***/

        /*****************************************************
         * Find an accelerometer sensor.
         * to create this code, simply type accelerometerSensor
         *****************************************************/
        new AccelerometerListener(hb) {
            @Override
            public void sensorUpdate(float x_val, float y_val, float z_val) {
                /* accelerometer values typically range from -1 to + 1 */
                /******** Write your code below this line ********/

                // Our altitude valid will only occur if our average gyro is above a certain value
                if (lowResolutionMode() && altitudeMovementValid) {
                    // we are going to make our value go from min to  max
                    // this allows conversion to radians the min value and max values level

                    float scaled_val = Sensor.scaleValue(-1, 1, MIN_ALT, MAX_ALT, y_val);
                    float altitude_val = scaled_val;

                    float altitude = (float) averageAltitude.addValue(altitude_val);
                    if (altitude >= 1 && altitude <= MAX_ALT) {

                        currentAltitude = altitude;

                        altitudeSender.setValue(currentAltitude);
                    }
                }

                /******** Write your code above this line ********/

            }
        }.setRounding(2);
        /*** End accelerometerSensor code ***/

        // create additional oscillators
        for (int i = 0; i < TOTAL_OSCILLATORS; i++) {
           envelopes.add(addOscillator(500+hb.rng.nextFloat()*50));
        }



        hbClock.start();
        /***** Type your HBAction code above this line ******/
    }

    /**
     * See if we are in high resolution mode
     * @return
     */
    private boolean lowResolutionMode() {
        return fieldsOfView[currentFovIndex] >= FOV_SENSOR_THRESHOLD;
    }

    /**
     * Add an oscillator to output
     * @param freq the frequency
     * @return The envelope to control the new oscillator
     */
    Envelope addOscillator(float freq){
        numOscillators++;
        masterGain.setGain(0.1f / numOscillators);
        WavePlayer wp = new WavePlayer(freq, Buffer.SINE);
        Envelope envelope = new Envelope(1);
        Gain soundAmp = new Gain(1, envelope);
        wp.connectTo(soundAmp).connectTo(masterGain);
        return  envelope;

    }

    /**
     * Add any code you need to have occur when a reset occurs
     */
    @Override
    public void doReset() {
        /***** Type your HBReset code below this line ******/

        /***** Type your HBReset code above this line ******/
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
    //</editor-fold>
}
