package top.niunaijun.blackbox.fake.spoof;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.utils.Slog;

/** Profil (userId) başına SpoofConfig'i diskten okur, yoksa üretip yazar. */
public final class SpoofStore {
  private static final String TAG = "SpoofStore";
  private static final Map<Integer, SpoofConfig> CACHE = new ConcurrentHashMap<>();

  private SpoofStore() {
  }

  public static SpoofConfig get(int userId) {
    if (userId < 0) {
      Slog.w(TAG, "Invalid userId=" + userId + ", falling back to 0");
      userId = 0;
    }
    SpoofConfig cached = CACHE.get(userId);
    if (cached != null)
      return cached;
    synchronized (SpoofStore.class) {
      cached = CACHE.get(userId);
      if (cached != null)
        return cached;
      SpoofConfig cfg = loadOrCreate(userId);
      CACHE.put(userId, cfg);
      return cfg;
    }
  }

  public static void invalidate(int userId) {
    CACHE.remove(userId);
  }

  private static SpoofConfig loadOrCreate(int userId) {
    File dir = BEnvironment.getUserDir(userId);
    File file = new File(dir, "spoof.json");
    try {
      if (!dir.exists())
        dir.mkdirs();
      // Birden fazla sanal process aynı anda üretmeye kalkmasın diye dosya kilidi
      try (RandomAccessFile raf = new RandomAccessFile(new File(dir, "spoof.lock"), "rw");
          FileChannel ch = raf.getChannel();
          FileLock lock = ch.lock()) {
        SpoofConfig cfg = read(file);
        if (cfg == null) {
          cfg = SpoofConfig.generate();
          write(file, cfg);
          android.util.Log.i(TAG,
              "Generated new identity for userId=" + userId + ", androidId=" + cfg.androidId.substring(0, 6) + "...");
        }
        return cfg;
      }
    } catch (Exception e) {
      Slog.e(TAG, "Could not read/write identity, generating a temporary one", e);
      return SpoofConfig.generate();
    }
  }

  private static SpoofConfig read(File file) {
    if (!file.exists())
      return null;
    try (FileInputStream in = new FileInputStream(file);
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      byte[] buf = new byte[1024];
      int n;
      while ((n = in.read(buf)) > 0)
        out.write(buf, 0, n);
      return SpoofConfig.fromJson(new JSONObject(out.toString("UTF-8")));
    } catch (Exception e) {
      Slog.w(TAG, "spoof.json is corrupt, will regenerate", e);
      return null;
    }
  }

  private static void write(File file, SpoofConfig cfg) throws Exception {
    File tmp = new File(file.getParentFile(), "spoof.json.tmp");
    try (FileOutputStream out = new FileOutputStream(tmp)) {
      out.write(cfg.toJson().toString().getBytes("UTF-8"));
      out.getFD().sync();
    }
    if (!tmp.renameTo(file))
      throw new java.io.IOException("rename failed");
  }
}
