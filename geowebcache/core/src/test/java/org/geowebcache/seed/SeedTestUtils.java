package org.geowebcache.seed;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;

import org.easymock.Capture;
import org.easymock.IAnswer;

public class SeedTestUtils {

    private SeedTestUtils(){};
    
    /**
     * Create a mock SeedTask, and expect a call to the breeder's createSeedTask method returning the created task
     * @param mockBreeder an EasyMock mock of a TileBreeder in its record phase
     * @return a mock SeedTask in its record phase
     */
    public static SeedTask createMockSeedTask(TileBreeder mockBreeder) throws Exception {
        final SeedTask task = createMock(SeedTask.class);
        final Capture<SeedJob> jobCap = new Capture<SeedJob>();
        expect(mockBreeder.createSeedTask(capture(jobCap))).andReturn(task).once();
        expect(task.getJob()).andStubAnswer(new IAnswer<SeedJob>(){
            // The task should report that it belongs to the captured job
            public SeedJob answer() throws Throwable {
                return jobCap.getValue();
            }

        });
        return task;
    }
    /**
     * Create a mock task, and expect a call to the breeder's createTruncateTask method returning the created task
     * @param mockBreeder an EasyMock mock of a TileBreeder in its record phase
     * @return a mock TruncateTask in its record phase
     */
    public static TruncateTask createMockTruncateTask(TileBreeder mockBreeder) throws Exception {
        final TruncateTask task = createMock(TruncateTask.class);
        final Capture<TruncateJob> jobCap = new Capture<TruncateJob>();
        expect(mockBreeder.createTruncateTask(capture(jobCap))).andReturn(task).once();
        expect(task.getJob()).andStubAnswer(new IAnswer<TruncateJob>(){
            // The task should report that it belongs to the captured job
            public TruncateJob answer() throws Throwable {
                return jobCap.getValue();
            }

        });
        return task;
    }

}
