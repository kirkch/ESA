package com.mosaic.jact.schedulers;

import com.mosaic.jact.AsyncContext;
import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.conc.Monitor;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
@Ignore
public class MultiThreadedSchedulerPerfTests {

    // 10th July 2012 - public jobqueue               : 15m jobs per second using all 8 cores (fairly consistent)
    //                  public then private job queue : 52m jobs per second using 6.5 cores   (have seen as much as 122m using 6 cores)
    //                                                  varies widely (40m to 122m)

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

            asyncContext.scheduleLocally( this );

            return null;
        }
    }

}
