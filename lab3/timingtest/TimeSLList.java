package timingtest;
import edu.princeton.cs.algs4.Stopwatch;

/**
 * Created by hug.
 */
public class TimeSLList {
    private static void printTimingTable(AList<Integer> Ns, AList<Double> times, AList<Integer> opCounts) {
        System.out.printf("%12s %12s %12s %12s\n", "N", "time (s)", "# ops", "microsec/op");
        System.out.printf("------------------------------------------------------------\n");
        for (int i = 0; i < Ns.size(); i += 1) {
            int N = Ns.get(i);
            double time = times.get(i);
            int opCount = opCounts.get(i);
            double timePerOp = time / opCount * 1e6;
            System.out.printf("%12d %12.2f %12d %12.2f\n", N, time, opCount, timePerOp);
        }
    }

    public static void main(String[] args) {
        timeGetLast();
    }

    public static void timeGetLast() {
        // TODO: YOUR CODE HERE
        int maxScale = 8;
        int targetSize = 1000;
        int ops = 10000;
        AList<Integer> Ns = new AList<>();
        AList<Double> times = new AList<>();
        AList<Integer> opCounts = new AList<>();
        for (int i = 0; i < maxScale; i++) {
            SLList<Integer> testList = new SLList<>();
            for (int index = 0; index < targetSize; index++) {
                testList.addFirst(1);
            }
            Stopwatch sw = new Stopwatch();
            for (int op = 0; op < ops; op++) {
                testList.getLast();
            }
            times.addLast(sw.elapsedTime());
            Ns.addLast(targetSize);
            opCounts.addLast(ops);
            targetSize *= 2;
        }
        printTimingTable(Ns, times, opCounts);
    }

}
