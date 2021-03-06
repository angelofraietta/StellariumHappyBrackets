public class HBPerm_DataSmoother {

    double messages [];
    final int BUFF_SIZE;
    int buffIndex = 0;

    double lastValue = 0;
    double accumulator = 0;


    /**
     * Constructor
     * @param buffer_size the number of items in our buffer
     */
    public HBPerm_DataSmoother(int buffer_size){
        BUFF_SIZE = buffer_size;
        messages = new double[buffer_size];
    }

    /**
     * Get the average calculated value
     * @return the average data inside buffer
     */
    public double getAverage(){
        if (buffIndex > 0) {
            if (buffIndex >= BUFF_SIZE) {
                return accumulator / BUFF_SIZE;
            } else {
                return accumulator / (buffIndex);
            }
        } else {
            return 0;
        }
    }

    /**
     * Add a value to our accumulated value
     * @param new_val the new value to add
     * @return the current average value
     */
    public double addValue(double new_val){
        int array_index = buffIndex % BUFF_SIZE;

        if (buffIndex >= BUFF_SIZE){
            // we need to pop one off the front
            double dropped_val = messages[array_index];
            accumulator -= dropped_val;
        }
        messages[array_index] = new_val;
        accumulator += new_val;
        buffIndex++;
        return getAverage();
    }
}
