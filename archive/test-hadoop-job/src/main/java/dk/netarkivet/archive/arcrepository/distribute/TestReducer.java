package dk.netarkivet.archive.arcrepository.distribute;

import java.io.IOException;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;

public class TestReducer extends Reducer<Text, Text, Text, Text> {
    @Override protected void reduce(Text key, Iterable<Text> values, Context context)
            throws IOException, InterruptedException {
        super.reduce(key, values, context);
    }
}
