package nl.cyso.vcloud.client;

import nl.cyso.vcloud.client.config.ConfigMode;
import nl.cyso.vcloud.client.config.ConfigModes;
import nl.cyso.vcloud.client.config.Configuration;
import nl.cyso.vcloud.client.types.ModeType;

import org.apache.commons.cli.CommandLine;

import com.vmware.vcloud.sdk.Task;

public class Entry {

	public static void main(String[] args) {

		// Start out in ROOT ConfigMode
		CommandLine cli = null;
		ConfigMode opt = ConfigModes.getMode(ModeType.ROOT);

		// Try to parse all ROOT cli options
		cli = Configuration.parseCli(opt, args);

		// Load the config if it was specified
		if (cli.hasOption("config")) {
			Configuration.loadFile(cli.getOptionValue("config"));
		}

		// Load all options parsed for ROOT ConfigMode
		Configuration.load(cli);

		// Now we know which ConfigMode was selected

		// Display (specific) help
		if (Configuration.getMode() == ModeType.HELP) {
			if (Configuration.hasHelpType()) {
				ConfigModes.printConfigModeHelp(Configuration.getHelpType());
			} else {
				ConfigModes.printConfigModeHelp(ModeType.ROOT);
			}
			System.exit(0);
		}

		// From this point we want a header displayed
		Formatter.printHeader();

		// Display version information
		if (Configuration.getMode() == ModeType.VERSION) {
			System.exit(0);
		}

		if (!Configuration.hasUsername() || !Configuration.hasPassword() || !Configuration.hasServer()) {
			Formatter.usageError("No credentials were set, or server uri was missing.", opt);
		}

		vCloudClient client = new vCloudClient();
		client.login(Configuration.getServer(), Configuration.getUsername(), Configuration.getPassword());

		if (Configuration.getMode() == ModeType.LIST) {
			opt = ConfigModes.getMode(ModeType.LIST);
			if (Configuration.getListType() == null) {
				Formatter.usageError("Invalid list type was selected.", opt);
			}
			Formatter.printBorderedInfo(Configuration.dumpToString(opt));

			switch (Configuration.getListType()) {
			case ORG:
				client.listOrganizations();
				break;
			case VDC:
				client.listVDCs(Configuration.getOrganization());
				break;
			case CATALOG:
				client.listCatalogs(Configuration.getOrganization());
				break;
			case VAPP:
				client.listVApps(Configuration.getOrganization(), Configuration.getVDC());
				break;
			case VM:
				client.listVMs(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp());
				break;
			default:
				Formatter.printErrorLine("Not yet implemented");
				break;
			}
		} else if (Configuration.getMode() == ModeType.ADDVM) {
			opt = ConfigModes.getMode(ModeType.ADDVM);

			Configuration.load(opt, args);
			Formatter.printBorderedInfo(Configuration.dumpToString(opt));

			Formatter.waitForTaskCompletion(client.addVM(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getCatalog(), Configuration.getTemplate(), Configuration.getFqdn(), Configuration.getDescription(), Configuration.getIp().getHostAddress(), Configuration.getNetwork()));
		} else if (Configuration.getMode() == ModeType.REMOVEVM || Configuration.getMode() == ModeType.POWERONVM || Configuration.getMode() == ModeType.POWEROFFVM || Configuration.getMode() == ModeType.SHUTDOWNVM || Configuration.getMode() == ModeType.CONSOLIDATEVM) {
			opt = ConfigModes.getMode(Configuration.getMode());

			Configuration.load(opt, args);
			Formatter.printBorderedInfo(Configuration.dumpToString(opt));

			Task t = null;
			switch (Configuration.getMode()) {
			case REMOVEVM:
				t = client.removeVM(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getVM());
				break;
			case POWERONVM:
				t = client.powerOnVM(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getVM());
				break;
			case POWEROFFVM:
				t = client.powerOffVM(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getVM());
				break;
			case SHUTDOWNVM:
				t = client.shutdownVM(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getVM());
				break;
			case CONSOLIDATEVM:
				t = client.consolidateVM(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getVM());
			}
			Formatter.waitForTaskCompletion(t);
		} else if (Configuration.getMode() == ModeType.RESIZEDISK) {
			opt = ConfigModes.getMode(ModeType.RESIZEDISK);

			Configuration.load(opt, args);
			Formatter.printBorderedInfo(Configuration.dumpToString(opt));

			Formatter.waitForTaskCompletion(client.resizeVMDisks(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getVM(), Configuration.getDiskName(), Configuration.getDiskSize()));
		} else {
			opt = ConfigModes.getMode(ModeType.ROOT);
			Formatter.usageError("No mode was selected", opt);
		}
	}
}
