package gitlet;

import java.io.File;

/** A debugging class whose main program may be invoked as follows:
 *      java gitlet.DumpObj FILE...
 *  where each FILE is a file produced by Utils.writeObject (or any file
 *  containing a serialized object).  This will simply read FILE,
 *  deserialize it, and call the dump method on the resulting Object.
 *  The object must implement the gitlet.Dumpable interface for this
 *  to work.  For example, you might define your class like this:
 *
 *        import java.io.Serializable;
 *        import java.util.TreeMap;
 *        class MyClass implements Serializeable, Dumpable {
 *            ...
 *            @Override
 *            public void dump() {
 *               System.out.printf("size: %d%nmapping: %s%n", _size, _mapping);
 *            }
 *            ...
 *            int _size;
 *            TreeMap<String, String> _mapping = new TreeMap<>();
 *        }
 *
 *  As illustrated, your dump method should print useful information from
 *  objects of your class.
 *  @author P. N. Hilfinger
 */

/** 一个调试类，其主程序可按如下方式调用：
 *      java gitlet.DumpObj 文件...
 *  其中每个文件（FILE）都是由 Utils.writeObject 方法生成的文件（或任何包含序列化对象的文件）。
 *  该程序会简单地读取文件，进行反序列化，然后在反序列化得到的对象上调用 dump 方法。
 *  为了使这一过程生效，该对象必须实现 gitlet.Dumpable 接口。
 *  例如，你可以这样定义你的类：
 *
 *        import java.io.Serializable;
 *        import java.util.TreeMap;
 *        class MyClass implements Serializeable, Dumpable {
 *            ...
 *            @Override
 *            public void dump() {
 *               System.out.printf("size: %d%nmapping: %s%n", _size, _mapping);
 *            }
 *            ...
 *            int _size;
 *            TreeMap<String, String> _mapping = new TreeMap<>();
 *        }
 *
 *  如上所示，你的 dump 方法应该打印出该类对象中有用的信息。
 *  @author P. N. Hilfinger
 */
public class DumpObj {

    /** Deserialize and apply dump to the contents of each of the files
     *  in FILES. */
    public static void main(String... files) {
        for (String fileName : files) {
            Dumpable obj = Utils.readObject(new File(fileName),
                                            Dumpable.class);
            obj.dump();
            System.out.println("---");
        }
    }
}

