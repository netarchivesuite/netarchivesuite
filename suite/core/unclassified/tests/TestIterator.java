
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TestIterator {
    

    /**
     * @param args
     */
    public static void main(String[] args) {
        List<String> list = new ArrayList<String>();
        int tal = 2707921;
        while (tal > 0) {
            list.add(UUID.randomUUID().toString());
            tal--;
        }
        
        
        System.out.println("Finished inserting data");
        while (true) {
            // Do nothing
        }
    }

}
