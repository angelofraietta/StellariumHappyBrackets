import net.beadsproject.beads.data.Buffer;
import net.beadsproject.beads.ugens.Envelope;
import net.beadsproject.beads.ugens.Gain;
import net.beadsproject.beads.ugens.WavePlayer;
import net.happybrackets.core.HBAction;
import net.happybrackets.core.HBReset;
import net.happybrackets.core.control.ControlScope;
import net.happybrackets.core.control.FloatBuddyControl;
import net.happybrackets.core.control.FloatSliderControl;
import net.happybrackets.core.control.IntegerSliderControl;
import net.happybrackets.core.scheduling.Clock;
import net.happybrackets.device.HB;
import net.happybrackets.device.sensors.AccelerometerListener;
import net.happybrackets.device.sensors.GyroscopeListener;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

public class PoiControl implements HBAction, HBReset {
    // Change to the number of audio Channels on your device
    final int NUMBER_AUDIO_CHANNELS = 1;

    // The string holds the Y accelerometer
    // around string is Pitch

    final int ALTITUDE_BUFF_SIZE = 4;
    final int AZIMUTH_BUFF_SIZE = 4;

    HBPerm_DataSmoother averageAltitude = new HBPerm_DataSmoother(ALTITUDE_BUFF_SIZE);
    HBPerm_DataSmoother averageAzimuth = new HBPerm_DataSmoother(AZIMUTH_BUFF_SIZE);

    float currentAltitude = 0;
    float currentAzimuth = 0;

    final int TOTAL_OSCILLATORS = 10;
    int numOscillators = 0;

    Gain masterGain = new Gain(1, 0.1f);

    ArrayList<Envelope> envelopes = new ArrayList<>();

    Clock beepClock;

    double clockInterval = 500;

    int envelopIndex =  0;

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
         * Create an integer type Dynamic Control that displays as a slider
         * Simply type intSliderControl to generate this code
         *************************************************************/
        IntegerSliderControl clockSpeed = new IntegerSliderControl(this, "ClockSpeed", 1000, 20, 5000) {

            @Override
            public void valueChanged(int control_val) {
                /*** Write your DynamicControl code below this line ***/
                clockInterval = control_val;
                beepClock.setInterval(clockInterval);
                /*** Write your DynamicControl code above this line ***/
            }
        };/*** End DynamicControl clockSpeed code ***/

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


        /*****************************************************
         * Add a gyroscope sensor listener. *
         * to create this code, simply type gyroscopeSensor
         *****************************************************/
        new GyroscopeListener(hb) {
            @Override
            public void sensorUpdated(float pitch, float roll, float yaw) {
                /******** Write your code below this line ********/

                float az_adjust = (float) averageAzimuth.addValue(pitch) / 100;
                currentAzimuth += az_adjust;
                azimuthSender.setValue(currentAzimuth);
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
                float altitude_val = x_val;
                currentAltitude = (float) averageAltitude.addValue(altitude_val);
                altitudeSender.setValue(currentAltitude);

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
