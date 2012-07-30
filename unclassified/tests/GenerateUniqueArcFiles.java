import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
	   	
	   	
public class GenerateUniqueArcFiles {
	// A reasonably optimal value for the chunksize
	static final int byteChunkSize = 10000000;
  	
   	/**
  	   	* Writes a large number of bytes to a given file.
 	   	* @param args
 	   	* args[0] is the number of bytes to write
  	   	* args[1] the name of the output file
  	   	* @throws IOException If unable to write to output file
  	   	*/
  	public static void main(String[] args) throws IOException {
 
 	// Arg 0: number of files
	// Arg 1: size of files in bytes
	// Arg 2: Destinationdir
   	
 	long nbytes = 0;
	long nfiles = 0;
 	if (args.length != 3) {
 		System.out.println("Usage: java GenerateUniqueArcFiles nfiles nbytes destinationdir");
 	 	System.exit(1);
	} 

        try {
  		nfiles = Long.parseLong(args[0]);
 	} catch (Exception e) {
 		System.out.println("First argument must be a number");
 	}
        try {
  		nbytes = Long.parseLong(args[1]);
 	} catch (Exception e) {
 		System.out.println("Second argument must be a number");
 	}

	File destinationDir = new File(args[2]);

	for (int i = 0; i < nfiles; i++) {
		generatefile(nbytes, destinationDir);
	}
	System.out.println("Finished generating " + nfiles 
 		+ " arcfiles in directory " + args[2]);
	}
	
	public static void generatefile(long nbytes, File destinationdir) throws IOException {	
		
 	File outputFile = new File(destinationdir, java.util.UUID.randomUUID().toString() 
		+ ".arc");
 	byte[] byteArr = new byte[byteChunkSize];
 	 FileOutputStream os = new FileOutputStream(outputFile);
  	FileChannel chan = os.getChannel();
	if (nbytes > byteChunkSize) {
  		for (int i = 0; i < nbytes / byteChunkSize; i++) {
  			chan.write(ByteBuffer.wrap(byteArr));
  	   	}
	} else {
		byte[] bytes = new byte[(int)nbytes];	
		chan.write(ByteBuffer.wrap(bytes));
	}
	chan.close();  	
	os.close();
  	}
}
