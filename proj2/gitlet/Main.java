package gitlet;

import java.util.Arrays;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author CST
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS containsCommit
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        try {
            if (args.length == 0) {
                throw Utils.error("Please enter a command.");
            }
            switch (args[0]) {
                case "init" -> {
                    validateNumArgs(args, 1, 1);
                    Repository.setup();
                }
                case "add" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.addFile(args[1]);
                }
                case "commit" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.commit(args[1], null);
                }
                case "rm" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.remove(args[1]);
                }
                case "log" -> {
                    validateNumArgs(args, 1, 1);
                    Repository.log();
                }
                case "global-log" -> {
                    validateNumArgs(args, 1, 1);
                    Repository.globalLog();
                }
                case "find" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.find(args[1]);
                }
                case "status" -> {
                    validateNumArgs(args, 1, 1);
                    Repository.status();
                }
                case "checkout" -> {
                    validateNumArgs(args, 2, 4);
                    String[] checkoutArgs = Arrays.copyOfRange(args, 1, args.length);
                    Repository.checkout(checkoutArgs);
                }
                case "branch" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.branch(args[1]);
                }
                case "rm-branch" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.rmBranch(args[1]);
                }
                case "reset" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.reset(args[1]);
                }
                case "merge" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.merge(args[1]);
                }
                case "add-remote" -> {
                    validateNumArgs(args, 3, 3);
                    Repository.addRemote(args[1], args[2]);
                }
                case "rm-remote" -> {
                    validateNumArgs(args, 2, 2);
                    Repository.rmRemote(args[1]);
                }
                case "push" -> {
                    validateNumArgs(args, 3, 3);
                    Repository.push(args[1], args[2]);
                }
                case "fetch" -> {
                    validateNumArgs(args, 3, 3);
                    Repository.fetch(args[1], args[2]);
                }
                case "pull" -> {
                    validateNumArgs(args, 3, 3);
                    Repository.pull(args[1], args[2]);
                }
                default -> throw Utils.error("No command with that name exists.");
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
        if (args[0].equals("checkout") && args.length > 2 && !Arrays.asList(args).contains("--")) {
            throw Utils.error("Incorrect operands.");
        }
        if (!args[0].equals("init")) {
            validatePath();
        }
    }
    public static void validatePath() {
        if (!Repository.gitletDir().exists()) {
            throw Utils.error("Not in an initialized Gitlet directory.");
        }
    }
}
