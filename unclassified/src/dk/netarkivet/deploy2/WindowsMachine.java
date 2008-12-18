package dk.netarkivet.deploy2;

import org.dom4j.Element;

public class WindowsMachine extends Machine {

	public WindowsMachine(Element e, XmlStructure parentSettings,
			Parameters param, String netarchiveSuiteFileSource) {
		super(e, parentSettings, param, netarchiveSuiteFileSource);

		// set operating system
		OS = "windows";
		scriptExtension = ".bat";

//		machineParameters.Display();
		
		// print machine type
//		System.out.println("WINDOWS MACHINE");
	}

	@Override
	protected String OSInstallScript() {
		String res = "";
		
		// pw.println("echo copying $1 to:" + host.getName());
		res += "echo copying ";
		res += netarchiveSuiteFileName;
		res += " to:";
		res += name;
		res += "\n";
	
		// pw.println("scp $1 " + destination + ":" + dir);
		res += "scp ";
		res += netarchiveSuiteFileName;
		res += " ";
		res += MachineUserLogin();
		res += ":";
		res += "\n";

		// pw.println("echo unzipping $1 at:" + host.getName());
		res += "echo unzipping ";
		res += netarchiveSuiteFileName;
		res += " at:";
		res += name;
		res += "\n";
	
		// pw.println("ssh " + destination + " " + unzip);
		res += "ssh ";
		res += MachineUserLogin();
		res += " cmd /c unzip.exe -q -d ";
		res += GetEnvironmentName();
        res += " -o ";
        res += netarchiveSuiteFileName;
        res += "\n";
		
		// pw.println("echo copying settings and scripts");
		res += "echo copying settings and scripts";
		res += "\n";
		
		// pw.println("scp -r " + host.getName() + "/* "
        // 			+ destination + ":" + confDir);
		res += "scp -r ";
		res += name;
		res += "/* ";
		res += MachineUserLogin();
		res += ":";
		res += GetConfDirPath();
		res += "\n";
		
		// pw.println("echo make scripts executable");
		res += "echo make scripts executable";
		res += "\n";
		
		// if (!isWindows) {
		// 		pw.println("ssh  " + destination + " \"chmod +x "
        //					+ confDir + "*.sh \"");
		// }
		res += "";
		
		// pw.println("echo make password files readonly");
		res += "echo make password files readonly";
		res += "\n";

		// 	if (isWindows) {
		//		pw.println("echo Y | ssh " + destination
        //		            + " cmd /c cacls " + confDir
        //		            + "jmxremote.password /P BITARKIV\\\\"
        //		            + user + ":R");
        //	} else {
        //		pw.println("ssh " + destination + " \"chmod 400 "
        //		            + confDir + "/jmxremote.password\"");
        //	}
		res += "echo Y | ssh ";
		res += MachineUserLogin();
		res += " cmd /c cacls ";
		res += GetConfDirPath();
		res += "jmxremote.password /P BITARKIV\\\\";
		res += machineParameters.machineUserName.getText();
		res += ":R";
		res += "\n";
		
		return res;
	}

	@Override
	protected String OSKillScript() {
		String res = "";
		
		res += "ssh ";
		res += MachineUserLogin();
        res += " \"cmd /c  ";
        res += GetConfDirPath();
        res += "killall";
        res += scriptExtension;
        res += " \" ";
 
		return res + "\n";
	}

	@Override
	protected String OSStartScript() {
		String res = "";
		
		res += "ssh ";
		res += MachineUserLogin();
		res += " \"cmd /c  ";
		res += GetConfDirPath();
		res += "startall";
		res += scriptExtension;
        res += " \" ";

		return res + "\n";
	}

	@Override
	protected String GetInstallDirPath() {
		return machineParameters.installDir.getText() + "\\" + GetEnvironmentName();
	}

	@Override
	protected String GetConfDirPath() {
		return GetEnvironmentName() + "\\\\conf\\\\";
	}

}
