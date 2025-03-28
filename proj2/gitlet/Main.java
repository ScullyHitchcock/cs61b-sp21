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
                    Repository.addFile(args[1]);
                    break;
                case "commit":
                    validateNumArgs(args, 2, 2);
                    Repository.commit(args[1], null);
                    break;
                case "rm":
                    validateNumArgs(args, 2, 2);
                    Repository.remove(args[1]);
                    break;
                case "log":
                    validateNumArgs(args, 1, 1);
                    Repository.log();
                    break;
                case "global-log":
                    validateNumArgs(args, 1, 1);
                    Repository.globalLog();
                    break;
                case "find":
                    validateNumArgs(args, 2, 2);
                    Repository.find(args[1]);
                    break;
                case "status":
                    validateNumArgs(args, 1, 1);
                    Repository.status();
                    break;
                case "checkout":
                    validateNumArgs(args, 2, 4);
                    if (args.length > 2 && !Arrays.asList(args).contains("--")) {
                        throw Utils.error("Incorrect operands.");
                    }
                    String[] checkoutArgs = Arrays.copyOfRange(args, 1, args.length);
                    Repository.checkout(checkoutArgs);
                    break;
                case "branch":
                    validateNumArgs(args, 2, 2);
                    Repository.branch(args[1]);
                    break;
                case "rm-branch":
                    validateNumArgs(args, 2, 2);
                    Repository.rmBranch(args[1]);
                    break;
                case "reset":
                    validateNumArgs(args, 2, 2);
                    Repository.reset(args[1]);
                    break;
                case "merge":
                    validateNumArgs(args, 2, 2);
                    Repository.merge(args[1]);
                    break;
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
}
