package gitlet;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            throw Utils.error("Must have at least one argument.");
        }
        String firstArg = args[0];
        switch(firstArg) {
            case "init":
                if (Repository.GITLET_DIR.exists()) {
                    throw Utils.error("A Gitlet version-control system already exists in the current directory.");
                }
                Repository.setup();
                break;
            case "add":
                // TODO: handle the `add [filename]` command
                break;
            case "commit":
                break;
            case "rm":
                break;
            case "log":
                break;
            case "global-log":
                break;
            case "find":
                break;
            case "status":
                break;
            case "checkout":
                break;
            case "branch":
                break;
            case "rm-branch":
                break;
            case "reset":
                break;
            case "merge":
                break;
            default:
                throw Utils.error("Unknown command: %s", args[0]);
        }
    }
}
