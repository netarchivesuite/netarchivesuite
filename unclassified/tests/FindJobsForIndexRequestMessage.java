import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import dk.netarkivet.common.utils.FileUtils;


public class FindJobsForIndexRequestMessage {

    /**
     * @param args File w/ ids
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
        File fin = new File(args[0]);
        File fout = new File(fin.getParentFile(), "indexjobIds.txt");
        List<String> lines = FileUtils.readListFromFile(fin);
        Iterator<String> iterator = lines.iterator();
        Set<Long> jobIds = new HashSet<Long>();
        while (iterator.hasNext()) {
            String line = iterator.next();
            String[]lineparts = line.split(",");
            if (lineparts == null) {
                System.out.println("no numbers in line: " + line);
                continue;
            } else {
                Set<Long> numbers = findIntegers(lineparts);
                jobIds.addAll(numbers);
                System.out.println("Found in line('" + line + ")' numbers: " 
                        + numbers.size());
            }
        }
        FileWriter fstream = new FileWriter(fout.getName());
        Iterator<Long> iterator2 = jobIds.iterator();
        while(iterator2.hasNext()) {
            fstream.write(iterator2.next().toString());
            fstream.write('\n');
        }
        fstream.flush();
        fstream.close();
    }

    private static Set<Long> findIntegers(String[] lineparts) {
        Set<Long> ids = new HashSet<Long>();
        for (String linepart: lineparts) {
            String potentialLong = linepart.trim();
            if (!potentialLong.isEmpty()) {
            ids.add(Long.valueOf(potentialLong));
            }
        }
        return ids;
    }

}
