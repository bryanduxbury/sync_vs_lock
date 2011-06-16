
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockVsSync {

  private static int HOW_MANY = 1000000;//0;//0;// 0;
  private static int NUM_THREADS = 2;

  private static abstract class SyncStartRunnable implements Runnable {
    private final ReadWriteLock lock;

    public SyncStartRunnable(ReadWriteLock lock) {
      this.lock = lock;
    }

    public abstract void realRun();

    @Override
    public void run() {
      lock.readLock().lock();
      lock.readLock().unlock();
      realRun();
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      HOW_MANY = 10000000;
      NUM_THREADS = 2;
    } else {
      HOW_MANY = Integer.parseInt(args[0]);
      NUM_THREADS = Integer.parseInt(args[1]);
    }

    for (int trial = 0; trial < 5; trial++) {
      final Object mutex = new Object();

      ReadWriteLock lock = new ReentrantReadWriteLock();
      lock.writeLock().lock();
      List<Thread> threads = new ArrayList<Thread>();
      for (int threadNum = 0; threadNum < NUM_THREADS; threadNum++) {
        final Thread thread = new Thread(new SyncStartRunnable(lock) {
          @Override
          public void realRun() {
            int x = 0;
            for (int i = 0; i < HOW_MANY; i++) {
              synchronized (mutex) {
                if (x == 10000) {
                  System.err.println(x);
                  x = 0;
                }
                x++;
              }
            }
          }
        });
        threads.add(thread);
        thread.start();
      }
      long start = System.currentTimeMillis();
      lock.writeLock().unlock();
      for (Thread t : threads) {
        t.join();
      }
      long end = System.currentTimeMillis();
      System.out.println("sync: " + (end - start));

      // lock version
      lock = new ReentrantReadWriteLock();
      lock.writeLock().lock();

      final Lock actualLock = new ReentrantLock();
      threads = new ArrayList<Thread>();
      for (int threadNum = 0; threadNum < NUM_THREADS; threadNum++) {
        final Thread thread = new Thread(new SyncStartRunnable(lock) {
          @Override
          public void realRun() {
            int x = 0;
            for (int i = 0; i < HOW_MANY; i++) {
              actualLock.lock();
              if (x == 10000) {
                System.err.println(x);
                x = 0;
              }
              x++;
              actualLock.unlock();
            }
          }
        });
        threads.add(thread);
        thread.start();
      }
      start = System.currentTimeMillis();
      lock.writeLock().unlock();
      for (Thread t : threads) {
        t.join();
      }
      end = System.currentTimeMillis();
      System.out.println("lock: " + (end - start));

      // int x = 0;
      // 
      // start = System.currentTimeMillis();
      // for (int i = 0; i < HOW_MANY; i++) {
      // lock.lock();
      // x++;
      // lock.unlock();
      // }
      // end = System.currentTimeMillis();
      // System.out.println("lock: " + (end-start));
    }
  }
}
