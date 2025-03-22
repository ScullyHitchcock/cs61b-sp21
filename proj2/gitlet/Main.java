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
                validateNumArgs(firstArg, args, 1);
                Repository.setup();
                break;
            case "add":
                validateNumArgs(firstArg, args, 2);
                Repository.addFile(args[1]);
                break;
            case "commit":
                validateNumArgs(firstArg, args, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validateNumArgs(firstArg, args, 2);
                Repository.remove(args[1]);
                break;
            case "log":
                validateNumArgs(firstArg, args, 1);
                Repository.log();
                break;
            case "global-log":
                validateNumArgs(firstArg, args, 1);
                Repository.globalLog();
                break;
            case "find":
                validateNumArgs(firstArg, args, 2);
                Repository.find(args[1]);
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

    /**
     * 检查输入的命令个数是否符合规定，如果不符合则抛出异常 Utils.error。
     * @param cmd：命令关键字如 "init"、"add" 等。
     * @param args：输入的命令。
     * @param n：规定的命令个数。
     */
    public static void validateNumArgs(String cmd, String[] args, int n) {
        if (args.length != n) {
            throw Utils.error("Incorrect operands.");
        }
    }
}
