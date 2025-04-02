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
            if (args[0].equals("init")) {
                validateNumArgs(args, 1, 1);
                Repository.setup();
            } else if (args[0].equals("add")) {
                validateNumArgs(args, 2, 2);
                Repository.addFile(args[1]);
            } else if (args[0].equals("commit")) {
                validateNumArgs(args, 2, 2);
                Repository.commit(args[1], null);
            } else if (args[0].equals("rm")) {
                validateNumArgs(args, 2, 2);
                Repository.remove(args[1]);
            } else if (args[0].equals("log")) {
                validateNumArgs(args, 1, 1);
                Repository.log();
            } else if (args[0].equals("global-log")) {
                validateNumArgs(args, 1, 1);
                Repository.globalLog();
            } else if (args[0].equals("find")) {
                validateNumArgs(args, 2, 2);
                Repository.find(args[1]);
            } else if (args[0].equals("status")) {
                validateNumArgs(args, 1, 1);
                Repository.status();
            } else if (args[0].equals("checkout")) {
                validateNumArgs(args, 2, 4);
                String[] checkoutArgs = Arrays.copyOfRange(args, 1, args.length);
                Repository.checkout(checkoutArgs);
            } else if (args[0].equals("branch")) {
                validateNumArgs(args, 2, 2);
                Repository.branch(args[1]);
            } else if (args[0].equals("rm-branch")) {
                validateNumArgs(args, 2, 2);
                Repository.rmBranch(args[1]);
            } else if (args[0].equals("reset")) {
                validateNumArgs(args, 2, 2);
                Repository.reset(args[1]);
            } else if (args[0].equals("merge")) {
                validateNumArgs(args, 2, 2);
                Repository.merge(args[1]);
            } else if (args[0].equals("add-remote")) {
                validateNumArgs(args, 3, 3);
                Repository.addRemote(args[1], args[2]);
            } else if (args[0].equals("rm-remote")) {
                validateNumArgs(args, 2, 2);
                Repository.rmRemote(args[1]);
            } else if (args[0].equals("push")) {
                validateNumArgs(args, 3, 3);
                Repository.push(args[1], args[2]);
            } else if (args[0].equals("fetch")) {
                validateNumArgs(args, 3, 3);
                Repository.fetch(args[1], args[2]);
            } else if (args[0].equals("pull")) {
                validateNumArgs(args, 3, 3);
                Repository.pull(args[1], args[2]);
            } else {
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
