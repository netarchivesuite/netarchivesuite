
public class Maketestdata {

	/**
	 * @param args number of unique netarkivet domains
	 */
	public static void main(String[] args) {
	    if (args.length != 2 ) {
	        System.err.println("Wrong number of arguments.");
	        System.err.println("Usage: Maketestdata <number> s|d");
	        System.exit(1);
	    } 
	    int numberOfDomains = Integer.parseInt(args[0]);
	    String mode = args[1];
	    boolean domainsMode = false;
	    boolean seedsMode = false;
	    if (mode.equalsIgnoreCase("s")) {
	        seedsMode = true;
	    } else if (mode.equalsIgnoreCase("d")) {
	        domainsMode = true;
	    } else {
	        System.err.println("Unknown test-mode");
	        System.err.println("Usage: Maketestdata <number> s|d");
            System.exit(1);
	    }
	    
		for (int i=0; i < numberOfDomains; i++) {
			if (seedsMode) {
			  System.out.println("www.netarkivet" + i + ".dk");    
			} else if (domainsMode) {
			    System.out.println("netarkivet" + i + ".dk");
			}
		}
	}

}
