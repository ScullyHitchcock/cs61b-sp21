package gitlet;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.List;


/** Assorted utilities.
 *
 * Give this file a good read as it provides several useful utility functions
 * to save you some time.
 *
 * 各种实用工具类。
 *
 * 仔细阅读本文件，它提供了几个有用的工具函数，可以帮你节省时间。
 *
 *  @author P. N. Hilfinger
 */
class Utils {

    /** The length of a complete SHA-1 UID as a hexadecimal numeral.
     *
     *  完整 SHA-1 UID 作为十六进制数的长度。
     */
    static final int UID_LENGTH = 40;

    /* SHA-1 HASH VALUES. */

    /** Returns the SHA-1 hash of the concatenation of VALS, which may
     *  be any mixture of byte arrays and Strings.
     *
     *  返回 VALS 连接后的 SHA-1 哈希值，VALS 可以是字节数组或字符串的任意组合。
     */
    static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            Formatter result = new Formatter();
            for (byte b : md.digest()) {
                result.format("%02x", b);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    /** Returns the SHA-1 hash of the concatenation of the strings in
     *  VALS.
     *
     *  返回 VALS 中字符串连接后的 SHA-1 哈希值。
     */
    static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[vals.size()]));
    }

    /* FILE DELETION */

    /** Deletes FILE if it exists and is not a directory. Returns true
     *  if FILE was deleted, and false otherwise. Refuses to delete FILE
     *  and throws IllegalArgumentException unless the directory designated by
     *  FILE also contains a directory named .gitlet.
     *
     *  如果 FILE 存在且不是目录，则删除 FILE。若成功删除，返回 true，否则返回 false。
     *  如果 FILE 目录下不存在 .gitlet 目录，则拒绝删除 FILE 并抛出 IllegalArgumentException 异常。
     */
    static boolean restrictedDelete(File file) {
        if (!(new File(file.getParentFile(), ".gitlet")).isDirectory()) {
            throw new IllegalArgumentException("not .gitlet working directory");
        }
        if (!file.isDirectory()) {
            return file.delete();
        } else {
            return false;
        }
    }

    /** Deletes the file named FILE if it exists and is not a directory.
     *  Returns true if FILE was deleted, and false otherwise. Refuses
     *  to delete FILE and throws IllegalArgumentException unless the
     *  directory designated by FILE also contains a directory named .gitlet.
     *
     *  如果 FILE 存在且不是目录，则删除名为 FILE 的文件。若成功删除，返回 true，否则返回 false。
     *  如果 FILE 目录下不存在 .gitlet 目录，则拒绝删除 FILE 并抛出 IllegalArgumentException 异常。
     */
    static boolean restrictedDelete(String file) {
        return restrictedDelete(new File(file));
    }

    /* READING AND WRITING FILE CONTENTS */

    /** Return the entire contents of FILE as a byte array.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems.
     *
     *  返回 FILE 的全部内容作为字节数组。FILE 必须是一个普通文件。
     *  如果出现问题，抛出 IllegalArgumentException 异常。
     */
    static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return the entire contents of FILE as a String.  FILE must
     *  be a normal file.  Throws IllegalArgumentException
     *  in case of problems.
     *
     *  返回 FILE 的全部内容作为字符串。FILE 必须是一个普通文件。
     *  如果出现问题，抛出 IllegalArgumentException 异常。
     */
    static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    /** Write the result of concatenating the bytes in CONTENTS to FILE,
     *  creating or overwriting it as needed.  Each object in CONTENTS may be
     *  either a String or a byte array.  Throws IllegalArgumentException
     *  in case of problems.
     *
     *  将 CONTENTS 中字节的连接结果写入 FILE，根据需要创建或覆盖它。
     *  CONTENTS 中的每个对象可以是字符串或字节数组。如果出现问题，抛出 IllegalArgumentException 异常。
     */
    static void writeContents(File file, Object... contents) {
        try {
            if (file.isDirectory()) {
                throw
                    new IllegalArgumentException("cannot overwrite directory");
            }
            BufferedOutputStream str =
                new BufferedOutputStream(Files.newOutputStream(file.toPath()));
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    str.write((byte[]) obj);
                } else {
                    str.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                }
            }
            str.close();
        } catch (IOException | ClassCastException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Return an object of type T read from FILE, casting it to EXPECTEDCLASS.
     *  Throws IllegalArgumentException in case of problems.
     *
     *  从 FILE 中读取类型为 T 的对象，并将其转换为 EXPECTEDCLASS。
     *  如果出现问题，抛出 IllegalArgumentException 异常。
     */
    static <T extends Serializable> T readObject(File file,
                                                 Class<T> expectedClass) {
        try {
            ObjectInputStream in =
                new ObjectInputStream(new FileInputStream(file));
            T result = expectedClass.cast(in.readObject());
            in.close();
            return result;
        } catch (IOException | ClassCastException
                 | ClassNotFoundException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    /** Write OBJ to FILE.
     *
     *  将 OBJ 写入 FILE。
     */
    static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }

    /* DIRECTORIES */

    /** Filter out all but plain files.
     *
     *  过滤掉所有非普通文件。
     */
    private static final FilenameFilter PLAIN_FILES =
        new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return new File(dir, name).isFile();
            }
        };

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory.
     *
     *  返回目录 DIR 中所有普通文件的名称列表，按字典顺序排列为 Java 字符串。
     *  如果 DIR 不是目录，则返回 null。
     */
    static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return null;
        } else {
            Arrays.sort(files);
            return Arrays.asList(files);
        }
    }

    /** Returns a list of the names of all plain files in the directory DIR, in
     *  lexicographic order as Java Strings.  Returns null if DIR does
     *  not denote a directory.
     *
     *  返回目录 DIR 中所有普通文件的名称列表，按字典顺序排列为 Java 字符串。
     *  如果 DIR 不是目录，则返回 null。
     */
    static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    /* OTHER FILE UTILITIES */

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the {@link java.nio.file.Paths.#get(String, String[])}
     *  method.
     *
     *  返回 FIRST 和 OTHERS 的连接结果作为文件设计符，类似于
     *  {@link java.nio.file.Paths.#get(String, String[])} 方法。
     */
    static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    /** Return the concatentation of FIRST and OTHERS into a File designator,
     *  analogous to the {@link java.nio.file.Paths.#get(String, String[])}
     *  method.
     *
     *  返回 FIRST 和 OTHERS 的连接结果作为文件设计符，类似于
     *  {@link java.nio.file.Paths.#get(String, String[])} 方法。
     */
    static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }


    /* SERIALIZATION UTILITIES */

    /** Returns a byte array containing the serialized contents of OBJ.
     *
     *  返回包含 OBJ 序列化内容的字节数组。
     */
    static byte[] serialize(Serializable obj) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ObjectOutputStream objectStream = new ObjectOutputStream(stream);
            objectStream.writeObject(obj);
            objectStream.close();
            return stream.toByteArray();
        } catch (IOException excp) {
            throw error("Internal error serializing commit.");
        }
    }



    /* MESSAGES AND ERROR REPORTING */

    /** Return a GitletException whose message is composed from MSG and ARGS as
     *  for the String.format method.
     *
     *  返回一个 GitletException，其消息由 MSG 和 ARGS 组成，类似于
     *  String.format 方法。
     */
    static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    /** Print a message composed from MSG and ARGS as for the String.format
     *  method, followed by a newline.
     *
     *  打印由 MSG 和 ARGS 组成的消息，类似于 String.format 方法，然后换行。
     */
    static void message(String msg, Object... args) {
        System.out.printf(msg, args);
        System.out.println();
    }

    static void createFile(File file) {
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw error("创建文件时发生错误：%s", e.getMessage());
        }
    }

    static void clean(File dir) {
        List<String> files = plainFilenamesIn(dir);
        if (files != null) {
            for (String file: files) {
                join(dir, file).delete();
            }
        }
    }
}
