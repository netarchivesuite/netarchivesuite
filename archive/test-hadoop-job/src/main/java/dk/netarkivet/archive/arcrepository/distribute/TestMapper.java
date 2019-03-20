package dk.netarkivet.archive.arcrepository.distribute;

import java.io.IOException;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Mapper;

public class TestMapper extends Mapper<Text, BytesWritable,Text,Text> {

    @Override protected void map(Text path, BytesWritable contents, Context context)
            throws IOException, InterruptedException {
        try {
            context.setStatus(path.toString());
            context.write(path, new Text(contents.toString()));
        } catch (Exception e){
            throw new RuntimeException(e);
        }
    }
}
