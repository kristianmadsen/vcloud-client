package nl.cyso.vcloud.client;

import java.util.concurrent.TimeoutException;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;

import com.vmware.vcloud.sdk.Task;
import com.vmware.vcloud.sdk.VCloudException;

public class Entry {
	public static void usageError(String error, Options opts) {
		System.out.println(error + "\n");
		new HelpFormatter().printHelp(Version.PROJECT_NAME, opts, true);
		System.exit(-1);
	}

	// TODO: find something better than hard coding examples
	public static void printExamples() {
		System.out.println("\nexamples:");
		System.out.println(String.format(" %s -c cloud.cfg", Version.PROJECT_NAME));
		System.out.println("  Load configuration from file \"cloud.cfg\"\n");
		System.out.println(String.format(" %s -c cloud.cfg -l org", Version.PROJECT_NAME));
		System.out.println("  List all organizations that you have access to\n");
		System.out.println(String.format(" %s -c cloud.cfg --organization=\"sample-org\" -l vdc", Version.PROJECT_NAME));
		System.out.println("  List all virtual data centers of organization \"sample-org\" and their networks\n");
		System.out.println(String.format(" %s -c cloud.cfg --organization=\"sample-org\" --vdc=\"sample-vdc\" -l vapp", Version.PROJECT_NAME));
		System.out.println("  List all vApps of virtual data center \"sample-vdc\" of organization \"sample-org\" and their networks\n");
		System.out.println(String.format(" %s -c cloud.cfg --organization=\"sample-org\" --vdc=\"sample-vdc\" --vapp=\"sample-vapp\" -l vm", Version.PROJECT_NAME));
		System.out.println("  List all VMs in vApp \"sample-vapp\" of virtual data center \"sample-vdc\" of organization \"sample-org\"\n");
		System.out.println(String.format(" %s -c cloud.cfg --organization=\"sample-org\" -l catalog", Version.PROJECT_NAME));
		System.out.println("  List all available catalogs and their content of organization \"sample-org\"\n");
		System.out.println(String.format(" %s -c cloud.cfg --organization=\"sample-org\" --vdc=\"sample-vdc\" --vapp=\"sample-vapp\" --catalog=\"sample-catalog\" --template=\"sample-template\" \\", Version.PROJECT_NAME));
		System.out.println("    --fqdn=\"new.vm.fqdn.com\" --description=\"This is a new VM\" --network=\"vcloud-netw-1\" --ip=\"1.2.3.4\" --add-vm");
		System.out.println("  Create a new VM in vApp \"sample-vapp\" of virtual data center \"sample-vdc\" of organization \"sample-org\",");
		System.out.println("     with FQDN \"new.vm.fqdn.com\", a description, and IP \"1.2.3.4\" in network \"vcloud-netw-1\"\n");
		System.out.println(String.format(" %s -c cloud.cfg --organization=\"sample-org\" --vdc=\"sample-vdc\" --vapp=\"sample-vapp\" --vm=\"existing.vm.fqdn.com\" --remove-vm", Version.PROJECT_NAME));
		System.out.println("  Remove an existing VM named \"existing.vm.fqdn.com\" from vApp \"sample-vapp\" of virtual data center \"sample-vdc\" of organization \"sample-org\".\n");
	}

	public static void waitForTaskCompletion(Task task) {
		String message = "Waiting for task completion ";
		String[] twirl = new String[] { "-", "\\", "|", "/" };
		boolean wait = true;
		int counter = 0;
		while (wait) {
			System.out.print("\r" + message + twirl[counter % twirl.length]);

			try {
				task.waitForTask(20);
			} catch (TimeoutException e) {
				// Still waiting...
				counter++;
				continue;
			} catch (VCloudException vce) {
				System.out.print("\n");
				System.err.println("An error occured while executing task");
				System.err.println(vce.getLocalizedMessage());
				System.exit(-1);
			}

			System.out.print("\n");
			System.out.println("Done");
			wait = false;
		}
	}

