package gitlet;

import java.util.Arrays;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS containsCommit
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        try {
            if (args.length == 0) {
                throw Utils.error("Please enter a command.");
            }
            String firstArg = args[0];
            switch(firstArg) {
                case "init":
                    validateNumArgs(args, 1, 1);
                    Repository.setup();
                    break;
                case "add":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.addFile(args[1]);
                    break;
                case "commit":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.commit(args[1], null);
                    break;
                case "rm":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.remove(args[1]);
                    break;
                case "log":
                    validateNumArgs(args, 1, 1);
                    validatePath();
                    Repository.log();
                    break;
                case "global-log":
                    validateNumArgs(args, 1, 1);
                    validatePath();
                    Repository.globalLog();
                    break;
                case "find":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.find(args[1]);
                    break;
                case "status":
                    validateNumArgs(args, 1, 1);
                    validatePath();
                    Repository.status();
                    break;
                case "checkout":
                    validateNumArgs(args, 2, 4);
                    if (args.length > 2 && !Arrays.asList(args).contains("--")) {
                        throw Utils.error("Incorrect operands.");
                    }
                    validatePath();
                    String[] checkoutArgs = Arrays.copyOfRange(args, 1, args.length);
                    Repository.checkout(checkoutArgs);
                    break;
                case "branch":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.branch(args[1]);
                    break;
                case "rm-branch":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.rmBranch(args[1]);
                    break;
                case "reset":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.reset(args[1]);
                    break;
                case "merge":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.merge(args[1]);
                    break;
                case "add-remote":
                    validateNumArgs(args, 3, 3);
                    validatePath();
                    Repository.addRemote(args[1], args[2]);
                case "rm-remote":
                    validateNumArgs(args, 2, 2);
                    validatePath();
                    Repository.rmRemote(args[1]);
                case "push":
                    validateNumArgs(args, 3, 3);
                    validatePath();
                    Repository.push(args[1], args[2]);
                case "fetch":
                    validateNumArgs(args, 3, 3);
                    validatePath();
                    Repository.fetch(args[1], args[2]);
                case "pull":
                    validateNumArgs(args, 3, 3);
                    validatePath();
                    Repository.pull(args[1], args[2]);
                default:
                    throw Utils.error("No command with that name exists.");
            }
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    /**
     * 检查输入的命令个数是否符合规定，如果不符合则抛出异常 Utils.error。
     *
     * @param args ：输入的命令。
     * @param mix  ：规定允许输入最少的命令个数。
     * @param max  ：规定允许输入最多的命令个数。
     */
    public static void validateNumArgs(String[] args, int mix, int max) {
        if (args.length < mix || args.length > max) {
            throw Utils.error("Incorrect operands.");
        }
    }
    public static void validatePath() {
        if (!Repository.GITLET_DIR.exists()) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
    }
}
