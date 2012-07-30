import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;

public class TestBigList {

    /**
     * @param args
     */
    public static void main(String[] args) {
        int size = 7000000;
        Collection<Long> list = new ArrayList<Long>();
        System.out.println("Started creation at " + new Date());
        Random r = new Random();
        for (int i = 0; i < size; i++) {
            list.add(r.nextLong());
        }
        System.out.println("Completed creation at " + new Date());
        Collection<Long> set = new HashSet<Long>();
        set.addAll(list);
        System.out.println("Completed copying to set " + new Date());
    }
    
    public static void addToList() {
        
    }
    
    

}
