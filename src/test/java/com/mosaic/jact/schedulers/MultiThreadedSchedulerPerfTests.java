package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncContext;
import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.conc.Monitor;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
//@Ignore
public class MultiThreadedSchedulerPerfTests {

    // from within intellij
    // -ea -server -Xms100m -Xmx100m -XX:MaxPermSize=100m -XX:PermSize=100m -Dsun.net.inetaddr.ttl=120 -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=10 -XX:CMSIncrementalDutyCycleMin=0 -XX:SurvivorRatio=2 -XX:NewRatio=3 -XX:+CMSClassUnloadingEnabled  -XX:+HeapDumpOnOutOfMemoryError -XX:SurvivorRatio=2 -XX:NewRatio=3

    // 10th July 2012 - public jobqueue               : 15-17m jobs per second using all 8 cores (fairly consistent)
    //                  public then private job queue : 52m jobs per second using 6.5 cores   (have seen as much as 122m using 6 cores)
    //                                                  varies widely (40m to 122m)

    // 11th July 2012 - public queue configured with 9 threads (gives better distribution of work) : 50-95m jobs per second 6 cores,
    //                  tended to be closer to 80m on average
    //                  6 threads : 55-85m using 5 cores
    //
    // did some memory tuning
    // new settings: -server -Xms400m -Xmx400m -XX:MaxPermSize=100m -XX:PermSize=100m -Dsun.net.inetaddr.ttl=120 -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=10 -XX:CMSIncrementalDutyCycleMin=0 -XX:SurvivorRatio=2 -XX:NewRatio=3 -XX:-CMSClassUnloadingEnabled
    //
    // 11th July 2012 - 8 threads, local scheduling - 100m to 200m 6.5 cores
    //                  private scheduling - 16m-20m 7.5 cores
    //
    // doubling memory again say 160m-195m as the most common range; memory usage is making a big difference
    // changing from using a linked list to a array that is large enough to take ALL jobs, performance increased to 211m
    // however if the array was not large enough and was being linked then performance dropped to 100m

    @Test
    public void throughputTest() throws InterruptedException {
        MultiThreadedScheduler scheduler = new MultiThreadedScheduler( "throughput", 8, 0 );

        scheduler.start();

        ProgressSheet[] progressSheets = setupProgressSheets( 20 );
        startReportingProgress( progressSheets );


        int numTopLevelJobs = 10000;

        for ( int i=0; i<numTopLevelJobs; i++ ) {
            scheduler.schedule( new SumJob(progressSheets[i%progressSheets.length]) );
        }

        Monitor lock = new Monitor();
        lock.sleep();
    }


    // 11th July 2012 - 8 core 2011 macbook pro laptop .. 1.9m jobs per second only used 2.5 cores ouch
    //
    // following new settings
    // -server -Xms400m -Xmx400m -XX:MaxPermSize=100m -XX:PermSize=100m -Dsun.net.inetaddr.ttl=120 -XX:+UseConcMarkSweepGC -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -XX:CMSIncrementalDutyCycle=10 -XX:CMSIncrementalDutyCycleMin=0 -XX:SurvivorRatio=2 -XX:NewRatio=3 -XX:-CMSClassUnloadingEnabled
    //
    // 11th July 2012 - 1.9-2.0m

    @Test
    public void throughputTestJavaExecutor() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(8);

        ProgressSheet[] progressSheets = setupProgressSheets( 20 );
        startReportingProgress( progressSheets );


        int numTopLevelJobs = 10000;

        for ( int i=0; i<numTopLevelJobs; i++ ) {
            executor.submit( new SumJobRunnable(progressSheets[i%progressSheets.length],executor) );
        }

        Monitor lock = new Monitor();
        lock.sleep();
    }


    // 11th July 2012 - 370-380m jobs per second using 8 cores

    @Test
    public void noSchedulingJust8Threads() {
        ProgressSheet[] progressSheets = setupProgressSheets( 8 );
        startReportingProgress( progressSheets );

        for ( int i=0; i<8; i++ ) {
            final ProgressSheet progressSheet = progressSheets[i];

            new Thread() {
                public void run() {
                    while (true) {
                        progressSheet.inc();
                    }
                }
            }.start();
        }

        Monitor lock = new Monitor();
        lock.sleep();
    }
    // todo disruptors
    // todo akka

    private ProgressSheet[] setupProgressSheets( int numProgressSheets ) {
        ProgressSheet[] sheets = new ProgressSheet[numProgressSheets];

        for ( int i=0; i<numProgressSheets; i++ ) {
            sheets[i] = new ProgressSheet();
        }

        return sheets;
    }

    private void startReportingProgress( final ProgressSheet[] progressSheets ) {
        for ( ProgressSheet progressSheet : progressSheets ) {
            progressSheet.start();
        }

        new Thread() {
            @Override
            public void run() {
                Monitor monitor = new Monitor();

                while ( true ) {
                    monitor.sleep( 10000 );

                    printProgressUpdate();
                    resetProgressSheets();
                }
            }

            private void printProgressUpdate() {
                double jobsPerSecondAverage = 0;

                for ( ProgressSheet progressSheet : progressSheets ) {
                    jobsPerSecondAverage += progressSheet.ratePerSecond();
                }

                System.out.println( "Jobs per second = " + jobsPerSecondAverage );
            }

            private void resetProgressSheets() {
                for ( ProgressSheet progressSheet : progressSheets ) {
                    progressSheet.reset();
                }
            }
        }.start();
    }


    static class ProgressSheet {
        public final AtomicLong jobCount = new AtomicLong(0);

        public final AtomicLong startMillis = new AtomicLong(0);
        public final AtomicLong endMillis   = new AtomicLong(0);


        public void start() {
            startMillis.compareAndSet( 0, System.currentTimeMillis() );
        }

        public void stop() {
            endMillis.compareAndSet( 0, System.currentTimeMillis() );
        }

        public double ratePerSecond() {
            long duration = System.currentTimeMillis() - startMillis.get();
            long count    = jobCount.get();

            return count / (duration/1000.0);
        }

        public void reset() {
            jobCount.set(0);
            startMillis.set( System.currentTimeMillis() );
        }

        public void inc() {
            jobCount.incrementAndGet();
        }
    }

    private static class SumJob extends AsyncJob {
        private ProgressSheet progressSheet;

        public SumJob( ProgressSheet progressSheet ) {
            this.progressSheet = progressSheet;
        }

        @Override
        public Object invoke( AsyncContext asyncContext ) throws Exception {
            progressSheet.inc();

            asyncContext.schedule( this );
//            asyncContext.scheduleLocally( this );

            return null;
        }
    }

    private static class SumJobRunnable implements Runnable {
        private ProgressSheet   progressSheet;
        private ExecutorService executor;

        public SumJobRunnable( ProgressSheet progressSheet, ExecutorService executor ) {
            this.progressSheet = progressSheet;
            this.executor      = executor;
        }

        public void run() {
            progressSheet.inc();

            executor.submit( this );
        }

    }
}