	public static void main(String[] args) {
		CommandLine cli = null;
		try {
			cli = new PosixParser().parse(Configuration.getOptions(), args);
		} catch (MissingArgumentException me) {
			usageError(me.getLocalizedMessage(), Configuration.getOptions());
			System.exit(-1);
		} catch (MissingOptionException mo) {
			usageError(mo.getLocalizedMessage(), Configuration.getOptions());
			System.exit(-1);
		} catch (AlreadySelectedException ase) {
			usageError(ase.getLocalizedMessage(), Configuration.getOptions());
		} catch (UnrecognizedOptionException uoe) {
			usageError(uoe.getLocalizedMessage(), Configuration.getOptions());
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		if (cli.hasOption("config")) {
			Configuration.loadFile(cli.getOptionValue("config"));
		}

		Configuration.load(cli);

		if (Configuration.getMode() == ModeType.HELP) {
			new HelpFormatter().printHelp("vcloud-client", Configuration.getOptions(), true);
			Entry.printExamples();
			System.exit(0);
		} else if (Configuration.getMode() == ModeType.VERSION) {
			System.out.println(String.format("%s version %s\nBUILD_VERSION: %s", Version.PROJECT_NAME, Version.RELEASE_VERSION, Version.BUILD_VERSION));
			System.exit(0);
		}

		if (!Configuration.hasUsername() || !Configuration.hasPassword() || !Configuration.hasServer()) {
			usageError("No credentials were set, or server uri was missing.", Configuration.getOptions());
		}

		vCloudClient client = new vCloudClient();
		client.login(Configuration.getServer(), Configuration.getUsername(), Configuration.getPassword());

		if (Configuration.getMode() == ModeType.LIST) {
			if (Configuration.getListType() == null) {
				usageError("Invalid list type was selected.", Configuration.getOptions());
			}

			switch (Configuration.getListType()) {
			case ORG:
				client.listOrganizations();
				break;
			case VDC:
				if (!Configuration.hasOrganization()) {
					usageError("An organization must also be specified when listing VDCs", Configuration.getOptions());
				}
				client.listVDCs(Configuration.getOrganization());
				break;
			case CATALOG:
				if (!Configuration.hasOrganization()) {
					usageError("An organization must also be specified when listing Catalogs", Configuration.getOptions());
				}
				client.listCatalogs(Configuration.getOrganization());
				break;
			case VAPP:
				if (!Configuration.hasOrganization()) {
					usageError("An organization must also be specified when listing vApps", Configuration.getOptions());
				}
				if (!Configuration.hasVDC()) {
					usageError("A VDC must also be specified when listing vApps", Configuration.getOptions());
				}
				client.listVApps(Configuration.getOrganization(), Configuration.getVDC());
				break;
			case VM:
				if (!Configuration.hasOrganization()) {
					usageError("An organization must also be specified when listing VMs", Configuration.getOptions());
				}
				if (!Configuration.hasVDC()) {
					usageError("A VDC must also be specified when listing VMs", Configuration.getOptions());
				}
				if (!Configuration.hasVApp()) {
					usageError("A vApp must also be specified when listing VMs", Configuration.getOptions());
				}
				client.listVMs(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp());
				break;
			default:
				System.err.println("Not yet implemented");
				break;
			}
		} else if (Configuration.getMode() == ModeType.ADDVM) {
			if (!Configuration.hasOrganization()) {
				usageError("An existing organization has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasVDC()) {
				usageError("An existing virtual data center has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasVApp()) {
				usageError("An existing vApp has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasCatalog()) {
				usageError("An existing Catalog has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasFqdn()) {
				usageError("A FQDN has to be specified for the new VM", Configuration.getOptions());
			}
			if (!Configuration.hasDescription()) {
				usageError("A description has to be specified for the new VM", Configuration.getOptions());
			}
			if (!Configuration.hasTemplate()) {
				usageError("A template has to be specified for the new VM", Configuration.getOptions());
			}
			if (!Configuration.hasIp()) {
				usageError("An IP has to be specified for the new VM", Configuration.getOptions());
			}
			if (!Configuration.hasNetwork()) {
				usageError("A Network has to be specified for the new VM", Configuration.getOptions());
			}

			waitForTaskCompletion(client.addVM(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getCatalog(), Configuration.getTemplate(), Configuration.getFqdn(), Configuration.getDescription(), Configuration.getIp().getHostAddress(), Configuration.getNetwork()));
		} else if (Configuration.getMode() == ModeType.REMOVEVM || Configuration.getMode() == ModeType.POWERONVM || Configuration.getMode() == ModeType.POWEROFFVM || Configuration.getMode() == ModeType.SHUTDOWNVM || Configuration.getMode() == ModeType.CONSOLIDATEVM) {
			if (!Configuration.hasOrganization()) {
				usageError("An existing organization has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasVDC()) {
				usageError("An existing virtual data center has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasVApp()) {
				usageError("An existing vApp has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasVM()) {
				usageError("An existing VM has to be selected", Configuration.getOptions());
			}
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
			waitForTaskCompletion(t);
		} else if (Configuration.getMode() == ModeType.RESIZEDISK) {
			if (!Configuration.hasOrganization()) {
				usageError("An existing organization has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasVDC()) {
				usageError("An existing virtual data center has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasVApp()) {
				usageError("An existing vApp has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasVM()) {
				usageError("An existing VM has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasDiskName()) {
				usageError("An existing disk has to be selected", Configuration.getOptions());
			}
			if (!Configuration.hasDiskSize()) {
				usageError("An new disk size has to be specified", Configuration.getOptions());
			}
			waitForTaskCompletion(client.resizeVMDisks(Configuration.getOrganization(), Configuration.getVDC(), Configuration.getVApp(), Configuration.getVM(), Configuration.getDiskName(), Configuration.getDiskSize()));
		} else {
			usageError("No mode was selected", Configuration.getOptions());
		}
	}
}
